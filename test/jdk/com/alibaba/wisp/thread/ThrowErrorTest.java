/*
 * @test
 * @summary test coroutine throw Error
 * @library /test/lib
 * @run main/othervm -XX:+EnableCoroutine -Dcom.alibaba.wisp.transparentWispSwitch=true ThrowErrorTest
 * @run main/othervm -XX:+EnableCoroutine -Dcom.alibaba.wisp.transparentWispSwitch=true -Dcom.alibaba.wisp.version=2 ThrowErrorTest
*/

import com.alibaba.wisp.engine.WispEngine;

import static jdk.test.lib.Asserts.assertTrue;

public class ThrowErrorTest {
    public static void main(String[] args) {
        WispEngine.dispatch(() -> {
            throw new Error();
        });

        boolean[] executed = new boolean[]{false};

        WispEngine.dispatch(() -> executed[0] = true);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
        }
        assertTrue(executed[0]);
    }
}
