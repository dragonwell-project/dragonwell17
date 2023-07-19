import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public final class EagerAppCDSTestRunner extends QuickStartTestRunner {

    ProjectWorkDir projectWorkDir;

    @Override
    protected void run(SingleProjectProvider projectProvider) throws Exception {
        String workDir = System.getProperty("user.dir");
        projectWorkDir = new ProjectWorkDir(workDir + File.separator + "a");
        Project project = projectProvider.getProject();
        project.build(projectWorkDir);
        runAsTracer(project, projectWorkDir);
        runAsReplayer(project, projectWorkDir);
    }

    @Override
    protected void run(PairProjectProvider projectProvider) throws Exception {
        throw new RuntimeException("Not support!");
    }

    @Override
    public String[] getQuickStartOptions(File cacheDir) {
        return new String[]{"-Xquickstart:path=" + cacheDir.getAbsolutePath(), "-XX:+IgnoreAppCDSDirCheck", "-Xquickstart:verbose", "-Xlog:class+eagerappcds=trace",
        "--add-exports=java.base/jdk.internal.util.xml.impl=ALL-UNNAMED",
        "--add-exports=java.base/jdk.internal.org.xml.sax=ALL-UNNAMED",
        "--add-exports=java.base/jdk.internal.access=ALL-UNNAMED", 
        "--add-exports=java.base/jdk.internal.org.xml.sax.helpers=ALL-UNNAMED"};
    }

    @Override
    void postCheck() throws Exception {
        //check if all classes and jars has been removed.
        List<Path> files = Files.walk(projectWorkDir.getCacheDir().toPath())
                .filter((f) -> (f.endsWith(".jar") || f.endsWith(".class")))
                .collect(Collectors.toList());
        if (!files.isEmpty()) {
            files.forEach((f) -> System.out.println("Found file :" + f.toFile().getAbsolutePath() + " in cache directory!"));
            throw new RuntimeException("Cache directory " + projectWorkDir.getCacheDir().getAbsolutePath() + " should not contain jars and class file!");
        }
    }
}
