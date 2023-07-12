import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RunWithEmptyURLsURLClassLoaderConf implements RunConf {
    private final String mainClass;

    public RunWithEmptyURLsURLClassLoaderConf(String mainClass) {
        this.mainClass = mainClass;
    }

    @Override
    public String[] buildJavaRunCommands(File buildDir, Artifact[] artifacts) {
        List<String> commands = new ArrayList<>();
        commands.add("EmptyURLsURLClassLoadLauncher");
        commands.add(mainClass);
        if (artifacts != null && artifacts.length > 0) {
            for (Artifact artifact : artifacts) {
                if (loadWithURLClassLoader(artifact)) {
                    commands.add(new File(buildDir, artifact.getFileRelativePath()).getAbsolutePath());
                }
            }
        }
        return commands.toArray(new String[commands.size()]);
    }

    @Override
    public List<String> classpath(File buildDir, Artifact[] artifacts) {
        if (artifacts != null && artifacts.length > 0) {
            List<String> paths = new ArrayList<>();
            for (Artifact artifact : artifacts) {
                if (!loadWithURLClassLoader(artifact)) {
                    paths.add(new File(buildDir, artifact.getFileRelativePath()).getAbsolutePath());
                }
            }
            return paths;
        } else {
            return null;
        }
    }

    private boolean loadWithURLClassLoader(Artifact artifact) {
        return artifact.getOptions() != null && Arrays.asList(artifact.getOptions()).contains(ArtifactOption.LOAD_BY_URLCLASSLOADER);
    }

    @Override
    public String mainClass() {
        return mainClass;
    }
}
