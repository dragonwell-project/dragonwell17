/*
 * @test
 * @summary Test elision spin
 * @modules java.base/jdk.internal.access
 * @library /test/lib
 * @run main/othervm  -XX:+EnableCoroutine -Dcom.alibaba.wisp.transparentWispSwitch=true -Dcom.alibaba.wisp.useStealLock=false TestElisionSpin
 */

import com.alibaba.wisp.engine.WispEngine;
import jdk.internal.access.SharedSecrets;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import static jdk.test.lib.Asserts.assertFalse;
import static jdk.test.lib.Asserts.assertTrue;

public class TestElisionSpin {
    public static void main(String[] args) {
        assertFalse(SharedSecrets.getWispEngineAccess().hasMoreTasks());

        ReentrantLock lock = new ReentrantLock();
        Condition cond = lock.newCondition();

        WispEngine.dispatch(() -> {
            lock.lock();
            try {
                cond.awaitUninterruptibly();
            } finally {
                lock.unlock();
            }
        });

        lock.lock();
        try {
            cond.signal();
        } finally {
            lock.unlock();
        }
        assertTrue(SharedSecrets.getWispEngineAccess().hasMoreTasks());
    }
}
