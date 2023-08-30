/*
 * @test
 * @library /test/lib
 * @modules java.base/jdk.internal.access
 * @modules java.base/com.alibaba.wisp.engine:+open
 * @summary Verify yield not really happened when queue is empty
 * @requires os.family == "linux"
 * @run main/othervm -XX:+UnlockExperimentalVMOptions  -XX:+UseWisp2 TestYieldEmptyQueue
 */

import com.alibaba.wisp.engine.WispEngine;
import jdk.internal.access.SharedSecrets;
import java.lang.reflect.Field;
import java.util.concurrent.Executors;

import static jdk.test.lib.Asserts.assertTrue;

public class TestYieldEmptyQueue {
    public static void main(String[] args) throws Exception {
        assertTrue(Executors.newSingleThreadExecutor().submit(() -> {
            long sc = (long) new ObjAccess(SharedSecrets.getJavaLangAccess().getWispTask(Thread.currentThread()))
                    .ref("carrier").ref("counter").ref("switchCount").obj;
            Thread.yield();
            return (long) new ObjAccess(SharedSecrets.getJavaLangAccess().getWispTask(Thread.currentThread()))
                    .ref("carrier").ref("counter").ref("switchCount").obj == sc;        }).get());
    }

    static class ObjAccess {
        Object obj;

        ObjAccess(Object obj) {
            this.obj = obj;
        }

        ObjAccess ref(String field) {
            try {
                Field f;
                try {
                    f = obj.getClass().getDeclaredField(field);
                } catch (NoSuchFieldException e) {
                    f = obj.getClass().getSuperclass().getDeclaredField(field);
                }
                f.setAccessible(true);
                return new ObjAccess(f.get(obj));
            } catch (ReflectiveOperationException e) {
                throw new Error(e);
            }
        }
    }
}
