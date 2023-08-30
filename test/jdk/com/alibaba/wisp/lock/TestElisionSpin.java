/*
 * @test
 * @summary Test elision spin
 * @modules java.base/jdk.internal.access
 * @library /test/lib
 * @requires os.family == "linux"
 * @run main/othervm  -XX:+UnlockExperimentalVMOptions -XX:+UseWisp2 -Dcom.alibaba.wisp.carrierEngines=1 TestElisionSpin
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
