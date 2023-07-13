/**
 * @test
 * @summary Change a jar's name.But no class changed.All class should not invalid
 * @library /test/lib /com/alibaba/lib
 * @requires os.arch=="amd64"
 * @run main/othervm TestChangePlainJarVersion static
 * @run main/othervm TestChangePlainJarVersion dynamic
 */
public class TestChangePlainJarVersion extends PairProjectProvider {

    public TestChangePlainJarVersion(String arg) {
        super(arg);
    }

    public static void main(String[] args) throws Exception {
        new DiffTestRunner().runTest(new TestChangePlainJarVersion(args[0]));
    }

    @Override
    public Project versionA() {
        return baseProject;
    }

    @Override
    public Project versionB() {
        return currProject;
    }

    private Project baseProject = new Project(new RunWithURLClassLoaderConf(Constant.TEST_LOGGER_MAIN.getClassName()),
            new Artifact[]{
                    Artifact.createPlainJar("logger", "lib", "logger-1.0.1.jar", null, new JavaSource[]{Constant.LOGGER,
                            Constant.MY_LOG4J_LOGGER, Constant.MY_LOGBACK_LOGGER, Constant.LOGGER_FACTORY
                    }, new ArtifactOption[]{ArtifactOption.LOAD_BY_URLCLASSLOADER}),
                    Artifact.createPlainJar("entry", "lib", "entry.jar", new String[]{"logger"},
                            new JavaSource[]{Constant.TEST_LOGGER_MAIN}, new ArtifactOption[]{ArtifactOption.LOAD_BY_URLCLASSLOADER})
            },
            new ExpectOutput(new String[]{"[log4j]hello world",
                    Constant.EAGER_LOAD_SUCCESS_KEYWORD_IN_LOG + Constant.LOGGER.internalName(),
                    Constant.EAGER_LOAD_SUCCESS_KEYWORD_IN_LOG + Constant.MY_LOG4J_LOGGER.internalName(),
                    Constant.EAGER_LOAD_SUCCESS_KEYWORD_IN_LOG + Constant.LOGGER_FACTORY.internalName(),
            }, new String[]{
                    Constant.EAGER_LOAD_SUCCESS_KEYWORD_IN_LOG + Constant.MY_LOGBACK_LOGGER.internalName()
            }));

    private Project currProject = new Project(new RunWithURLClassLoaderConf(Constant.TEST_LOGGER_MAIN.getClassName()),
            new Artifact[]{
                    Artifact.createPlainJar("logger", "lib", "logger-2.0.1.jar", null, new JavaSource[]{Constant.LOGGER,
                            Constant.MY_LOG4J_LOGGER, Constant.MY_LOGBACK_LOGGER, Constant.LOGGER_FACTORY
                    }, new ArtifactOption[]{ArtifactOption.LOAD_BY_URLCLASSLOADER}),
                    Artifact.createPlainJar("entry", "lib", "entry.jar", new String[]{"logger"},
                            new JavaSource[]{Constant.TEST_LOGGER_MAIN}, new ArtifactOption[]{ArtifactOption.LOAD_BY_URLCLASSLOADER})
            },
            new ExpectOutput(new String[]{"[log4j]hello world",

                    Constant.EAGER_LOAD_SUCCESS_KEYWORD_IN_LOG + Constant.LOGGER.internalName(),
                    Constant.EAGER_LOAD_SUCCESS_KEYWORD_IN_LOG + Constant.MY_LOG4J_LOGGER.internalName(),
                    Constant.EAGER_LOAD_SUCCESS_KEYWORD_IN_LOG + Constant.LOGGER_FACTORY.internalName(),
            }, new String[]{
                    Constant.EAGER_LOAD_SUCCESS_KEYWORD_IN_LOG + Constant.MY_LOGBACK_LOGGER.internalName()
            }));
}
