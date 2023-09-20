
/*
 * @test
 * @library /test/lib
 * @summary Test fix of WispEngine block on Thread.class lock
 * @modules java.base/java.lang:+open
 * @requires os.family == "linux"
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseWisp2 TestThreadObjGC
 */

import java.util.Arrays;
import java.util.List;

import jdk.test.lib.JDKToolLauncher;
import jdk.test.lib.Utils;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

public class TestThreadObjGC {

    private static ProcessBuilder processBuilder = new ProcessBuilder();

    public static void main(String[] args) throws Exception {
        new TestThread(() -> {
            System.out.println("test");
        }).start();
        System.gc();
        Thread.sleep(1000);
        OutputAnalyzer output = jmap("-histo:live");
        output.shouldHaveExitValue(0);
        output.shouldNotContain("TestThreadObjGC$TestThread");
    }

    static class TestThread extends Thread {
        public TestThread(Runnable task) {
            super(task);
        }
    }

    private static OutputAnalyzer jmap(String... toolArgs) throws Exception {
        JDKToolLauncher launcher = JDKToolLauncher.createUsingTestJDK("jmap");
        launcher.addVMArgs(Utils.getTestJavaOpts());
        if (toolArgs != null) {
            for (String toolArg : toolArgs) {
                launcher.addToolArg(toolArg);
            }
        }
        launcher.addToolArg(Long.toString(ProcessTools.getProcessId()));

        processBuilder.command(launcher.getCommand());
        System.out.println(Arrays.toString(processBuilder.command().toArray()));
        OutputAnalyzer output = ProcessTools.executeProcess(processBuilder);
        System.out.println(output.getOutput());

        return output;
    }
}
