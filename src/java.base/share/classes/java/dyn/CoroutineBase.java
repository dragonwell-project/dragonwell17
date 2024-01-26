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

import jdk.internal.access.SharedSecrets;

/**
 * Abstract of coroutines.
 */
public abstract class CoroutineBase {
    transient long nativeCoroutine;

    boolean finished = false;

    transient CoroutineSupport threadSupport;

    /**
     * Allocates a new {@code CoroutineBase} object.
     */
    CoroutineBase() {
        Thread thread = SharedSecrets.getJavaLangAccess().currentThread0();
        assert thread.getCoroutineSupport() != null;
        this.threadSupport = thread.getCoroutineSupport();
    }

    /**
     * Creates the initial coroutine for a new thread
     *
     * @param threadSupport CoroutineSupport
     * @param nativeCoroutine pointer of native Coroutine
     */
    CoroutineBase(CoroutineSupport threadSupport, long nativeCoroutine) {
        this.threadSupport = threadSupport;
        this.nativeCoroutine = nativeCoroutine;
    }

    /**
     * entry of coroutine
     */
    protected abstract void run();

    @SuppressWarnings({"unused"})
    private final void startInternal() {
        assert threadSupport.getThread() == SharedSecrets.getJavaLangAccess().currentThread0();
        try {
            run();
        } catch (Throwable t) {
            if (!(t instanceof CoroutineExitException)) {
                t.printStackTrace();
            }
        } finally {
            finished = true;
            threadSupport.terminateCoroutine();
        }
        assert threadSupport.getThread() == SharedSecrets.getJavaLangAccess().currentThread0();
    }

    /**
     * @return true if this coroutine has reached its end. Under normal circumstances this happens when the {@link #run()} method returns.
     */
    public final boolean isFinished() {
        return finished;
    }

    /**
     * @return the thread that this coroutine is associated with
     * @throws NullPointerException if the coroutine has been terminated
     */
    public Thread getThread() {
        return threadSupport.getThread();
    }

    /**
     * @return the current coroutine in the thread
     */
    public static CoroutineBase current() {
        return SharedSecrets.getJavaLangAccess().currentThread0().getCoroutineSupport().getCurrent();
    }
}
