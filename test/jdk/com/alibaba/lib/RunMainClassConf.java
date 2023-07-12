import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class RunMainClassConf implements RunConf {
    private final String mainClass;

    public RunMainClassConf(String mainClass) {
        this.mainClass = mainClass;
    }

    @Override
    public String[] buildJavaRunCommands(File buildDir, Artifact[] artifacts) {
        List<String> commands = new ArrayList<>();
        commands.add(mainClass);
        return commands.toArray(new String[commands.size()]);
    }

    @Override
    public String mainClass() {
        return mainClass;
    }
}
