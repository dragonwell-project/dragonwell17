/**
 * @test
 * @summary Change a jar in fat jar but no class changed. All class should not invalid
 * @library /test/lib /com/alibaba/lib
 * @requires os.arch=="amd64"
 * @run main/othervm TestChangeFatJarVersion
 */
public class TestChangeFatJarVersion implements PairProjectProvider {
    public static void main(String[] args) throws Exception {
        new StaticDiffTestRunner().runTest(new TestChangeFatJarVersion());
    }

    @Override
    public Project versionA() {
        return baseProject;
    }

    @Override
    public Project versionB() {
        return currProject;
    }

    private Project baseProject = new Project(new RunFatJarConf(StaticDiffTestConstant.TEST_LOGGER_MAIN.getClassName()),
            new Artifact[]{
                    Artifact.createFatJar("fat-jar", "fat-lib", "fat-lib-1.0.jar", new Artifact[]{
                            Artifact.createPlainJar("logger", "lib", "logger-v1.0.jar", null, new JavaSource[]{StaticDiffTestConstant.LOGGER,
                                    StaticDiffTestConstant.MY_LOG4J_LOGGER, StaticDiffTestConstant.MY_LOGBACK_LOGGER, StaticDiffTestConstant.LOGGER_FACTORY
                            }, new ArtifactOption[]{ArtifactOption.LOAD_BY_URLCLASSLOADER}),
                            Artifact.createPlainJar("entry", "lib", "entry.jar", new String[]{"logger"},
                                    new JavaSource[]{StaticDiffTestConstant.TEST_LOGGER_MAIN}, new ArtifactOption[]{ArtifactOption.LOAD_BY_URLCLASSLOADER})
                    })
            },
            new ExpectOutput(new String[]{"[log4j]hello world",
                    StaticDiffTestConstant.EAGER_LOAD_SUCCESS_KEYWORD_IN_LOG + StaticDiffTestConstant.LOGGER.internalName(),
                    StaticDiffTestConstant.EAGER_LOAD_SUCCESS_KEYWORD_IN_LOG + StaticDiffTestConstant.MY_LOG4J_LOGGER.internalName(),
                    StaticDiffTestConstant.EAGER_LOAD_SUCCESS_KEYWORD_IN_LOG + StaticDiffTestConstant.LOGGER_FACTORY.internalName(),
            }, new String[]{
                    StaticDiffTestConstant.EAGER_LOAD_SUCCESS_KEYWORD_IN_LOG + StaticDiffTestConstant.MY_LOGBACK_LOGGER.internalName()
            }));

    private Project currProject = new Project(new RunFatJarConf(StaticDiffTestConstant.TEST_LOGGER_MAIN.getClassName()),
            new Artifact[]{
                    Artifact.createFatJar("fat-jar", "fat-lib", "fat-lib-1.0.jar", new Artifact[]{
                            Artifact.createPlainJar("logger", "lib", "logger-v2.0.jar", null, new JavaSource[]{StaticDiffTestConstant.LOGGER,
                                    StaticDiffTestConstant.MY_LOG4J_LOGGER, StaticDiffTestConstant.MY_LOGBACK_LOGGER, StaticDiffTestConstant.LOGGER_FACTORY
                            }, new ArtifactOption[]{ArtifactOption.LOAD_BY_URLCLASSLOADER}),
                            Artifact.createPlainJar("entry", "lib", "entry.jar", new String[]{"logger"},
                                    new JavaSource[]{StaticDiffTestConstant.TEST_LOGGER_MAIN}, new ArtifactOption[]{ArtifactOption.LOAD_BY_URLCLASSLOADER})
                    })
            },
            new ExpectOutput(new String[]{"[log4j]hello world",
                    StaticDiffTestConstant.EAGER_LOAD_SUCCESS_KEYWORD_IN_LOG + StaticDiffTestConstant.LOGGER.internalName(),
                    StaticDiffTestConstant.EAGER_LOAD_SUCCESS_KEYWORD_IN_LOG + StaticDiffTestConstant.MY_LOG4J_LOGGER.internalName(),
                    StaticDiffTestConstant.EAGER_LOAD_SUCCESS_KEYWORD_IN_LOG + StaticDiffTestConstant.LOGGER_FACTORY.internalName(),
            }, new String[]{
                    StaticDiffTestConstant.EAGER_LOAD_SUCCESS_KEYWORD_IN_LOG + StaticDiffTestConstant.MY_LOGBACK_LOGGER.internalName()
            }));
}
