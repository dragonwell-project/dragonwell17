/**
 * @test
 * @summary all classes in a directory .no class changed.
 * @library /test/lib /com/alibaba/lib
 * @requires os.arch=="amd64"
 * @run main/othervm TestSameMixClassesPlainJar
 */
public class TestSameMixClassesPlainJar implements PairProjectProvider {

    public static void main(String[] args) throws Exception {
        new StaticDiffTestRunner().runTest(new TestSameMixClassesPlainJar());
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
                    new String[]{"com.x.Add", "com.y.Sub", "com.m.Multiply", "com.u.Divide", "com.p.Power"}, null,
                    new JavaSource.MethodDesc[]{
                            new JavaSource.MethodDesc("main",
                                    "public static void main(String[] args) {" +
                                            "Add add = new Add();" +
                                            "System.out.println(add.add(10,20));" +
                                            "Sub sub = new Sub();" +
                                            "System.out.println(sub.sub(100,10));" +
                                            "Multiply m = new Multiply();" +
                                            "System.out.println(m.multiply(4,12));" +
                                            "Power p = new Power();" +
                                            "System.out.println(p.power(2,10));" +
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

    private JavaSource[] bazBaseSource = new JavaSource[]{
            new JavaSource(
                    "com.p.Power", "public class Power",
                    null, null,
                    new JavaSource.MethodDesc[]{
                            new JavaSource.MethodDesc("power",
                                    "public int power(int a,int b) { int c=1; for(int i = 0 ; i < b;i++) c*=a; return c; } ")
                    }
            )
    };


    private Project baseProject = new Project(new RunMainClassConf("com.z.Main"),
            new Artifact[]{
                    Artifact.createPlainJar("foo", "foo-lib", "add-sub.1.0.jar", new String[]{"myclasses", "otherclasses"}, fooBaseSource),
                    Artifact.createClasses("myclasses", "myclasses", null, barBaseSource),
                    Artifact.createClasses("otherclasses", "otherclasses", null, bazBaseSource)
            },
            new ExpectOutput(new String[]{"30", "90", "48", "1024"},
                    new String[]{
                            StaticDiffTestConstant.INVALID_CLASS_KEYWORD_IN_LOG
                    }));
}
