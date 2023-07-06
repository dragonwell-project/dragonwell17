/*
 * @test
 * @summary Test -Xquickstart options
 * @library /test/lib
 * @run main/othervm TestQuickStartOptions
 */

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

public class TestQuickStartOptions {
    public static void main(String[] args) throws Exception {
        TestQuickStartOptions test = new TestQuickStartOptions();
        test.verifyPathSetting();
        test.verifyInvalidOptCheck();
    }

    void verifyPathSetting() throws Exception {
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder("-Xquickstart:path=/a/b/c", "-Xquickstart:verbose", "-version");
        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        output.shouldContain("[QuickStart] cache path is set from");
        output.shouldHaveExitValue(0);
    }

    void verifyInvalidOptCheck() throws Exception {
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder("-Xquickstart:jwmup,verbose", "-version");
        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        output.shouldContain("Invalid -Xquickstart option");
        output.shouldHaveExitValue(1);
    }
}

