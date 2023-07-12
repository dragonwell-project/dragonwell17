import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

public class JarSigner {
    private String keystoreFilePath;
    private String alias;
    private String keystorePass;

    public JarSigner(String keystoreFilePath, String alias, String keystorePass) {
        this.keystoreFilePath = keystoreFilePath;
        this.alias = alias;
        this.keystorePass = keystorePass;
    }

    public void sign(String destJar) throws IOException {
        String jarsigner = jdk.test.lib.JDKToolFinder.getJDKTool("jarsigner");

        ArrayList<String> args = new ArrayList<>();
        args.add(jarsigner);
        args.add("-keystore");
        args.add(keystoreFilePath);
        args.add("-storepass");
        args.add(keystorePass);
        args.add(destJar);
        args.add(alias);

        // Reporting
        StringBuilder cmdLine = new StringBuilder();
        for (String cmd : args)
            cmdLine.append(cmd).append(' ');
        System.out.println("Command line: [" + cmdLine.toString() + "]");
        ProcessBuilder pb = new ProcessBuilder(args.toArray(new String[args.size()]));
        jdk.test.lib.process.OutputAnalyzer output = new jdk.test.lib.process.OutputAnalyzer(pb.start());
        output.shouldHaveExitValue(0);
    }
}
