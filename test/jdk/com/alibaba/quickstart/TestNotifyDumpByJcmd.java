/*
 * @test
 * @modules java.base/sun.security.action
 * @summary Test dumping using jcmd
 * @library /test/lib
 * @build TestDump
 * @requires os.arch=="amd64"
 * @run driver jdk.test.lib.helpers.ClassFileInstaller  -jar test.jar TestDump TestDump$Policy TestDump$ClassLoadingPolicy TestDump$WatcherThread
 * @run main/othervm TestNotifyDumpByJcmd
 */

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import sun.security.action.GetPropertyAction;

import java.io.File;
import java.security.AccessController;

public class TestNotifyDumpByJcmd {

    private static final String TESTJAR = "./test.jar";
    private static final String TESTCLASS = "TestDump";

    public static void main(String[] args) throws Exception {
        String dir = AccessController.doPrivileged(new GetPropertyAction("test.classes"));
        TestNotifyDumpByJcmd.verifyPathSetting(dir);
        new File(dir).delete();
    }

    static void verifyPathSetting(String parentDir) throws Exception {
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(
                "-Xquickstart:path=" + parentDir + "/quickstartcache",
                "-Xquickstart:verbose",
                // In sleeping condition there is no classloading happens,
                // we will consider it as the start-up finish
                "-DcheckIntervalMS=" + TestDump.SLEEP_MILLIS,
                "-DtestJcmd=true",
                "-cp",
                TESTJAR,
                TESTCLASS);
        pb.redirectErrorStream(true);
        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        System.out.println("[Child Output] " + output.getOutput());
        output.shouldContain(TestDump.ANCHOR);
        output.shouldHaveExitValue(0);
    }

}
