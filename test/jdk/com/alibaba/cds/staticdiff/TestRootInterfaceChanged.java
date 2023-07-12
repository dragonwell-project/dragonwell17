/**
 * @test
 * @summary The root interface changed of a class hierarchy
 * @library /test/lib /com/alibaba/lib
 * @requires os.arch=="amd64"
 * @run main/othervm TestRootInterfaceChanged
 */
public class TestRootInterfaceChanged implements PairProjectProvider {

    public static void main(String[] args) throws Exception {
        new StaticDiffTestRunner().runTest(new TestRootInterfaceChanged());
    }

    @Override
    public Project versionA() {
        return baseProject;
    }

    @Override
    public Project versionB() {
        return currProject;
    }

    private JavaSource[] interfaceSource = new JavaSource[]{
            new JavaSource("com.intf.IPrefix", "public interface IPrefix", null, null, new JavaSource.MethodDesc[]{
                    new JavaSource.MethodDesc("prefix", "String prefix();"),
                    new JavaSource.MethodDesc("flag", "default String flag() { return \"^\"; }")
            }),
            new JavaSource("com.intf.ISuffix", "public interface ISuffix", null, null, new JavaSource.MethodDesc[]{
                    new JavaSource.MethodDesc("suffix", "public String suffix(); ")
            }),
            new JavaSource("com.intf.IDecorate", "public interface IDecorate extends IPrefix,ISuffix", new String[0], null, null)
    };

    private JavaSource[] impl1Source = new JavaSource[]{
            new JavaSource("com.impl1.Style1", "public class Style1 implements IDecorate", new String[]{"com.intf.IDecorate"}, null, new JavaSource.MethodDesc[]{
                    new JavaSource.MethodDesc("prefix", "public String prefix() { return flag()+\"!\"; }"),
                    new JavaSource.MethodDesc("suffix", "public String suffix() { return flag() +\"@\"; }")
            }),
            new JavaSource("com.impl1.Style2", "public class Style2 implements IDecorate", new String[]{"com.intf.IDecorate"}, null, new JavaSource.MethodDesc[]{
                    new JavaSource.MethodDesc("prefix", "public String prefix() { return flag()+\"###\"; }"),
                    new JavaSource.MethodDesc("suffix", "public String suffix() { return flag()+\"$$$\"; }")
            })
    };

    private JavaSource[] impl2Source = new JavaSource[]{
            new JavaSource("com.impl2.Style3", "public class Style3 implements ISuffix", new String[]{"com.intf.ISuffix"}, null, new JavaSource.MethodDesc[]{
                    new JavaSource.MethodDesc("suffix", "public String suffix() { return \"qqq\"; }")
            }),
            new JavaSource("com.impl2.Style4", "public class Style4 implements ISuffix", new String[]{"com.intf.ISuffix"}, null, new JavaSource.MethodDesc[]{
                    new JavaSource.MethodDesc("suffix", "public String suffix() { return \"www\"; }")
            })
    };

    private JavaSource[] mainSource = new JavaSource[]{
            new JavaSource("com.m.Main", "public class Main", new String[]{"com.impl2.*", "com.impl1.*", "com.intf.*"}, null, new JavaSource.MethodDesc[]{
                    new JavaSource.MethodDesc("getDecorate", "static IDecorate getDecorate() { return new Style2(); }"),
                    new JavaSource.MethodDesc("getSuffix", "static ISuffix getSuffix() { return new Style3(); }"),
                    new JavaSource.MethodDesc("main", "public static void main(String[] args) {" +
                            "IDecorate d = getDecorate();" +
                            "System.out.println(d.prefix()+\"foo\"+d.suffix());" +
                            "ISuffix s = getSuffix();" +
                            "System.out.println(\"bar\"+s.suffix());" +
                            "}")
            }),
    };

    private Project baseProject = new Project(new RunFatJarConf("com.m.Main"),
            new Artifact[]{
                    Artifact.createFatJar("all-in-one", "fat-lib", "m-fat.jar",
                            new Artifact[]{
                                    Artifact.createPlainJar("interface", "lib", "interface.jar", null, interfaceSource),
                                    Artifact.createPlainJar("impl1", "lib", "impl1.jar", new String[]{"interface"}, impl1Source),
                                    Artifact.createPlainJar("impl2", "lib", "impl2.jar", new String[]{"interface"}, impl2Source),
                                    Artifact.createPlainJar("main", "lib", "main.jar", new String[]{"interface", "impl1", "impl2"}, mainSource),
                            })
            },
            new ExpectOutput(new String[]{"^###foo^$$$", "barqqq"}));


    private Project currProject = ProjectUtil.updateSource(baseProject, "interface",
            JavaSourceUtil.updateMethod(interfaceSource, "com.intf.IPrefix", "flag",
                    "default String flag() { return \"?\"; }"
                    , null),
            new ExpectOutput(new String[]{"?###foo?$$$", "barqqq",
                    "com/intf/IPrefix" + StaticDiffTestConstant.INVALID_CLASS_KEYWORD_IN_LOG,
                    "com/intf/IDecorate" + StaticDiffTestConstant.INVALID_CLASS_KEYWORD_IN_LOG,
                    "com/impl1/Style2" + StaticDiffTestConstant.INVALID_CLASS_KEYWORD_IN_LOG}));
}
