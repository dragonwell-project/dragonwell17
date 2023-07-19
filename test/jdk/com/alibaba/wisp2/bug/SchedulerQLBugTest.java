/*
 * @test
 * @library /test/lib
 * @summary verify queue length not growth infinity
 * @modules java.base/jdk.internal.access
 * @modules java.base/com.alibaba.wisp.engine:+open
 * @run main/othervm -XX:+UseWisp2 DisableStealBugTest
 */

import com.alibaba.wisp.engine.Wisp2Group;
import com.alibaba.wisp.engine.WispEngine;
import jdk.internal.access.SharedSecrets;

import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;

import static jdk.test.lib.Asserts.assertLT;

public class SchedulerQLBugTest {
    public static void main(String[] args) throws Exception {
        Wisp2Group wisp2Group = Wisp2Group.createGroup(2, Thread::new);
        CountDownLatch latch = new CountDownLatch(1);
        wisp2Group.execute(() -> {
            DisableStealBugTest.setOrGetStealEnable(SharedSecrets.getWispEngineAccess().getCurrentTask(), true, false);
            latch.countDown();
            while (true) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        latch.await();
        for (int i = 0; i < 10; i++) { // trigger steal
            wisp2Group.execute(() -> { /**/});
        }

        Thread.sleep(100);
        for (int i = 0; i < 10; i++) {
            int ql = wisp2Group.submit(() -> {
                try {
                    Method m = WispEngine.class.getDeclaredMethod("getTaskQueueLength");
                    m.setAccessible(true);
                    return (int) m.invoke(WispEngine.current());
                } catch (ReflectiveOperationException e) {
                    throw new Error(e);
                }
            }).get();
            assertLT(ql, 100);
        }
    }
}
