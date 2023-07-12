public class ProjectUtil {
    public static Project updateSource(Project baseProject, String targetArtifactId, JavaSource[] newSource, ExpectOutput expectOutput) {
        boolean[] found = new boolean[]{false};
        Project newProject = new Project(baseProject.getRunConf(),
                updateArtifact(targetArtifactId, baseProject.getArtifacts(), newSource, found),
                expectOutput);
        if (!found[0]) {
            throw new RuntimeException("Cannot find artifact with id : " + targetArtifactId);
        }
        return newProject;
    }

    private static Artifact[] updateArtifact(String targetArtifactId, Artifact[] artifacts, JavaSource[] newSource, boolean[] found) {
        Artifact[] newArtifacts = new Artifact[artifacts.length];
        for (int i = 0; i < artifacts.length; i++) {
            Artifact artifact = artifacts[i];
            if (artifact.getId().equals(targetArtifactId)) {
                newArtifacts[i] = artifact.clone(newSource);
                found[0] = true;
            } else {
                Artifact[] inner = artifact.getInnerArtifacts();
                if (inner != null) {
                    newArtifacts[i] = artifact.clone(updateArtifact(targetArtifactId, inner, newSource, found));
                } else {
                    newArtifacts[i] = artifact;
                }
            }
        }
        return newArtifacts;
    }
}
