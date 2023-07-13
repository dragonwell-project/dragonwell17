/*
 * @test
 * @summary Test -Xquickstart:replay option
 * @library /test/lib
 * @requires os.arch=="amd64"
 * @run main/othervm/timeout=600 TestForceReplayer
 */

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

public class TestForceReplayer {
    private static String cachepath = System.getProperty("user.dir");
    public static void main(String[] args) throws Exception {
        TestForceReplayer test = new TestForceReplayer();
        cachepath = cachepath + "/dir-does-not-exist";
        test.runWithoutCache();
    }

    void runWithoutCache() throws Exception {
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder("-Xquickstart:path=" + cachepath, "-Xquickstart:replay,verbose" + Config.QUICKSTART_FEATURE_COMMA, "-version");
        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        System.out.println(output.getStdout());
        output.shouldContain("QuickStart replay role is specified without shared directory found. Running as a normal process with quickstart disabled.");
        output.shouldHaveExitValue(0);
    }
}

