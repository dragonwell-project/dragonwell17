/**
 * @test
 * @summary The root class changed
 * @library /test/lib /com/alibaba/lib
 * @requires os.arch=="amd64"
 * @run main/othervm TestRootClassChanged
 */
public class TestRootClassChanged implements PairProjectProvider {

    public static void main(String[] args) throws Exception {
        new StaticDiffTestRunner().runTest(new TestRootClassChanged());
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
                                    "public static void main(String[] args) { A a = new GrandSonA();}"
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
            new JavaSource("com.a.SonB", "package com.a;\npublic class SonB extends A {}"),
            new JavaSource("com.a.GrandSonA", "package com.a;\npublic class GrandSonA extends SonA{}"),
            new JavaSource("com.a.GrandSonB", "package com.a;\npublic class GrandSonB extends SonB{}")
    };

    private Project baseProject = new Project(new RunWithURLClassLoaderConf("com.m.Main"),
            new Artifact[]{
                    Artifact.createPlainJar("base", "classes", "main.jar", null, baseClassSource, new ArtifactOption[]{ArtifactOption.LOAD_BY_URLCLASSLOADER}),
                    Artifact.createPlainJar("sub", "lib", "sub.jar", new String[]{"base"}, subClassSource, new ArtifactOption[]{ArtifactOption.LOAD_BY_URLCLASSLOADER}),
                    Artifact.createPlainJar("main", "lib", "main.jar", new String[]{"base", "sub"}, mainSource, new ArtifactOption[]{ArtifactOption.LOAD_BY_URLCLASSLOADER})
            },
            new ExpectOutput(new String[]{"Call A.<init>"}));


    private Project currProject = ProjectUtil.updateSource(baseProject, "base", JavaSourceUtil.updateClass(baseClassSource, "com.a.A", "package com.a;\npublic class A {" +
                    "public A() {" +
                    "    System.out.println(\"Enter A.<init>\");" +
                    "  }" +
                    "}"),
            new ExpectOutput(new String[]{"Enter A.<init>",
                    "com/a/A" + StaticDiffTestConstant.INVALID_CLASS_KEYWORD_IN_LOG,
                    "com/a/SonA" + StaticDiffTestConstant.INVALID_CLASS_KEYWORD_IN_LOG,
                    "com/a/GrandSonA" + StaticDiffTestConstant.INVALID_CLASS_KEYWORD_IN_LOG,
            }));
}
