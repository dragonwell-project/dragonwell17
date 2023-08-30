/*
 * @test
 * @summary test InterruptedException was thrown by sleep()
 * @library /test/lib
 * @requires os.family == "linux"
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+EnableCoroutine -Dcom.alibaba.wisp.transparentWispSwitch=true TestInterruptedSleep
*/

import com.alibaba.wisp.engine.WispEngine;

import static jdk.test.lib.Asserts.assertFalse;
import static jdk.test.lib.Asserts.assertLessThan;
import static jdk.test.lib.Asserts.assertTrue;

public class TestInterruptedSleep {
    public static void main(String[] args) {
        Thread mainCoro = Thread.currentThread();
        WispEngine.dispatch(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
            mainCoro.interrupt();
        });
        long start = System.currentTimeMillis();
        boolean ie = false;
        try {
            Thread.sleep(1000L);
        } catch (InterruptedException e) {
            ie = true;
        }
        assertLessThan((int) (System.currentTimeMillis() - start), 1000);
        assertTrue(ie);
        assertFalse(mainCoro.isInterrupted());
    }
}
