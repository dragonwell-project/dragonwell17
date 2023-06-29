/*
 * @test
 * @summary Test timer implement
 * @modules java.base/jdk.internal.misc
 * @run main/othervm -XX:+EnableCoroutine TimerTest
 * @run main/othervm -XX:+EnableCoroutine -Dcom.alibaba.wisp.version=2 TimerTest
*/


import com.alibaba.wisp.engine.WispEngine;
import com.alibaba.wisp.engine.WispTask;
import jdk.internal.access.SharedSecrets;


/**
 * test the time out implement
 */
public class TimerTest {
    public static void main(String... arg) throws Exception {
        WispEngine.dispatch(() -> {
            long ts = System.currentTimeMillis();
            for (int i = 0; i < 10; i++) {
                SharedSecrets.getWispEngineAccess().sleep(100);
                long now = System.currentTimeMillis();
                if (Math.abs(now - ts - 100) > 10)
                    throw new Error();
                ts = now;
            }

        });

        SharedSecrets.getWispEngineAccess().eventLoop();
    }
}
