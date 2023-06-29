/*
 * @test
 * @library /lib/testlibrary
 * @summary convert all thread to wisp
 * @modules java.base/jdk.internal.misc
 * @run main/othervm -XX:+EnableCoroutine  -Dcom.alibaba.wisp.transparentWispSwitch=true  -Dcom.alibaba.wisp.version=2 -XX:+UseWispMonitor -Dcom.alibaba.wisp.enableThreadAsWisp=true -Dcom.alibaba.wisp.allThreadAsWisp=true AllThreadAsWispTest
 */

import jdk.internal.access.SharedSecrets;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static jdk.testlibrary.Asserts.assertTrue;

public class AllThreadAsWispTest {
    public static void main(String[] args) throws Exception {
        CountDownLatch done = new CountDownLatch(60);

        Runnable r = () -> {
            if (SharedSecrets.getJavaLangAccess().currentThread0() != Thread.currentThread()) {
                done.countDown();
            }
        };

        for (int i = 0; i < 10; i++) {
            Executors.newSingleThreadExecutor().submit(r);
            Executors.newWorkStealingPool().submit(r);
            Executors.newScheduledThreadPool(100).submit(r);
            new Thread(r).start();
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    r.run();
                }
            }, 0);
            new Thread() {
                @Override
                public void run() {
                    r.run();
                }
            }.start();
        }

        assertTrue(done.await(10, TimeUnit.SECONDS));
    }
}
