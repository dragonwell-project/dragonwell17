/*
 * @test
 * @library /test/lib
 * @summary Test Daemon Thread Group implementation
 * @modules java.base/jdk.internal.access
 * @modules java.base/com.alibaba.wisp.engine:+open
 * @requires os.family == "linux"
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+EnableCoroutine -Dcom.alibaba.wisp.transparentWispSwitch=true  -Dcom.alibaba.wisp.useCarrierAsPoller=false TestDaemonThreadGroup
*/


import com.alibaba.wisp.engine.WispEngine;
import com.alibaba.wisp.engine.WispTask;
import jdk.internal.access.SharedSecrets;
import jdk.internal.access.WispEngineAccess;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static jdk.test.lib.Asserts.assertTrue;


/**
 * test the Daemon Thread Group implementation
 */
public class TestDaemonThreadGroup {
    public static void main(String... arg) throws Exception {
        Field f = Class.forName("com.alibaba.wisp.engine.WispEngine").getDeclaredField("pollerThread");
        f.setAccessible(true);
        Thread t = (Thread) f.get(null);

        f = Class.forName("com.alibaba.wisp.engine.WispEngine").getDeclaredField("daemonThreadGroup");
        f.setAccessible(true);
        ThreadGroup threadGroup = (ThreadGroup) f.get(null);

        System.out.println(threadGroup.getName());

        assertTrue(t.getThreadGroup() == threadGroup, "the thread isn't in daemonThreadGroup");
    }
}

