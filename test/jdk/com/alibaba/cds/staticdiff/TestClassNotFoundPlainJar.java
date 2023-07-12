/**
 * @test
 * @summary A class is not found when tracing, but it exists when replaying.
 * @library /test/lib /com/alibaba/lib
 * @requires os.arch=="amd64"
 * @run main/othervm TestClassNotFoundPlainJar
 */
public class TestClassNotFoundPlainJar implements PairProjectProvider {
    public static void main(String[] args) throws Exception {
        new StaticDiffTestRunner().runTest(new TestClassNotFoundPlainJar());
    }

    @Override
    public Project versionA() {
        return baseProject;
    }

    @Override
    public Project versionB() {
        return currProject;
    }

    private Project baseProject = new Project(new RunWithURLClassLoaderConf(StaticDiffTestConstant.DISCOUNT_MAIN.getClassName()),
            new Artifact[]{
                    Artifact.createPlainJar("discount", "lib", "discount.jar", null,
                            new JavaSource[]{StaticDiffTestConstant.DISCOUNT, StaticDiffTestConstant.DISCOUNT_MIN}, new ArtifactOption[]{ArtifactOption.LOAD_BY_URLCLASSLOADER}),
                    Artifact.createPlainJar("main", "lib", "main.jar", new String[]{"discount"},
                            new JavaSource[]{StaticDiffTestConstant.DISCOUNT_MAIN}, new ArtifactOption[]{ArtifactOption.LOAD_BY_URLCLASSLOADER}),
            },
            new ExpectOutput(new String[]{"You need pay 80",
                    StaticDiffTestConstant.EAGER_LOAD_SUCCESS_KEYWORD_IN_LOG + StaticDiffTestConstant.DISCOUNT.internalName(),
                    StaticDiffTestConstant.EAGER_LOAD_SUCCESS_KEYWORD_IN_LOG + StaticDiffTestConstant.DISCOUNT_MIN.internalName(),
                    StaticDiffTestConstant.EAGER_LOAD_SUCCESS_KEYWORD_IN_LOG + StaticDiffTestConstant.DISCOUNT_MAIN.internalName()
            }, null));

    private Project currProject = new Project(new RunWithURLClassLoaderConf(StaticDiffTestConstant.DISCOUNT_MAIN.getClassName()),
            new Artifact[]{
                    Artifact.createPlainJar("discount", "lib", "discount.jar", null,
                            new JavaSource[]{StaticDiffTestConstant.DISCOUNT, StaticDiffTestConstant.DISCOUNT_MIN, StaticDiffTestConstant.DISCOUNT_MAX}, new ArtifactOption[]{ArtifactOption.LOAD_BY_URLCLASSLOADER}),
                    Artifact.createPlainJar("main", "lib", "main.jar", new String[]{"discount"},
                            new JavaSource[]{StaticDiffTestConstant.DISCOUNT_MAIN}, new ArtifactOption[]{ArtifactOption.LOAD_BY_URLCLASSLOADER}),
            },
            new ExpectOutput(new String[]{"You need pay 50",
                    StaticDiffTestConstant.EAGER_LOAD_SUCCESS_KEYWORD_IN_LOG + StaticDiffTestConstant.DISCOUNT.internalName(),
                    StaticDiffTestConstant.EAGER_LOAD_SUCCESS_KEYWORD_IN_LOG + StaticDiffTestConstant.DISCOUNT_MAIN.internalName()
            }, null));
}
