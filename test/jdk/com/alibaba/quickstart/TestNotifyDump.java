/*
 * @test
 * @modules java.base/sun.security.action
 * @summary Test dumping when process exits
 * @library /test/lib
 * @build TestDump
 * @run main/othervm TestNotifyDump
 */

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import sun.security.action.GetPropertyAction;

import java.io.File;
import java.security.AccessController;

public class TestNotifyDump {

    private static final String TESTCLASS = "TestDump";

    public static void main(String[] args) throws Exception {
        String dir = AccessController.doPrivileged(new GetPropertyAction("test.classes"));
        TestNotifyDump.verifyPathSetting(dir);
        new File(dir).delete();
    }

    static void verifyPathSetting(String parentDir) throws Exception {
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(
                "-Xquickstart:path=" + parentDir + "/quickstartcache",
                "-Xquickstart:verbose",
                // In sleeping condition there is no classloading happens,
                // we will consider it as the start-up finish
                "-DcheckIntervalMS=" + (TestDump.SLEEP_MILLIS / 5),
                TESTCLASS);
        pb.redirectErrorStream(true);
        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        System.out.println("[Child Output] " + output.getOutput());
        output.shouldContain(TestDump.ANCHOR);
        output.shouldHaveExitValue(0);
    }

}
