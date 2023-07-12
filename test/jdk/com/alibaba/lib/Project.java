import java.io.IOException;
import java.util.*;

public class Project {
    private final Artifact[] artifacts;
    private final ExpectOutput expectOutput;
    private RunConf runConf;

    public Project(RunConf runConf, Artifact[] artifacts, ExpectOutput expectOutput) {
        this.runConf = runConf;
        this.artifacts = artifacts;
        this.expectOutput = expectOutput;
    }

    public void build(ProjectWorkDir workDir) throws IOException {
        workDir.resetBuildDir();
        Map<String, Artifact> artifactMap = new HashMap<>();
        List<Artifact> sorted = Artifact.topoSort(artifacts, artifactMap);
        for (Artifact artifact : sorted) {
            artifact.build(workDir, artifactMap, runConf);
        }
    }


    public Artifact[] getArtifacts() {
        return artifacts;
    }

    public ExpectOutput getExpectOutput() {
        return expectOutput;
    }

    public RunConf getRunConf() {
        return runConf;
    }
}
