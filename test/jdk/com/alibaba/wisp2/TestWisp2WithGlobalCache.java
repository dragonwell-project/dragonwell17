/*
 * @test
 * @library /test/lib
 * @summary test the task exit flow for ThreadAsWisp task
 * @modules java.base/jdk.internal.access
 * @run main/othervm -XX:ActiveProcessorCount=2 -XX:+EnableCoroutine  -Dcom.alibaba.wisp.transparentWispSwitch=true  -Dcom.alibaba.wisp.version=2 -XX:+UseWispMonitor -Dcom.alibaba.wisp.enableThreadAsWisp=true -Dcom.alibaba.wisp.allThreadAsWisp=true -Dcom.alibaba.wisp.engineTaskCache=2 TestWisp2WithGlobalCache
 */

import jdk.internal.access.SharedSecrets;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static jdk.test.lib.Asserts.assertTrue;

public class TestWisp2WithGlobalCache {
    public static void main(String[] args) throws Exception {
        CountDownLatch done = new CountDownLatch(80);

        Runnable r = () -> {
            try {
                Thread.sleep(100);
            } catch(Throwable t) {
            }
            if (SharedSecrets.getJavaLangAccess().currentThread0() != Thread.currentThread()) {
                done.countDown();
            }
        };

        for (int i = 0; i < 10; i++) {
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
            new Thread() {
                @Override
                public void run() {
                    r.run();
                }
            }.start();
            new Thread() {
                @Override
                public void run() {
                    r.run();
                }
            }.start();
            new Thread() {
                @Override
                public void run() {
                    r.run();
                }
            }.start();
            new Thread() {
                @Override
                public void run() {
                    r.run();
                }
            }.start();
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
