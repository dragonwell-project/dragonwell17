/**
 * @test
 * @summary Change a jar in fat jar but no class changed. All class should not invalid
 * @library /test/lib /com/alibaba/lib
 * @requires os.arch=="amd64"
 * @run main/othervm TestChangeFatJarVersion static
 */

// TODO: CDS dynamic diff doesn't support heuristic for Fatjar entry's version, like
//   fat-lib.jar!/logger-v1.0.jar!/...
//   ->
//   fat-lib.jar!/logger-v2.0.jar!/...
//   If that is supported, we can mark this ready for testing CDS dynamic diff.

public class TestChangeFatJarVersion extends PairProjectProvider {

    public TestChangeFatJarVersion(String arg) {
        super(arg);
    }

    public static void main(String[] args) throws Exception {
        new DiffTestRunner().runTest(new TestChangeFatJarVersion(args[0]));
    }

    @Override
    public Project versionA() {
        return baseProject;
    }

    @Override
    public Project versionB() {
        return currProject;
    }

    private Project baseProject = new Project(new RunFatJarConf(Constant.TEST_LOGGER_MAIN.getClassName()),
            new Artifact[]{
                    Artifact.createFatJar("fat-jar", "fat-lib", "fat-lib-1.0.jar", new Artifact[]{
                            Artifact.createPlainJar("logger", "lib", "logger-v1.0.jar", null, new JavaSource[]{Constant.LOGGER,
                                    Constant.MY_LOG4J_LOGGER, Constant.MY_LOGBACK_LOGGER, Constant.LOGGER_FACTORY
                            }, new ArtifactOption[]{ArtifactOption.LOAD_BY_URLCLASSLOADER}),
                            Artifact.createPlainJar("entry", "lib", "entry.jar", new String[]{"logger"},
                                    new JavaSource[]{Constant.TEST_LOGGER_MAIN}, new ArtifactOption[]{ArtifactOption.LOAD_BY_URLCLASSLOADER})
                    })
            },
            new ExpectOutput(new String[]{"[log4j]hello world",
                    Constant.EAGER_LOAD_SUCCESS_KEYWORD_IN_LOG + Constant.LOGGER.internalName(),
                    Constant.EAGER_LOAD_SUCCESS_KEYWORD_IN_LOG + Constant.MY_LOG4J_LOGGER.internalName(),
                    Constant.EAGER_LOAD_SUCCESS_KEYWORD_IN_LOG + Constant.LOGGER_FACTORY.internalName(),
            }, new String[]{
                    Constant.EAGER_LOAD_SUCCESS_KEYWORD_IN_LOG + Constant.MY_LOGBACK_LOGGER.internalName()
            }));

    private Project currProject = new Project(new RunFatJarConf(Constant.TEST_LOGGER_MAIN.getClassName()),
            new Artifact[]{
                    Artifact.createFatJar("fat-jar", "fat-lib", "fat-lib-1.0.jar", new Artifact[]{
                            Artifact.createPlainJar("logger", "lib", "logger-v2.0.jar", null, new JavaSource[]{Constant.LOGGER,
                                    Constant.MY_LOG4J_LOGGER, Constant.MY_LOGBACK_LOGGER, Constant.LOGGER_FACTORY
                            }, new ArtifactOption[]{ArtifactOption.LOAD_BY_URLCLASSLOADER}),
                            Artifact.createPlainJar("entry", "lib", "entry.jar", new String[]{"logger"},
                                    new JavaSource[]{Constant.TEST_LOGGER_MAIN}, new ArtifactOption[]{ArtifactOption.LOAD_BY_URLCLASSLOADER})
                    })
            },
            new ExpectOutput(new String[]{"[log4j]hello world",
                    Constant.EAGER_LOAD_SUCCESS_KEYWORD_IN_LOG + Constant.LOGGER.internalName(),
                    Constant.EAGER_LOAD_SUCCESS_KEYWORD_IN_LOG + Constant.MY_LOG4J_LOGGER.internalName(),
                    Constant.EAGER_LOAD_SUCCESS_KEYWORD_IN_LOG + Constant.LOGGER_FACTORY.internalName(),
            }, new String[]{
                    Constant.EAGER_LOAD_SUCCESS_KEYWORD_IN_LOG + Constant.MY_LOGBACK_LOGGER.internalName()
            }));
}
