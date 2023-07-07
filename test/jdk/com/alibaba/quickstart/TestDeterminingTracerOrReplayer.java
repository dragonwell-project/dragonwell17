/*
 * @test
 * @summary Test the flow to determine tracer or replayer
 * @library /test/lib
 * @run main/othervm TestDeterminingTracerOrReplayer
 */

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

public class TestDeterminingTracerOrReplayer {
    private static String cachepath = System.getProperty("user.dir");
    public static void main(String[] args) throws Exception {
        TestDeterminingTracerOrReplayer test = new TestDeterminingTracerOrReplayer();
        cachepath = cachepath + "/determine";
        test.verifyDetermine();
        test.verifyDestroy();
    }

    void verifyDetermine() throws Exception {
        destroyCache();
        runAsTracer();
        runAsReplayer();
    }

    void runAsTracer() throws Exception {
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder("-Xquickstart:path=" + cachepath, "-Xquickstart:verbose", "-version");
        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        output.shouldContain("Running as tracer");
        output.shouldHaveExitValue(0);
    }

    void runAsReplayer() throws Exception {
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder("-Xquickstart:path=" + cachepath, "-Xquickstart:verbose", "-version");
        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        output.shouldContain("Running as replayer");
        output.shouldHaveExitValue(0);
    }

    void destroyCache() throws Exception {
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder("-Xquickstart:path=" + cachepath, "-Xquickstart:verbose,destroy", "-version");
        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        output.shouldContain("destory the cache folder");
        output.shouldHaveExitValue(0);
    }


    void verifyDestroy() throws Exception {
        runAsReplayer();
        destroyCache();
        runAsTracer();
    }
}

