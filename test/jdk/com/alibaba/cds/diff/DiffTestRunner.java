import jdk.test.lib.process.OutputAnalyzer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public final class DiffTestRunner extends QuickStartTestRunner {
    private final String SERVERLESS_LIB = String.join(File.separator, System.getProperty("java.home"), "lib", "serverless", "serverless-adapter.jar");
    private final String DIFF_MAIN_CLASS = "com.alibaba.jvm.cds.diff.DiffClasses";

    private final String[] STATIC_DIFF_OPTIONS  = new String[] {
            "-XX:+EagerAppCDSStaticClassDiffCheck"
    };
    private final String[] DYNAMIC_DIFF_OPTIONS = new String[] {
            "-XX:+EagerAppCDSDynamicClassDiffCheck",
            "-Dcom.alibaba.cds.dynamic.jtregTest=true",
            "-Dcom.alibaba.cds.dynamic.logLevel=TRACE",
    };

    @Override
    protected void run(SingleProjectProvider projectProvider) throws Exception {
        throw new RuntimeException("Not support!");
    }

    @Override
    protected void run(PairProjectProvider projectProvider) throws Exception {
        final boolean doClassDiff = projectProvider.isStaticDiff;

        ProjectWorkDir wda = new ProjectWorkDir(workDir + File.separator + "a");
        ProjectWorkDir wdb = new ProjectWorkDir(workDir + File.separator + "b");

        //Trace&Replay for version A
        Project pa = projectProvider.versionA();
        pa.build(wda);
        runAsTracer(pa, wda, doClassDiff);

        if (doClassDiff) {
            diffClasses(wda, pa);
        }
        runAsReplayer(pa, wda, doClassDiff);

        //Build another version B
        Project pb = projectProvider.versionB();
        pb.build(wdb);


        //backup origin classes,and copy new classes to origin build directory.
        wda.backupBuild();
        wdb.copyClasses(wda);

        //generate diff class list
        if (doClassDiff) {
            diffClasses(wda, pa);
        }
        runAsReplayer(pb, wda, doClassDiff);
    }

    @Override
    public String[] getQuickStartOptions(File cacheDir, boolean doClassDiff) {
        final String[] basicOptions = new String[]{"-Xquickstart:path=" + cacheDir.getAbsolutePath(),
                "-XX:+IgnoreAppCDSDirCheck", "-Xquickstart:verbose", "-Xlog:class+eagerappcds=trace"};
        final String[] additionalCDSOptions = doClassDiff ? STATIC_DIFF_OPTIONS : DYNAMIC_DIFF_OPTIONS;
        return Stream.concat(Arrays.stream(basicOptions), Arrays.stream(additionalCDSOptions)).toArray(String[]::new);
    }

    private void diffClasses(ProjectWorkDir pwd, Project project) throws IOException {
        File libDirFile = createLibDirFile(pwd, project);
        ProcessBuilder pb = createJavaProcessBuilder(List.of(SERVERLESS_LIB),
                DIFF_MAIN_CLASS,
                "--cache-dir",
                pwd.getCacheDir().getAbsolutePath(),
                "--lib-dir-file",
                libDirFile.getCanonicalPath());
        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        output.shouldContain("Write diff result to");
        output.shouldHaveExitValue(0);
    }

    private File createLibDirFile(ProjectWorkDir workDir, Project project) throws IOException {
        File libDirFile = new File(workDir.getPlayground() + File.separator + "libdir" + System.nanoTime() + ".lst");
        if (libDirFile.exists()) {
            libDirFile.delete();
        }
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(libDirFile))) {
            for (Artifact artifact : project.getArtifacts()) {
                switch (artifact.getPackageType()) {
                    case CLASSES:
                        bw.write("class," + (new File(workDir.getBuild(), artifact.getDeployDir()).getCanonicalPath()));
                        bw.newLine();
                        break;
                    case FAT_JAR:
                    case PLAIN_JAR:
                        bw.write("jar," + ((new File(workDir.getBuild(), artifact.getDeployDir()).getCanonicalPath())));
                        bw.newLine();
                        break;
                }
            }
            for (String cp : System.getProperty("test.classes").split(File.pathSeparator)) {
                File f = new File(cp);
                if (f.isDirectory()) {
                    bw.write("class," + cp);
                    bw.newLine();
                } else {
                    bw.write("jar," + cp);
                    bw.newLine();
                }
            }
        }
        return libDirFile;
    }
}
