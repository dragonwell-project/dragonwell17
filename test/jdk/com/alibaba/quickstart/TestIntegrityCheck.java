/*
 * @test
 * @summary Test Integrity Check
 * @library /test/lib
 * @run main/othervm TestIntegrityCheck
 */

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

public class TestIntegrityCheck {
    private static String cachepath = System.getProperty("user.dir");
    public static void main(String[] args) throws Exception {
        TestIntegrityCheck test = new TestIntegrityCheck();
        cachepath = cachepath + "/integrityCheck";
        test.verifyIntegrity();
        test.verifyImageEnvChange();
        test.verifyOptionChange();
    }

    void verifyIntegrity() throws Exception {
        runAsTracer();
        runAsReplayer();
    }

    void verifyImageEnvChange() throws Exception {
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder("-Xquickstart:path=" + cachepath, "-Xquickstart:verbose,containerImageEnv=pouchid", "-version");
        pb.environment().put("pouchid", "123456");
        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        output.shouldContain("Container image isn't the same");
        output.shouldHaveExitValue(0);
    }

    void verifyOptionChange() throws Exception {
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder("-Xquickstart:path=" + cachepath, "-Xquickstart:verbose,containerImageEnv=pouchid", "-esa", "-version");
        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        output.shouldContain("JVM option isn't the same");
        output.shouldHaveExitValue(0);
    }

    void runAsTracer() throws Exception {
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder("-Xquickstart:path=" + cachepath, "-Xquickstart:verbose,containerImageEnv=pouchid", "-version");
        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        output.shouldContain("Running as tracer");
        output.shouldHaveExitValue(0);
    }

    void runAsReplayer() throws Exception {
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder("-Xquickstart:path=" + cachepath, "-Xquickstart:verbose,containerImageEnv=pouchid", "-version");
        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        output.shouldContain("Running as replayer");
        output.shouldHaveExitValue(0);
    }
}

