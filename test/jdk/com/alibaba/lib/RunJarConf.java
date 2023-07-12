import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class RunJarConf implements RunConf {

    private final String mainClass;
    private final String mainJarArtifactId;

    public RunJarConf(String mainClass, String mainJarArtifactId) {
        this.mainClass = mainClass;
        this.mainJarArtifactId = mainJarArtifactId;
    }

    @Override
    public String[] buildJavaRunCommands(File buildDir, Artifact[] artifacts) {
        List<String> commands = new ArrayList<>();
        for (Artifact artifact : artifacts) {
            if (artifact.getId().equals(mainJarArtifactId)) {
                commands.add("-jar");
                commands.add(new File(buildDir, artifact.getFileRelativePath()).getAbsolutePath());
            }
        }
        return commands.toArray(new String[commands.size()]);
    }

    @Override
    public String mainClass() {
        return mainClass;
    }
}
