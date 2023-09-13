/*
 * @test
 * @summary Verify wisp internal logic can not be preempted
 * @modules java.base/jdk.internal.access
 * @library /test/lib
 * @requires os.family == "linux"
 * @run main TestPreemptWispInternalBug
 */

import com.alibaba.wisp.engine.WispEngine;
import com.alibaba.wisp.engine.WispTask;
import jdk.internal.access.SharedSecrets;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

import java.util.concurrent.FutureTask;

/*
 * In the old test design, we run a wisp task that always invokes
 * task.toString(). When the first GC happens (initialized by other threads at
 * JVM boots up), the wisp task is stopped at POLL_AT_RETURN with high
 * probability. The wisp task's stack is considered as critical and print
 * message at the moment. This test design depends on the first GC at JVM
 * boots up. If wisp task misses the POLL_AT_RETURN at the first GC, the test
 * will fail. So the test failure's probability is high.
 *
 * We implement a new test design to leverage the test success probability.
 * The new design employs a new dedicated wisp task that continuously generate
 * temporary objects garbage to trigger GC continuously. So the another wisp
 * task can stop at POLL_AT_RETURN with higher probability. The new test design
 * is more robust.
 */
public class TestPreemptWispInternalBug {
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(
                    "-XX:+UnlockExperimentalVMOptions",
                    "-XX:+UseWisp2", "-XX:+UnlockDiagnosticVMOptions", "-XX:+VerboseWisp",
                    "-XX:-Inline", "-Xmn32M",
                    "--add-exports=java.base/jdk.internal.access=ALL-UNNAMED",
                    TestPreemptWispInternalBug.class.getName(), "1000");
            OutputAnalyzer output = new OutputAnalyzer(pb.start());
            output.shouldContain("[WISP] preempt was blocked, because wisp internal method on the stack");
            return;
        }

        // Make test duration can be controlled.
        long durationMillisArg = 1000;
        try {
            durationMillisArg = Integer.parseInt(args[0]);
        } catch (Exception ex) {
        }

        final long durationMillis = durationMillisArg;
        final long start = System.currentTimeMillis();
        FutureTask<Void> future1 = new FutureTask<>(() -> {
            Object task = SharedSecrets.getWispEngineAccess().getCurrentTask();
            while (System.currentTimeMillis() - start < durationMillis) {
                for (int i = 0; i < 100; i++) {
                    task.toString();
                }
            }
            return null;
        });

        // Employ another new dedicated wisp task to generate temporary objects
        // garbage. Hope that it will trigger many GCs and stop the other
        // wisp task at POLL_AT_RETURN or POLL_AT_LOOP.
        FutureTask<Void> future2 = new FutureTask<>(() -> {
            Object task = SharedSecrets.getWispEngineAccess().getCurrentTask();
            while (System.currentTimeMillis() - start < durationMillis) {
                new Object[4096].hashCode();
            }
            return null;
        });

        WispEngine.dispatch(future1);
        WispEngine.dispatch(future2);
        future1.get();
        future2.get();
    }
}
