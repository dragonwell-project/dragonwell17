import jdk.test.lib.Asserts;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import sun.security.action.GetPropertyAction;

import java.io.File;
import java.io.IOException;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;


/*
 * @test
 * @modules java.base/sun.security.action
 * @summary Test quick start dump when enable AOT.
 * @library /test/lib
 * @library /lib/testlibrary
 * @requires os.arch=="amd64"
 * @run main/othervm/timeout=1200 TestArgumentVerification
 */
public class TestArgumentVerification {

    private static String workDir = AccessController.doPrivileged(new GetPropertyAction("test.classes"));
    private static File cacheDir = createCacheDir(workDir);
    private static ArrayList<String> postBasicVmOpts = new ArrayList<>();
    static {
        try {
            // initialize basicVmOpts
            Collections.addAll(postBasicVmOpts, new String[] {
                    "-Xquickstart:path=" + cacheDir.getCanonicalPath(),
                    "-Xquickstart:verbose," + Config.QUICKSTART_FEATURE,
                    "-XX:+UnlockDiagnosticVMOptions",
                    "-XX:+UseAOTStrictLoading",
                    "-version"
                    // no other arguments
            });
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    public static class Pair {
        public String[] tracerOpts;
        public String[] replayerOpts;
        public boolean shouldPass;

        public Pair(String[] tracerOpts, String[] replayerOpts, boolean shouldPass) {
            this.tracerOpts = tracerOpts;
            this.replayerOpts = replayerOpts;
            this.shouldPass = shouldPass;
        }
    }

    private static final ArrayList<Pair> testOptsCombinations = new ArrayList<>();
    static {
        // test CompressedOops
        testOptsCombinations.add(new Pair(
                new String[] { "-XX:+UseCompressedOops" },
                new String[] { "-XX:-UseCompressedOops" },
                false
        ));
        testOptsCombinations.add(new Pair(
                new String[] { "-XX:-UseCompressedOops" },
                new String[] { "-XX:+UseCompressedOops" },
                false
        ));
        // UseTLAB
        testOptsCombinations.add(new Pair(
                new String[] { "-XX:-UseTLAB" },
                new String[] { "-XX:+UseTLAB" },
                false
        ));
    }

    public static void main(String[] args) throws Exception {
        test();
    }

    static void test() throws Exception {
        for (int i = 0; i < testOptsCombinations.size(); i++) {
            destroyCache(cacheDir);
            runTracer(i);
            runReplayer(i);
        }
    }

    static void runTracer(int index) throws Exception {
        ArrayList<String> vmOpts = new ArrayList<>();
        vmOpts.addAll(Arrays.asList(testOptsCombinations.get(index).tracerOpts));
        vmOpts.addAll(postBasicVmOpts);

        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(vmOpts.toArray(new String[vmOpts.size()]));
        pb.redirectErrorStream(true);
        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        System.out.println("[Tracer Output] " + output.getOutput());
        output.shouldContain(TestDump.ANCHOR);
        output.shouldHaveExitValue(0);
    }

    static void runReplayer(int index) throws Exception {
        ArrayList<String> vmOpts = new ArrayList<>();
        vmOpts.addAll(Arrays.asList(testOptsCombinations.get(index).replayerOpts));
        vmOpts.addAll(postBasicVmOpts);

        boolean shouldPass = testOptsCombinations.get(index).shouldPass;

        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(vmOpts.toArray(new String[vmOpts.size()]));
        pb.redirectErrorStream(true);
        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        System.out.println("[Replayer Output] " + output.getOutput());
        if (shouldPass) {
            output.shouldHaveExitValue(0);
        } else {
            Asserts.assertTrue(output.getExitValue() != 0);
        }
    }

    private static File createCacheDir(String workDir) {
        File cacheDir = new File(workDir, "sharedCache");
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }
        return cacheDir;
    }

    static void destroyCache(File cacheDir) throws Exception {
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder("-Xquickstart:destroy", "-Xquickstart:path=" + cacheDir.getCanonicalPath(), "-Xquickstart:verbose", "-version");
        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        output.shouldContain("destroy the cache folder");
        output.shouldHaveExitValue(0);
    }


}
