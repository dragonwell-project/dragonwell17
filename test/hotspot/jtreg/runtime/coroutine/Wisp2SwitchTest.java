/*
 * @test
 * @summary test wisp2 switch
 * @library /test/lib
 * @modules java.base/jdk.internal.access
 * @modules java.base/com.alibaba.wisp.engine:+open
 * @run main/othervm -XX:+UseWisp2  Wisp2SwitchTest
 */


import com.alibaba.wisp.engine.WispEngine;
import jdk.internal.access.SharedSecrets;

import java.lang.reflect.Field;
import static jdk.test.lib.Asserts.*;

public class Wisp2SwitchTest {
    public static void main(String[] args) throws Exception {
        WispEngine.dispatch(() -> {
            for (int i = 0; i < 9999999; i++) {
                try {
                    Thread.sleep(100);
                    System.out.println(i + ": " + SharedSecrets.getJavaLangAccess().currentThread0());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        System.out.println("Wisp2SwitchTest.main");
        Field f = Class.forName("com.alibaba.wisp.engine.WispConfiguration").getDeclaredField("WISP_VERSION");
        f.setAccessible(true);
        int version = f.getInt(null);
        assertTrue(version == 2, "Wisp Version isn't Wisp2");

        boolean isEnabled;
        f = Class.forName("com.alibaba.wisp.engine.WispConfiguration").getDeclaredField("TRANSPARENT_WISP_SWITCH");
        f.setAccessible(true);
        isEnabled = f.getBoolean(null);
        assertTrue(isEnabled == true, "The property com.alibaba.wisp.transparentWispSwitch isn't enabled");

        f = Class.forName("com.alibaba.wisp.engine.WispConfiguration").getDeclaredField("ENABLE_THREAD_AS_WISP");
        f.setAccessible(true);
        isEnabled = f.getBoolean(null);
        assertTrue(isEnabled == true, "The property com.alibaba.wisp.enableThreadAsWisp isn't enabled");

        f = Class.forName("com.alibaba.wisp.engine.WispConfiguration").getDeclaredField("ALL_THREAD_AS_WISP");
        f.setAccessible(true);
        isEnabled = f.getBoolean(null);
        assertTrue(isEnabled == true, "The property com.alibaba.wisp.allThreadAsWisp isn't enabled");

        f = Class.forName("com.alibaba.wisp.engine.WispConfiguration").getDeclaredField("ENABLE_HANDOFF");
        f.setAccessible(true);
        isEnabled = f.getBoolean(null);
        assertFalse(isEnabled == true, "The property com.alibaba.wisp.enableHandOff isn't enabled");

        Thread.sleep(1000);
    }
}
