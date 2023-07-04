/*
 * @test
 * @summary Test dumping with limited metaspace with loading of JVMCI related classes.
 *          VM should not crash but CDS dump will abort upon failure in allocating metaspace.
 * @library /lib/testlibrary /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 *          jdk.jartool/sun.tools.jar
 * @modules jdk.compiler
 * @run main/othervm -XX:+UnlockExperimentalVMOptions TestDumpUnsupportedCheck
 */

import jdk.test.lib.cds.CDSTestUtils;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

import java.util.ArrayList;
import java.util.List;
import java.io.*;

public class TestDumpUnsupportedCheck {

    private static final String CLASSLIST_FILE = "TestDumpUnsupportedCheck.classlist";
    private static final String ARCHIVE_FILE = "./TestDumpUnsupportedCheck.jsa";

    public static void main(String[] args) throws Exception {
        // create an archive using the classlist
        dumpArchive();
    }

    static void dumpArchive() throws Exception {
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(
            "-XX:+EagerAppCDS",
            "-XX:SharedClassListFile=" + System.getProperty("test.src", ".") + File.separator + CLASSLIST_FILE,
            "-XX:SharedArchiveFile=" + ARCHIVE_FILE,
            "-Xlog:cds,class+eagerappcds=trace",
            "-Xshare:dump",
            "-XX:MetaspaceSize=12M",
            "-XX:MaxMetaspaceSize=12M");

        OutputAnalyzer output = CDSTestUtils.executeAndLog(pb, "dump-archive");
        output.shouldContain("Loading classes to share").
               shouldContain("Preload Warning: Unsupported source with class TestSimple");
    }
}
