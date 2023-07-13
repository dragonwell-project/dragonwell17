/**
 * @test
 * @summary test quickstart when enable EagerAppCDS with the process: profile,dump,replay
 * @library /test/lib /com/alibaba/lib
 * @requires os.arch=="amd64"
 * @run main/othervm TestEagerAppCDSPlainJarProfileDump
 */
public class TestEagerAppCDSPlainJarProfileDump implements SingleProjectProvider {
    public static void main(String[] args) throws Exception {
        new ProfileDumpTestRunner(QuickStartFeature.EAGER_APPCDS).run(new TestEagerAppCDSPlainJarProfileDump());
    }

    @Override
    public Project getProject() {
        return project;
    }

    private Project project = new Project(new RunWithURLClassLoaderConf("com.z.Main"),
            new Artifact[]{
                    Artifact.createPlainJar("foo", "foo-lib", "add-sub.1.0.jar", new String[]{"bar"}, SourceConstants.ADD_SUB_SOURCE,
                            new ArtifactOption[]{ArtifactOption.LOAD_BY_URLCLASSLOADER}),
                    Artifact.createPlainJar("bar", "bar-lib", "mul-div-1.0.jar", null, SourceConstants.MUL_DIV_SOURCE,
                            new ArtifactOption[]{ArtifactOption.LOAD_BY_URLCLASSLOADER})
            },
            new ExpectOutput(new String[]{"30", "90", "48",
                    "Successful loading of class com/m/Multiply",
                    "Successful loading of class com/x/Add",
                    "Successful loading of class com/y/Sub"
            }));
}
