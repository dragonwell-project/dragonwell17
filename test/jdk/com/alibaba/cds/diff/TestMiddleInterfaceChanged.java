/**
 * @test
 * @summary A class nor leaf and nor root class changed.
 * @library /test/lib /com/alibaba/lib
 * @requires os.arch=="amd64"
 * @run main/othervm TestMiddleInterfaceChanged static
 * @run main/othervm TestMiddleInterfaceChanged dynamic
 */
public class TestMiddleInterfaceChanged extends PairProjectProvider {

    public TestMiddleInterfaceChanged(String arg) {
        super(arg);
    }

    public static void main(String[] args) throws Exception {
        new DiffTestRunner().runTest(new TestMiddleInterfaceChanged(args[0]));
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
            new JavaSource("com.intf.IDecorate", "public interface IDecorate extends IPrefix,ISuffix", new String[0], null, new JavaSource.MethodDesc[]{
                    new JavaSource.MethodDesc("getDecorateStr", "default String getDecorateStr() { return \">>>>>>>>>>\";}")
            })
    };

    private JavaSource[] impl1Source = new JavaSource[]{
            new JavaSource("com.impl1.Style1", "public class Style1 implements IDecorate", new String[]{"com.intf.IDecorate"}, null, new JavaSource.MethodDesc[]{
                    new JavaSource.MethodDesc("prefix", "public String prefix() { return getDecorateStr()+\"!\"; }"),
                    new JavaSource.MethodDesc("suffix", "public String suffix() { return flag() +\"@\"; }")
            }),
            new JavaSource("com.impl1.Style2", "public class Style2 implements IDecorate", new String[]{"com.intf.IDecorate"}, null, new JavaSource.MethodDesc[]{
                    new JavaSource.MethodDesc("prefix", "public String prefix() { return getDecorateStr()+\"###\"; }"),
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
                    new JavaSource.MethodDesc("getDecorate1", "static IDecorate getDecorate1() { return new Style1(); }"),
                    new JavaSource.MethodDesc("getDecorate2", "static IDecorate getDecorate2() { return new Style2(); }"),
                    new JavaSource.MethodDesc("getSuffix", "static ISuffix getSuffix() { return new Style3(); }"),
                    new JavaSource.MethodDesc("main", "public static void main(String[] args) {" +
                            "IDecorate d1 = getDecorate1();" +
                            "IDecorate d2 = getDecorate2();" +
                            "System.out.println(d1.prefix()+\"foo\"+d1.suffix());" +
                            "System.out.println(d2.prefix()+\"foo\"+d2.suffix());" +
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
            new ExpectOutput(new String[]{">>>>>>>>>>!foo^@", ">>>>>>>>>>###foo^$$$", "barqqq"}));


    private Project currProject = ProjectUtil.updateSource(baseProject, "interface",
            JavaSourceUtil.updateMethod(interfaceSource, "com.intf.IDecorate", "getDecorateStr",
                    "default String getDecorateStr() { return \"<<<<<<<<<<\";}"
                    , null),
            new ExpectOutput(new String[]{"<<<<<<<<<<!foo^@", "<<<<<<<<<<###foo^$$$", "barqqq",
                            "com/intf/IDecorate" + Constant.INVALID_CLASS_KEYWORD_IN_LOG,
                            isStaticDiff ? "com/impl1/Style1" + Constant.INVALID_CLASS_KEYWORD_IN_LOG :
                                           Constant.DYNAMIC_INTERFACE_VERIFICATION_FAILED + "com/impl1/Style1",
                            isStaticDiff ? "com/impl1/Style2" + Constant.INVALID_CLASS_KEYWORD_IN_LOG :
                                            Constant.DYNAMIC_INTERFACE_VERIFICATION_FAILED + "com/impl1/Style2",
                    },
                    new String[]{
                            "com/impl2/Style3" + Constant.INVALID_CLASS_KEYWORD_IN_LOG,
                            "com/impl2/Style4" + Constant.INVALID_CLASS_KEYWORD_IN_LOG,
                            "com/intf/IPrefix" + Constant.INVALID_CLASS_KEYWORD_IN_LOG,
                            "com/intf/ISuffix" + Constant.INVALID_CLASS_KEYWORD_IN_LOG
                    }));
}
