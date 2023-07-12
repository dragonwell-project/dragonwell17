/**
 * @test
 * @summary Test when class hierarchy change,related classes should invalid
 * @library /test/lib /com/alibaba/lib
 * @requires os.arch=="amd64"
 * @run main/othervm TestClassHierarchyChanged
 */
public class TestClassHierarchyChanged implements PairProjectProvider {

    public static void main(String[] args) throws Exception {
        new StaticDiffTestRunner().runTest(new TestClassHierarchyChanged());
    }

    @Override
    public Project versionA() {
        JavaSource[] baseSource = new JavaSource[]{
                new JavaSource("com.x.A", "package com.x;class A {}"),
                new JavaSource("com.x.SonA", "package com.x;class SonA extends A {}"),
                new JavaSource("com.x.SonB", "package com.x;class SonB extends A {}"),
                new JavaSource("com.x.GrandSonA", "package com.x;class GrandSonA extends SonA{}"),
                new JavaSource("com.x.GrandSonB", "package com.x;class GrandSonB extends SonB {}"),
                new JavaSource("com.x.Main", "package com.x;public class Main {\n" +
                        "public static void main(String[] args) {\n" +
                        "SonA a = new GrandSonA();" +
                        "SonB b = new GrandSonB();" +
                        "}\n" +
                        "}\n"
                )
        };
        return new Project(new RunMainClassConf("com.x.Main"),
                new Artifact[]{
                        Artifact.createClasses("classes", "classes", null, baseSource)
                }, new ExpectOutput(new String[]{})
        );
    }

    @Override
    public Project versionB() {
        JavaSource[] baseSource = new JavaSource[]{
                new JavaSource("com.x.A", "package com.x;class A {}"),
                new JavaSource("com.x.NewA", "package com.x;class NewA{}"),
                new JavaSource("com.x.SonA", "package com.x;class SonA extends NewA {}"),
                new JavaSource("com.x.SonB", "package com.x;class SonB extends A {}"),
                new JavaSource("com.x.GrandSonA", "package com.x;class GrandSonA extends SonA{}"),
                new JavaSource("com.x.GrandSonB", "package com.x;class GrandSonB extends SonB {}"),
                new JavaSource("com.x.Main", "package com.x;public class Main {\n" +
                        "public static void main(String[] args) {\n" +
                        "SonA a = new GrandSonA();" +
                        "SonB b = new GrandSonB();" +
                        "}\n" +
                        "}\n"
                )
        };
        return new Project(new RunMainClassConf("com.x.Main"),
                new Artifact[]{
                        Artifact.createClasses("classes", "classes", null, baseSource)
                }, new ExpectOutput(new String[]{
                "com/x/SonA" + StaticDiffTestConstant.INVALID_CLASS_KEYWORD_IN_LOG,
                "com/x/GrandSonA" + StaticDiffTestConstant.INVALID_CLASS_KEYWORD_IN_LOG
        }, new String[]{
                "com/x/A" + StaticDiffTestConstant.INVALID_CLASS_KEYWORD_IN_LOG,
                "com/x/SonB" + StaticDiffTestConstant.INVALID_CLASS_KEYWORD_IN_LOG,
                "com/x/GrandSonB" + StaticDiffTestConstant.INVALID_CLASS_KEYWORD_IN_LOG
        }));
    }
}
