import jdk.test.lib.process.OutputAnalyzer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public final class StaticDiffTestRunner extends QuickStartTestRunner {
    private final String SERVERLESS_LIB = String.join(File.separator, System.getProperty("java.home"), "lib", "serverless", "serverless-adapter.jar");
    private final String DIFF_MAIN_CLASS = "com.alibaba.jvm.cds.diff.DiffClasses";

    @Override
    protected void run(SingleProjectProvider projectProvider) throws Exception {
        throw new RuntimeException("Not support!");
    }

    @Override
    protected void run(PairProjectProvider projectProvider) throws Exception {
        ProjectWorkDir wda = new ProjectWorkDir(workDir + File.separator + "a");
        ProjectWorkDir wdb = new ProjectWorkDir(workDir + File.separator + "b");

        //Trace&Replay for version A
        Project pa = projectProvider.versionA();
        pa.build(wda);
        runAsTracer(pa, wda);

        diffClasses(wda, pa);
        runAsReplayer(pa, wda);

        //Build another version B
        Project pb = projectProvider.versionB();
        pb.build(wdb);


        //backup origin classes,and copy new classes to origin build directory.
        wda.backupBuild();
        wdb.copyClasses(wda);

        //generate diff class list
        diffClasses(wda, pa);
        runAsReplayer(pb, wda);
    }

    @Override
    public String[] getQuickStartOptions(File cacheDir) {
        return new String[]{"-Xquickstart:path=" + cacheDir.getAbsolutePath(),
                "-XX:+IgnoreAppCDSDirCheck", "-XX:+EagerAppCDSStaticClassDiffCheck", "-Xquickstart:verbose", "-Xlog:class+eagerappcds=trace"};
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
