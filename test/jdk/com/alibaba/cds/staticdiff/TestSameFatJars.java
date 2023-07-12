/**
 * @test
 * @summary all classes in a fat jar.no class changed.
 * @library /test/lib /com/alibaba/lib
 * @requires os.arch=="amd64"
 * @run main/othervm TestSameFatJars
 */
public class TestSameFatJars implements PairProjectProvider {

    public static void main(String[] args) throws Exception {
        new StaticDiffTestRunner().runTest(new TestSameFatJars());
    }

    @Override
    public Project versionA() {
        return baseProject;
    }

    @Override
    public Project versionB() {
        return baseProject;
    }

    private JavaSource[] aSources = new JavaSource[]{
            new JavaSource("com.a.A1", "public class A1", null, null, new JavaSource.MethodDesc[]{
                    new JavaSource.MethodDesc("name", "public String name() { return \"A1\"; }")
            }),
            new JavaSource("com.a.A2", "public class A2", null, null, new JavaSource.MethodDesc[]{
                    new JavaSource.MethodDesc("name", "public String name() { return \"A2\"; }")
            })
    };

    private JavaSource[] bSources = new JavaSource[]{
            new JavaSource("com.b.B1", "public class B1", null, null, new JavaSource.MethodDesc[]{
                    new JavaSource.MethodDesc("name", "public String name() { return \"B1\"; }")
            }),
            new JavaSource("com.b.B2", "public class B2", null, null, new JavaSource.MethodDesc[]{
                    new JavaSource.MethodDesc("name", "public String name() { return \"B2\"; }")
            })
    };
    private JavaSource[] cSources = new JavaSource[]{
            new JavaSource("com.c.C1", "public class C1", null, null, new JavaSource.MethodDesc[]{
                    new JavaSource.MethodDesc("name", "public String name() { return \"C1\"; }")
            }),
            new JavaSource("com.c.C2", "public class C2", null, null, new JavaSource.MethodDesc[]{
                    new JavaSource.MethodDesc("name", "public String name() { return \"C2\"; }")
            }),
            new JavaSource("com.m.M1", "public class M1", null, null, new JavaSource.MethodDesc[]{
                    new JavaSource.MethodDesc("name", "public String name() { return \"M1\"; }")
            }),
            new JavaSource("com.m.M2", "public class M2", null, null, new JavaSource.MethodDesc[]{
                    new JavaSource.MethodDesc("name", "public String name() { return \"M2\"; }")
            }),
            new JavaSource("com.m.Main", "public class Main", null, null, new JavaSource.MethodDesc[]{
                    new JavaSource.MethodDesc("main", "    public static void main(String[] args) {\n" +
                            "        M1 m1 = new M1();\n" +
                            "        System.out.println(m1.name());\n" +
                            "        M2 m2 = new M2();\n" +
                            "        System.out.println(m2.name());\n" +
                            "    }")
            })
    };

    private Project baseProject = new Project(new RunFatJarConf("com.m.Main"),
            new Artifact[]{
                    Artifact.createFatJar("all-in-one", "fat-lib", "m-fat.jar",
                            new Artifact[]{
                                    Artifact.createPlainJar("a", "lib", "a.jar", null, aSources),
                                    Artifact.createPlainJar("b", "lib", "b.jar", null, bSources),
                                    Artifact.createClasses("c", "myclasses", null, cSources)
                            })
            },
            new ExpectOutput(new String[]{"M1", "M2"},
                    new String[]{
                            StaticDiffTestConstant.INVALID_CLASS_KEYWORD_IN_LOG
                    }));
}
