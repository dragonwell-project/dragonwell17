
/**
 * @test
 * @summary A class that no parent and no child class.
 * @library /test/lib /com/alibaba/lib
 * @requires os.arch=="amd64"
 * @run main/othervm TestLonelyClassChanged static
 * @run main/othervm TestLonelyClassChanged dynamic
 */
public class TestLonelyClassChanged extends PairProjectProvider {

    public TestLonelyClassChanged(String arg) {
        super(arg);
    }

    public static void main(String[] args) throws Exception {
        new DiffTestRunner().runTest(new TestLonelyClassChanged(args[0]));
    }

    @Override
    public Project versionA() {
        return baseProject;
    }

    @Override
    public Project versionB() {
        return currProject;
    }

    private JavaSource[] mainSource = new JavaSource[]{
            new JavaSource(
                    "Main", "public class Main",
                    null, null,
                    new JavaSource.MethodDesc[]{
                            new JavaSource.MethodDesc("main",
                                    "public static void main(String[] args) {" +
                                            "Bar bar = new Bar();" +
                                            "System.out.println(bar.value());" +
                                            "}"
                            )
                    }
            )
    };

    private JavaSource[] barSource = new JavaSource[]{
            new JavaSource(
                    "Bar", "public class Bar {" +
                    "public String value() {" +
                    "      return \"barbar\";" +
                    "  }" +
                    "}"
            ),
    };

    private Project baseProject = new Project(new RunMainClassConf("Main"),
            new Artifact[]{
                    Artifact.createPlainJar("bar", "lib", "bar.jar", null, barSource),
                    Artifact.createPlainJar("main", "lib", "main.jar", new String[]{"bar"}, mainSource)
            },
            new ExpectOutput(new String[]{"barbar"}));


    private Project currProject = ProjectUtil.updateSource(baseProject, "bar", JavaSourceUtil.updateClass(barSource, "Bar", "public class Bar {" +
                    "private final int flag = 100;" +
                    "public String value() {" +
                    "      return \"barbar\" + flag;" +
                    "  }" +
                    "}"),
            new ExpectOutput(new String[]{"barbar100"}));
}
