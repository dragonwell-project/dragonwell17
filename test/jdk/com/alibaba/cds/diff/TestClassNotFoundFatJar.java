/**
 * @test
 * @summary A class is not found when tracing, but it exists when replaying.
 * @library /test/lib /com/alibaba/lib
 * @requires os.arch=="amd64"
 * @run main/othervm TestClassNotFoundFatJar static
 * @run main/othervm TestClassNotFoundFatJar dynamic
 */
public class TestClassNotFoundFatJar extends PairProjectProvider {

    public TestClassNotFoundFatJar(String arg) {
        super(arg);
    }

    public static void main(String[] args) throws Exception {
        new DiffTestRunner().runTest(new TestClassNotFoundFatJar(args[0]));
    }

    @Override
    public Project versionA() {
        return baseProject;
    }

    @Override
    public Project versionB() {
        return currProject;
    }

    private Project baseProject = new Project(new RunFatJarConf(Constant.DISCOUNT_MAIN.getClassName()),
            new Artifact[]{
                    Artifact.createFatJar("fat-jar", "fat-lib", "all-discount.jar", new Artifact[]{
                            Artifact.createPlainJar("discount", "lib", "discount.jar", null,
                                    new JavaSource[]{Constant.DISCOUNT, Constant.DISCOUNT_MIN}, new ArtifactOption[]{ArtifactOption.LOAD_BY_URLCLASSLOADER}),
                            Artifact.createPlainJar("main", "lib", "main.jar", new String[]{"discount"},
                                    new JavaSource[]{Constant.DISCOUNT_MAIN}, new ArtifactOption[]{ArtifactOption.LOAD_BY_URLCLASSLOADER})
                    })
            },
            new ExpectOutput(new String[]{"You need pay 80",
                    Constant.EAGER_LOAD_SUCCESS_KEYWORD_IN_LOG + Constant.DISCOUNT.internalName(),
                    Constant.EAGER_LOAD_SUCCESS_KEYWORD_IN_LOG + Constant.DISCOUNT_MIN.internalName(),
                    Constant.EAGER_LOAD_SUCCESS_KEYWORD_IN_LOG + Constant.DISCOUNT_MAIN.internalName()
            }, null));

    private Project currProject = new Project(new RunFatJarConf(Constant.DISCOUNT_MAIN.getClassName()),
            new Artifact[]{
                    Artifact.createFatJar("fat-jar", "fat-lib", "all-discount.jar", new Artifact[]{
                            Artifact.createPlainJar("discount", "lib", "discount.jar", null,
                                    new JavaSource[]{Constant.DISCOUNT, Constant.DISCOUNT_MIN, Constant.DISCOUNT_MAX}, new ArtifactOption[]{ArtifactOption.LOAD_BY_URLCLASSLOADER}),
                            Artifact.createPlainJar("main", "lib", "main.jar", new String[]{"discount"},
                                    new JavaSource[]{Constant.DISCOUNT_MAIN}, new ArtifactOption[]{ArtifactOption.LOAD_BY_URLCLASSLOADER})
                    })
            },
            new ExpectOutput(new String[]{"You need pay 50",
                    Constant.EAGER_LOAD_SUCCESS_KEYWORD_IN_LOG + Constant.DISCOUNT.internalName(),
                    Constant.EAGER_LOAD_SUCCESS_KEYWORD_IN_LOG + Constant.DISCOUNT_MAIN.internalName()
            }, null));
}
