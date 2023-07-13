import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class RunMainClassConf implements RunConf {
    private final String mainClass;
    private String[] params;

    public RunMainClassConf(String mainClass) {
        this.mainClass = mainClass;
    }

    public RunMainClassConf(String mainClass, String[] params) {
        this.mainClass = mainClass;
        this.params = params;
    }

    @Override
    public String[] buildJavaRunCommands(File buildDir, Artifact[] artifacts) {
        List<String> commands = new ArrayList<>();
        commands.add(mainClass);
        if (params != null) {
            for (String param : params) {
                commands.add(param);
            }
        }
        return commands.toArray(new String[commands.size()]);
    }

    @Override
    public String mainClass() {
        return mainClass;
    }
}
