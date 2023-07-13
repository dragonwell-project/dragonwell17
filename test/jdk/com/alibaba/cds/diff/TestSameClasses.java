/**
 * @test
 * @summary all classes in a directory .no class changed.
 * @library /test/lib /com/alibaba/lib
 * @requires os.arch=="amd64"
 * @run main/othervm TestSameClasses static
 * @run main/othervm TestSameClasses dynamic
 */
public class TestSameClasses extends PairProjectProvider {

    public TestSameClasses(String arg) {
        super(arg);
    }

    public static void main(String[] args) throws Exception {
        new DiffTestRunner().runTest(new TestSameClasses(args[0]));
    }

    @Override
    public Project versionA() {
        return baseProject;
    }

    @Override
    public Project versionB() {
        return baseProject;
    }

    private JavaSource[] baseSource = new JavaSource[]{
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
                    new String[]{"com.x.Add", "com.y.Sub"}, null,
                    new JavaSource.MethodDesc[]{
                            new JavaSource.MethodDesc("main",
                                    "public static void main(String[] args) {" +
                                            "Add add = new Add();" +
                                            "System.out.println(add.add(10,20));" +
                                            "Sub sub = new Sub();" +
                                            "System.out.println(sub.sub(100,10));" +
                                            "}"
                            )
                    }
            )
    };


    private Project baseProject = new Project(new RunMainClassConf("com.z.Main"),
            new Artifact[]{
                    Artifact.createClasses("foo", "myclasses", null, baseSource)
            },
            new ExpectOutput(new String[]{"30", "90"},
                    new String[]{
                            Constant.INVALID_CLASS_KEYWORD_IN_LOG
                    })
    );

}
