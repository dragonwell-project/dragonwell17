/*
 * @test
 * @summary Test the config compatibility in different wisp version
 * @library /test/lib
 * @run main ConfigurationCompatibilityCheckTest
 */
import java.util.ArrayList;
import java.util.Arrays;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

public class ConfigurationCompatibilityCheckTest {
    public static void main(String[] args) throws Exception {
        incompatibility("-Dcom.alibaba.wisp.version=65536");
        incompatibility("-Dcom.alibaba.wisp.enableThreadAsWisp=true");
        incompatibility("-Dcom.alibaba.wisp.enableThreadAsWisp=true", "-Dcom.alibaba.wisp.transparentWispSwitch=false");
        incompatibility("-Dcom.alibaba.wisp.version=2", "-Dcom.alibaba.globalPoller=false");
        incompatibility("-Dcom.alibaba.wisp.allThreadAsWisp=true");
        incompatibility("-Dcom.alibaba.wisp.allThreadAsWisp=true", "-Dcom.alibaba.wisp.version=1");
        incompatibility("-Dcom.alibaba.wisp.allThreadAsWisp=true", "-Dcom.alibaba.wisp.enableThreadAsWisp=false");
    }


    private static void incompatibility(String... args) throws Exception {
        ArrayList<String> list = new ArrayList<>();
        list.add("-XX:+EnableCoroutine");
        list.addAll(Arrays.asList(args));
        list.add("-version");
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(list.toArray(new String[0]));
        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        output.shouldContain("IllegalArgumentException");
    }
}
