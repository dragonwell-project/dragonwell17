/**
 * @test
 * @summary test main class with parameter like a VM option.
 * @library /test/lib /com/alibaba/lib
 * @requires os.arch=="amd64"
 * @run main/othervm TestEagerAppCDSWithParametersProfileDump
 */
public class TestEagerAppCDSWithParametersProfileDump implements SingleProjectProvider {
    public static void main(String[] args) throws Exception {
        new ProfileDumpTestRunner(QuickStartFeature.EAGER_APPCDS).run(new TestEagerAppCDSWithParametersProfileDump());
    }

    @Override
    public Project getProject() {
        return project;
    }

    private Project project = new Project(new RunMainClassConf("com.z.Main",new String[]{"-XX:+ArbitraryOptionLikeVMOption"}),
            new Artifact[]{
                    Artifact.createPlainJar("foo", "foo-lib", "add-sub.1.0.jar", new String[]{"bar"}, SourceConstants.ADD_SUB_SOURCE),
                    Artifact.createPlainJar("bar", "bar-lib", "mul-div-1.0.jar", null, SourceConstants.MUL_DIV_SOURCE)
            },
            new ExpectOutput(new String[]{"30", "90", "48"
            }));
}
