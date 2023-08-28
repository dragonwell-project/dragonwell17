/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package java.dyn;

import jdk.internal.vm.annotation.IntrinsicCandidate;
import jdk.internal.access.SharedSecrets;
import jdk.internal.misc.Unsafe;
import jdk.internal.vm.annotation.Contended;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;

/**
 * Jvm entry of coroutine APIs.
 */
@Contended
public class CoroutineSupport {

    private static final boolean CHECK_LOCK = true;
    private static final int SPIN_BACKOFF_LIMIT = 2 << 8;
    private static final Unsafe UNSAFE = Unsafe.getUnsafe();

    private static AtomicInteger idGen = new AtomicInteger();

    // The thread that this CoroutineSupport belongs to. There's only one CoroutineSupport per Thread
    private final Thread thread;
    // The initial coroutine of the Thread
    private final Coroutine threadCoroutine;
    // The thread's native thread id
    private final long nativeThreadId;

    // The currently executing coroutine
    private Coroutine currentCoroutine;

    private long lockOwnerAddress; // also protect double link list of JavaThread->coroutine_list()
    private int lockRecursive; // volatile is not need

    private final int id;
    private boolean terminated = false;

    static {
        registerNatives();
    }

    /**
     * Allocates a new {@code CoroutineSupport} object.
     * @param thread the thread
     * @param nativeThreadId the thread native thread id
     * @param lockOwnerAddress the native coroutine supprt lock address
     */
    public CoroutineSupport(Thread thread, long nativeThreadId, long lockOwnerAddress) {
        if (thread.getCoroutineSupport() != null) {
            throw new IllegalArgumentException("Cannot instantiate CoroutineThreadSupport for existing Thread");
        }
        id = idGen.incrementAndGet();
        this.thread = thread;
        threadCoroutine = new Coroutine(this, getNativeThreadCoroutine());
        markThreadCoroutine(threadCoroutine.nativeCoroutine, threadCoroutine);
        currentCoroutine = threadCoroutine;
        this.nativeThreadId = nativeThreadId;
        this.lockOwnerAddress = lockOwnerAddress;
    }

    /**
     * return the threadCoroutine
     * @return threadCoroutine
     */
    public Coroutine threadCoroutine() {
        return threadCoroutine;
    }

    void addCoroutine(Coroutine coroutine, long stacksize) {
        assert currentCoroutine != null;
        lock();
        try {
            coroutine.nativeCoroutine = createCoroutine(coroutine, stacksize);
        } finally {
            unlock();
        }
    }

    Thread getThread() {
        return thread;
    }

    /**
     * check if we should throw a TenantDeath or ThreqadDeathException
     * @param coroutine the coroutine
     * @return if coroutine should throw exception
     */
    public static boolean checkAndThrowException(Coroutine coroutine) {
        return shouldThrowException0(coroutine.nativeCoroutine);
    }


    /**
     * Telling if current coroutine is executing clinit
     *
     * @param coroutine the coroutine
     * @return if current coroutine is executing clinit
     */
    public static boolean isInClinit(Coroutine coroutine) {
        return isInClinit0(coroutine.nativeCoroutine);
    }

    /**
     * drain all alive coroutines.
     */
    public void drain() {
        if (Thread.currentThread() != thread) {
            throw new IllegalArgumentException("Cannot drain another threads CoroutineThreadSupport");
        }

        lock();
        try {
            // drain all coroutines
            Coroutine next = null;
            while ((next = getNextCoroutine(currentCoroutine.nativeCoroutine)) != currentCoroutine) {
                symmetricExitInternal(next);
            }

            CoroutineBase coro;
            while ((coro = cleanupCoroutine()) != null) {
                System.out.println(coro);
                throw new NotImplementedException();
            }
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            assert lockOwner() == nativeThreadId && lockRecursive == 0;
            terminated = true;
            unlock();
        }
    }

    /**
     * optimized version of symmetricYieldTo based on assumptions:
     * 1. we won't simultaneously steal a {@link Coroutine} from other threads
     * 2. we won't switch to a {@link Coroutine} that's being stolen
     * 3. we won't steal a running {@link Coroutine}
     * this function should only be called in
     * {@link com.alibaba.wisp.engine.WispTask#switchTo(WispTask, WispTask)},
     * we skipped unnecessary lock to improve performance.
     * @param target coroutine
     */
    public void unsafeSymmetricYieldTo(Coroutine target) {
        if (target.threadSupport != this) {
            return;
        }
        final Coroutine current = currentCoroutine;
        currentCoroutine = target;
        switchTo(current, target);
        //check if locked by exiting coroutine
        beforeResume(current);
    }

    /**
     * yield to coroutine with lock
     * @param target coroutine
     */
    public void symmetricYieldTo(Coroutine target) {
        lock();
        if (target.threadSupport != this) {
            unlock();
            return;
        }
        moveCoroutine(currentCoroutine.nativeCoroutine, target.nativeCoroutine);
        unlockLater(target);
        unsafeSymmetricYieldTo(target);
    }

    /**
     * yield to coroutine with lock and stop the current coroutine
     * @param target coroutine
     */
    public void symmetricStopCoroutine(Coroutine target) {
        Coroutine current;
        lock();
        try {
            if (target.threadSupport != this) {
                unlock();
                return;
            }
            current = currentCoroutine;
            currentCoroutine = target;
            moveCoroutine(current.nativeCoroutine, target.nativeCoroutine);
        } finally {
            unlock();
        }
        switchToAndExit(current, target);
    }

    /**
     * switch to coroutine and throw Exception in coroutine
     * @param coroutine coroutine
     */
    void symmetricExitInternal(Coroutine coroutine) {
        assert currentCoroutine != coroutine;
        assert coroutine.threadSupport == this;

        if (!testDisposableAndTryReleaseStack(coroutine.nativeCoroutine)) {
            moveCoroutine(currentCoroutine.nativeCoroutine, coroutine.nativeCoroutine);

            final Coroutine current = currentCoroutine;
            currentCoroutine = coroutine;
            switchToAndExit(current, coroutine);
            beforeResume(current);
        }
    }

    /**
     * terminate current coroutine and yield forward
     * @param  target target
     */
    public void terminateCoroutine(Coroutine target) {
        assert currentCoroutine != threadCoroutine : "cannot exit thread coroutine";

        lock();
        Coroutine old = currentCoroutine;
        Coroutine forward = target;
        if (forward == null) {
            forward = getNextCoroutine(old.nativeCoroutine);
        }
        assert forward == threadCoroutine : "switch to target must be thread coroutine";
        currentCoroutine = forward;
        unlockLater(forward);
        switchToAndTerminate(old, forward);

        // should never run here.
        assert false;
    }

    /**
     * Steal coroutine from it's carrier thread to current thread.
     *
     * @param failOnContention steal fail if there's too much lock contention
     *
     * @param coroutine to be stolen
     */
    Coroutine.StealResult steal(Coroutine coroutine, boolean failOnContention) {
        assert coroutine.threadSupport.threadCoroutine() != coroutine;
        CoroutineSupport source = this;
        CoroutineSupport target = SharedSecrets.getJavaLangAccess().currentThread0().getCoroutineSupport();

        if (source == target) {
            return Coroutine.StealResult.SUCCESS;
        }

        if (source.id < target.id) { // prevent dead lock
            if (!source.lockInternal(failOnContention)) {
                return Coroutine.StealResult.FAIL_BY_CONTENTION;
            }
            target.lock();
        } else {
            target.lock();
            if (!source.lockInternal(failOnContention)) {
                target.unlock();
                return Coroutine.StealResult.FAIL_BY_CONTENTION;
            }
        }

        try {
            if (source.terminated || coroutine.finished ||
                    coroutine.threadSupport != source || // already been stolen
                    source.currentCoroutine == coroutine) {
                return Coroutine.StealResult.FAIL_BY_STATUS;
            }
            if (!stealCoroutine(coroutine.nativeCoroutine)) { // native frame
                return Coroutine.StealResult.FAIL_BY_NATIVE_FRAME;
            }
            coroutine.threadSupport = target;
        } finally {
            source.unlock();
            target.unlock();
        }

        return Coroutine.StealResult.SUCCESS;
    }

    /**
     * Can not be stolen while executing this, because lock is held
     */
    void beforeResume(CoroutineBase source) {
        if (source.needsUnlock) {
            source.needsUnlock = false;
            source.threadSupport.unlock();
        }
    }

    private void unlockLater(CoroutineBase next) {
        if (CHECK_LOCK && next.needsUnlock) {
            throw new InternalError("pending unlock");
        }
        next.needsUnlock = true;
    }

    private long lockOwner() {
        return UNSAFE.getLongAcquire(null, lockOwnerAddress);
    }

    private void clearLockOwner() {
        UNSAFE.putLongRelease(null, lockOwnerAddress, 0);
    }

    private void lock() {
        boolean success = lockInternal(false);
        assert success;
    }

    private boolean lockInternal(boolean tryingLock) {
        final Thread th = SharedSecrets.getJavaLangAccess().currentThread0();
        final long tid = th.getCoroutineSupport().nativeThreadId;
        if (lockOwner() == tid) {
            lockRecursive++;
            return true;
        }
        for (int spin = 1; ; ) {
            if (lockOwner() == 0 &&
                UNSAFE.compareAndSetLong(null, lockOwnerAddress, 0, tid)) {
                return true;
            }
            for (int i = 0; i < spin; ) {
                i++;
            }
            if (spin == SPIN_BACKOFF_LIMIT) {
                if (tryingLock) {
                    return false;
                }
                SharedSecrets.getJavaLangAccess().yield0(); // yield safepoint
            } else { // back off
                spin *= 2;
            }
        }
    }

    private void unlock() {
        if (CHECK_LOCK &&
            SharedSecrets.getJavaLangAccess().currentThread0()
            .getCoroutineSupport().nativeThreadId != lockOwner()) {
            throw new InternalError("unlock from non-owner thread");
        }
        if (lockRecursive > 0) {
            lockRecursive--;
        } else {
            UNSAFE.putLongRelease(null, lockOwnerAddress, 0);
        }
    }

    /**
     * @param coroutine the coroutine
     * @return whether the coroutine is current coroutine
     */
    public boolean isCurrent(CoroutineBase coroutine) {
        return coroutine == currentCoroutine;
    }

    /**
     * @return current running coroutine
     */
    public CoroutineBase getCurrent() {
        return currentCoroutine;
    }


    private static native void registerNatives();

    private static native long getNativeThreadCoroutine();

    /**
     * need lock because below methods will operate on thread->coroutine_list()
     */
    private static native long createCoroutine(CoroutineBase coroutine, long stacksize);

    @IntrinsicCandidate
    private static native void switchToAndTerminate(CoroutineBase current, CoroutineBase target);

    private static native boolean testDisposableAndTryReleaseStack(long coroutine);

    private static native boolean stealCoroutine(long coroPtr);
    // end of locking

    /**
     * get next {@link Coroutine} from current thread's doubly linked {@link Coroutine} list
     * @param coroPtr hotspot coroutine
     * @return java Coroutine
     */
    private static native Coroutine getNextCoroutine(long coroPtr);

    /**
     * move coroPtr to targetPtr's next field in underlying hotspot coroutine list
     * @param coroPtr current threadCoroutine
     * @param targetPtr coroutine that is about to exit
     */
    private static native void moveCoroutine(long coroPtr, long targetPtr);

    /**
     * track hotspot couroutine with java coroutine.
     * @param coroPtr threadCoroutine in hotspot
     * @param threadCoroutine threadCoroutine in java
     */
    private static native void markThreadCoroutine(long coroPtr, CoroutineBase threadCoroutine);

    /**
     * print info by native method, without synchronize
     * @param info info to be printed in Java
     */
    public static native void printlnLockFree(String info);

    @IntrinsicCandidate
	private static native void switchTo(CoroutineBase current, CoroutineBase target);

    @IntrinsicCandidate
    private static native void switchToAndExit(CoroutineBase current, CoroutineBase target);

    private static native CoroutineBase cleanupCoroutine();

    /**
     * Telling jvm that wisp is ready to be used.
     */
    public static native void setWispBooted();

    /**
     * this will turn on a safepoint to stop all threads.
     * @param coroPtr coroutine
     * @return target coroutine's stack
     */
    public static native StackTraceElement[] getCoroutineStack(long coroPtr);

    private static native boolean isInClinit0(long coroPtr);

    private static native boolean shouldThrowException0(long coroPtr);
}
