/*
 * @test
 * @summary Test dumping for custom classloaders with JVMTI ClassfileHook and transforming.
 * @library /test/lib /test/hotspot/jtreg/runtime/cds/appcds
 * @modules java.base/jdk.internal.misc
 *          java.management
 *          jdk.jartool/sun.tools.jar
 * @modules jdk.compiler
 * @modules java.base/com.alibaba.util:+open
 * @modules java.base/jdk.internal.loader:+open
 * @modules java.base/sun.security.action
 * @build generatePackageInfo.Simple
 * @build LoadWithCustomClassLoader
 * @requires os.arch=="amd64"
 * @run driver jdk.test.lib.helpers.ClassFileInstaller -jar test.jar LoadWithCustomClassLoader
 * @run main/othervm/native -XX:+UnlockExperimentalVMOptions TestClassLoaderWithJVMTIAgent
 */
import com.alibaba.util.QuickStart;
import com.alibaba.util.Utils;
import jdk.test.lib.cds.CDSTestUtils;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

import java.io.File;
import java.nio.file.Path;
import java.io.*;

/**
 * Run by:
 * <jtreg> -v:error,fail -jdk:<jdk> -nativepath:<images>/test/hotspot/jtreg/native <this test>
 *
 * The native lib is generated by:
 *   make test-image-hotspot-jtreg-native CONF=<conf>
 *
 * To reproduce a crash:
 *
 * # A fatal error has been detected by the Java Runtime Environment:
 * #
 * #  SIGSEGV (0xb) at pc=0x00007f06d67b8518, pid=118431, tid=118439
 * #
 * # JRE version: OpenJDK Runtime Environment (11.0.14.13+39) (build 11.0.14.13-AJDK+551-Alibaba)
 * # Java VM: OpenJDK 64-Bit Server VM (11.0.14.13-AJDK+551-Alibaba, mixed mode, sharing, tiered, compressed oops, g1 gc, linux-amd64)
 * # Problematic frame:
 * # V  [libjvm.so+0xb37518]  InstanceKlass::module() const+0x18
 * #
 * # No core dump will be written. Core dumps have been disabled. To enable core dumping, try "ulimit -c unlimited" before starting Java again
 * #
 * # An error report file with more information is saved as:
 * # /home/yunyao.zxl/jdk11/JTwork/scratch/0/hs_err_pid118431.log
 * #
 * # If you would like to submit a bug report, please visit:
 * #   https://bugreport.java.com/bugreport/crash.jsp
 * #
 * ];
 *  stderr: [Agent library loaded with options = generatePackageInfo/Simple,XXX,YYY
 * CLASS_NAME = generatePackageInfo/Simple, FROM = XXX, TO = YYY
 * found class to be hooked: generatePackageInfo/Simple - rewriting ...
 * Rewriting done. Replaced 2 occurrence(s) of "XXX" to "YYY"
 * ]
 */

public class TestClassLoaderWithJVMTIAgent {

    private static final String TESTJAR = "./test.jar";
    private static final String TESTNAME = "LoadWithCustomClassLoader";

    private static final String CLASSLIST_FILE = "./TestDumpAndLoadClassWithNullURL.classlist";
    private static final String CLASSLIST_FILE_2 = "./TestDumpAndLoadClassWithNullURL.classlist2";
    private static final String ARCHIVE_FILE = "./TestDumpAndLoadClassWithNullURL.jsa";

    private static final String TEST_CLASS = System.getProperty("test.classes");

    public static void main(String[] args) throws Exception {
        String name = System.getProperty("os.name");
        if (name.equals("Linux")) {
            traceClasses();
            replaceFingerprint();
            convertClassList();
            dumpArchive();
            startWithJsa();
        }
    }

    static void traceClasses() throws Exception {
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(
            "-Dtest.classes=" + TEST_CLASS,
            "-Xlog:class+eagerappcds=trace",
            "-XX:DumpLoadedClassList=" + CLASSLIST_FILE,
            "-Xlog:class+path=info",
            "-agentlib:SimpleClassFileLoadHook=generatePackageInfo/Simple,XXX,YYY",
            // trigger JVMCI runtime init so that JVMCI classes will be
            // included in the classlist
            "--add-exports=java.base/jdk.internal.loader=ALL-UNNAMED",
            "-XX:+UnlockExperimentalVMOptions",
            "-XX:+EagerAppCDS",
            "-XX:-UseSharedSpaces",
            "-cp",
            TESTJAR,
            TESTNAME);

        OutputAnalyzer output = CDSTestUtils.executeAndLog(pb, "dump-loaded-classes");
        System.out.println("==start==\n" + output.getOutput() + "\n ==end==");
        output.shouldHaveExitValue(0);
    }

    private static void replaceFingerprint() {
        // skip the fingerprint check by the SimpleClassFileLoadHook's transform
        Utils.runProcess(true, "[Fingerprint Modifier] ", null,
                "sed",
                "-i",
                "s/ fingerprint.*//",
                CLASSLIST_FILE
        );
    }

    private static Path getJDKHome() {
        String jdkHome = System.getProperty("java.home");
        if (!new File(jdkHome).exists()) {
            throw new Error("Fatal error, cannot find jdk path: [" + jdkHome + "] doesn't exist!");
        }
        return Path.of(jdkHome);
    }
    private static Path getJDKLibDir() {
        return getJDKHome().resolve("lib");
    }

    static void convertClassList() throws Exception {
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(
                "--add-exports",
                "java.base/jdk.internal.misc=ALL-UNNAMED",
                "-cp",
                Path.of(Utils.getJDKHome(), "lib", QuickStart.getServerlessAdapter()).toString(),
                "com.alibaba.jvm.cds.Classes4CDS",
                CLASSLIST_FILE,
                CLASSLIST_FILE_2);

        OutputAnalyzer output = CDSTestUtils.executeAndLog(pb, "convert-class-list");
        output.shouldHaveExitValue(0);

    }
    static void dumpArchive() throws Exception {
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(
                "-cp",
                TESTJAR,
                "-XX:+UnlockExperimentalVMOptions",
                "-XX:+EagerAppCDS",
                "-XX:SharedClassListFile=" + CLASSLIST_FILE_2,
                "-XX:SharedArchiveFile=" + ARCHIVE_FILE,
                "-Xlog:class+eagerappcds=trace,cds=info",
                "--add-exports=java.base/jdk.internal.loader=ALL-UNNAMED",
                "-Xshare:dump",
                "-XX:MetaspaceSize=12M",
                "-XX:MaxMetaspaceSize=12M");

        OutputAnalyzer output = CDSTestUtils.executeAndLog(pb, "dump-archive");
        int exitValue = output.getExitValue();
        if (exitValue == 1) {
            output.shouldContain("Failed allocating metaspace object type");
        } else if (exitValue == 0) {
            output.shouldContain("Loading classes to share");
        } else {
            throw new RuntimeException("Unexpected exit value " + exitValue);
        }
    }

    static void startWithJsa() throws Exception {
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(
                "-Dtest.classes=" + TEST_CLASS,
                "-XX:+UnlockExperimentalVMOptions",
                "-XX:+EagerAppCDS",
                "-Xshare:on",
                "-XX:SharedArchiveFile=" + ARCHIVE_FILE,
                "-agentlib:SimpleClassFileLoadHook=generatePackageInfo/Simple,XXX,YYY",
                "-Xlog:class+eagerappcds=trace",
                "--add-exports=java.base/jdk.internal.loader=ALL-UNNAMED",
                "-cp",
                TESTJAR,
                TESTNAME);

        OutputAnalyzer output = CDSTestUtils.executeAndLog(pb, "start-with-shared-archive");
        output.shouldHaveExitValue(0);
        output.shouldNotContain("[CDS load class Failed");
    }

}
