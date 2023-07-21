/*
 * @test
 * @library /test/lib
 * @summary Verify park not happened for a very small interval
 * @modules java.base/com.alibaba.wisp.engine:+open
 * @run main/othervm  -XX:+UseWisp2 TestYieldFewNanos
 */

import com.alibaba.wisp.engine.WispEngine;

import java.util.concurrent.Executors;
import java.util.concurrent.locks.LockSupport;

import static jdk.test.lib.Asserts.assertTrue;

public class TestYieldFewNanos {
    public static void main(String[] args) throws Exception {
        assertTrue(Executors.newSingleThreadExecutor().submit(() -> {
            long pc = (long) new TestYieldEmptyQueue.ObjAccess(WispEngine.current()).ref("counter").ref("parkCount").obj;
            LockSupport.parkNanos(1);
            return (long) new TestYieldEmptyQueue.ObjAccess(WispEngine.current()).ref("counter").ref("parkCount").obj == pc;
        }).get());
    }
}
