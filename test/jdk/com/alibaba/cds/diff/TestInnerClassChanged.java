/**
 * @test
 * @summary An inner class changed.
 * @library /test/lib /com/alibaba/lib
 * @requires os.arch=="amd64"
 * @run main/othervm TestInnerClassChanged static
 * @run main/othervm TestInnerClassChanged dynamic
 */
public class TestInnerClassChanged extends PairProjectProvider {

    public TestInnerClassChanged(String arg) {
        super(arg);
    }

    public static void main(String[] args) throws Exception {
        new DiffTestRunner().runTest(new TestInnerClassChanged(args[0]));
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
                                            "Outer o = new Outer();" +
                                            "Outer.Inner oi = o.new Inner();" +
                                            "System.out.println(oi.value());" +
                                            "}"
                            )
                    }
            )
    };

    private JavaSource[] outerInnerSource = new JavaSource[]{
            new JavaSource(
                    "Outer", "public class Outer {" +
                    "public class Inner {" +
                    "public String value() {" +
                    "      return \"inner string\";" +
                    "    }" +
                    "  }" +
                    "}"
            ),
    };

    private Project baseProject = new Project(new RunMainClassConf("Main"),
            new Artifact[]{
                    Artifact.createPlainJar("outerInner", "lib1", "outerInner.jar", null, outerInnerSource),
                    Artifact.createPlainJar("main", "lib2", "main.jar", new String[]{"outerInner"}, mainSource)
            },
            new ExpectOutput(new String[]{"inner string"}));


    private Project currProject = ProjectUtil.updateSource(baseProject, "outerInner", JavaSourceUtil.updateClass(outerInnerSource, "Outer", "public class Outer {" +
                    "public class Inner {" +
                    "public String value() {" +
                    "      return \"inner string version 2\";" +
                    "    }" +
                    "  }" +
                    "}"),
            new ExpectOutput(new String[]{"inner string version 2"}));
}
