/**
 * @test
 * @summary The root class changed
 * @library /test/lib /com/alibaba/lib
 * @requires os.arch=="amd64"
 * @run main/othervm TestMiddleClassChanged static
 * @run main/othervm TestMiddleClassChanged dynamic
 */
public class TestMiddleClassChanged extends PairProjectProvider {

    public TestMiddleClassChanged(String arg) {
        super(arg);
    }

    public static void main(String[] args) throws Exception {
        new DiffTestRunner().runTest(new TestMiddleClassChanged(args[0]));
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
                    "com.m.Main", "public class Main",
                    new String[]{"com.a.*"}, null,
                    new JavaSource.MethodDesc[]{
                            new JavaSource.MethodDesc("main",
                                    "public static void main(String[] args) { A a = new GrandSonB();}"
                            )
                    }
            )
    };

    private JavaSource[] baseClassSource = new JavaSource[]{
            new JavaSource(
                    "com.a.A", "package com.a;\npublic class A {" +
                    "public A() {" +
                    "    System.out.println(\"Call A.<init>\");" +
                    "  }" +
                    "}"
            )
    };
    private JavaSource[] subClassSource = new JavaSource[]{
            new JavaSource("com.a.SonA", "package com.a;\npublic class SonA extends A {}"),
            new JavaSource("com.a.SonB", "public class SonB extends A", null, new JavaSource.FieldDesc[]{
                    new JavaSource.FieldDesc("f1", "private String f1"),
                    new JavaSource.FieldDesc("f2", "private int f2 = 0")
            }, new JavaSource.MethodDesc[]{
                    new JavaSource.MethodDesc("eat", "public void eat() {}")
            }),
            new JavaSource("com.a.GrandSonA", "package com.a;\npublic class GrandSonA extends SonA{}"),
            new JavaSource("com.a.GrandSonB", "package com.a;\npublic class GrandSonB extends SonB{}")
    };

    private Project baseProject = new Project(new RunMainClassConf("com.m.Main"),
            new Artifact[]{
                    Artifact.createClasses("base", "classes", null, baseClassSource),
                    Artifact.createPlainJar("sub", "lib", "sub.jar", new String[]{"base"}, subClassSource),
                    Artifact.createPlainJar("main", "lib", "main.jar", new String[]{"base", "sub"}, mainSource)
            },
            new ExpectOutput(new String[]{"Call A.<init>"}));


    private Project currProject = ProjectUtil.updateSource(baseProject, "sub", JavaSourceUtil.updateMethod(subClassSource, "com.a.SonB", "eat",
                    "public void eat() {f2++;}", null),
            new ExpectOutput(new String[]{"Call A.<init>",
                    "com/a/SonB" + Constant.INVALID_CLASS_KEYWORD_IN_LOG,
                    isStaticDiff ? "com/a/GrandSonB" + Constant.INVALID_CLASS_KEYWORD_IN_LOG :
                                   Constant.DYNAMIC_SUPER_VERIFICATION_FAILED + "com/a/GrandSonB",
            }, new String[]{
                    "com/a/A" + Constant.INVALID_CLASS_KEYWORD_IN_LOG,
            }));
}
