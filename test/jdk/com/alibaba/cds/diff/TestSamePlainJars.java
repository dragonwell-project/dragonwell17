/**
 * @test
 * @summary all classes in plain jars. no class changed.
 * @library /test/lib /com/alibaba/lib
 * @requires os.arch=="amd64"
 * @run main/othervm TestSamePlainJars static
 * @run main/othervm TestSamePlainJars dynamic
 */
public class TestSamePlainJars extends PairProjectProvider {

    public TestSamePlainJars(String arg) {
        super(arg);
    }

    public static void main(String[] args) throws Exception {
        new DiffTestRunner().runTest(new TestSamePlainJars(args[0]));
    }

    @Override
    public Project versionA() {
        return baseProject;
    }

    @Override
    public Project versionB() {
        return baseProject;
    }

    private JavaSource[] fooBaseSource = new JavaSource[]{
            new JavaSource(
                    "com.x.Add", "public class Add",
                    null, null,
                    new JavaSource.MethodDesc[]{
                            new JavaSource.MethodDesc("add",
                                    "public int add(int a,int b) { return a+b; } ")
                    }
            ),
            new JavaSource(
                    "com.y.Sub", "public class Sub",
                    null, null,
                    new JavaSource.MethodDesc[]{
                            new JavaSource.MethodDesc("sub",
                                    "public int sub(int a,int b) {return a-b;}")
                    }
            ),
            new JavaSource(
                    "com.z.Main", "public class Main",
                    new String[]{"com.x.Add", "com.y.Sub", "com.m.Multiply", "com.u.Divide"}, null,
                    new JavaSource.MethodDesc[]{
                            new JavaSource.MethodDesc("main",
                                    "public static void main(String[] args) {" +
                                            "Add add = new Add();" +
                                            "System.out.println(add.add(10,20));" +
                                            "Sub sub = new Sub();" +
                                            "System.out.println(sub.sub(100,10));" +
                                            "Multiply m = new Multiply();" +
                                            "System.out.println(m.multiply(4,12));" +
                                            "}"
                            )
                    }
            )
    };

    private JavaSource[] barBaseSource = new JavaSource[]{
            new JavaSource(
                    "com.m.Multiply", "public class Multiply",
                    null, null,
                    new JavaSource.MethodDesc[]{
                            new JavaSource.MethodDesc("multiply",
                                    "public int multiply(int a,int b) { return a*b; } ")
                    }
            ),
            new JavaSource(
                    "com.u.Divide", "public class Divide",
                    null, null,
                    new JavaSource.MethodDesc[]{
                            new JavaSource.MethodDesc("divide",
                                    "public int divide(int a,int b) {return a/b;}")
                    }
            )
    };

    private Project baseProject = new Project(new RunMainClassConf("com.z.Main"),
            new Artifact[]{
                    Artifact.createPlainJar("foo", "foo-lib", "add-sub.1.0.jar", new String[]{"bar"}, fooBaseSource),
                    Artifact.createPlainJar("bar", "bar-lib", "mul-div-1.0.jar", null, barBaseSource)
            },
            new ExpectOutput(new String[]{"30", "90", "48"},
                    new String[]{
                            Constant.INVALID_CLASS_KEYWORD_IN_LOG
                    }));
}
