/**
 * @test
 * @summary A jar in fat jar was removed
 * @library /test/lib /com/alibaba/lib
 * @requires os.arch=="amd64"
 * @run main/othervm TestRemoveJarInFatJar
 */
public class TestRemoveJarInFatJar implements PairProjectProvider {
    public static void main(String[] args) throws Exception {
        new StaticDiffTestRunner().runTest(new TestRemoveJarInFatJar());
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
                    Artifact.createFatJar("fat-jar", "lib", "fat-1.0.jar", new Artifact[]{
                            Artifact.createPlainJar("logger", "lib", "logger.jar", null, new JavaSource[]{StaticDiffTestConstant.LOGGER}, new ArtifactOption[]{ArtifactOption.LOAD_BY_URLCLASSLOADER}),
                            Artifact.createPlainJar("mylog4j", "lib", "mylog4j.jar", new String[]{"logger"}, new JavaSource[]{StaticDiffTestConstant.MY_LOG4J_LOGGER}, new ArtifactOption[]{ArtifactOption.LOAD_BY_URLCLASSLOADER}),
                            Artifact.createPlainJar("mylogback", "lib", "mylogback.jar", new String[]{"logger"}, new JavaSource[]{StaticDiffTestConstant.MY_LOGBACK_LOGGER}, new ArtifactOption[]{ArtifactOption.LOAD_BY_URLCLASSLOADER}),
                            Artifact.createPlainJar("loggerfactory", "lib", "loggerfactory.jar", new String[]{"logger"}, new JavaSource[]{StaticDiffTestConstant.LOGGER_FACTORY}, new ArtifactOption[]{ArtifactOption.LOAD_BY_URLCLASSLOADER}),
                            Artifact.createPlainJar("entry", "lib", "entry.jar", new String[]{"loggerfactory", "logger"}, new JavaSource[]{StaticDiffTestConstant.TEST_LOGGER_MAIN}, new ArtifactOption[]{ArtifactOption.LOAD_BY_URLCLASSLOADER})
                    }),
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
                    Artifact.createFatJar("fat-jar", "lib", "fat-1.0.jar", new Artifact[]{
                            Artifact.createPlainJar("logger", "lib", "logger.jar", null, new JavaSource[]{StaticDiffTestConstant.LOGGER}, new ArtifactOption[]{ArtifactOption.LOAD_BY_URLCLASSLOADER}),
                            Artifact.createPlainJar("mylogback", "lib", "mylogback.jar", new String[]{"logger"}, new JavaSource[]{StaticDiffTestConstant.MY_LOGBACK_LOGGER}, new ArtifactOption[]{ArtifactOption.LOAD_BY_URLCLASSLOADER}),
                            Artifact.createPlainJar("loggerfactory", "lib", "loggerfactory.jar", new String[]{"logger"}, new JavaSource[]{StaticDiffTestConstant.LOGGER_FACTORY}, new ArtifactOption[]{ArtifactOption.LOAD_BY_URLCLASSLOADER}),
                            Artifact.createPlainJar("entry", "lib", "entry.jar", new String[]{"loggerfactory", "logger"}, new JavaSource[]{StaticDiffTestConstant.TEST_LOGGER_MAIN}, new ArtifactOption[]{ArtifactOption.LOAD_BY_URLCLASSLOADER})
                    }),
            },
            new ExpectOutput(new String[]{"[logback]hello world",
                    StaticDiffTestConstant.EAGER_LOAD_SUCCESS_KEYWORD_IN_LOG + StaticDiffTestConstant.LOGGER.internalName(),
                    StaticDiffTestConstant.EAGER_LOAD_SUCCESS_KEYWORD_IN_LOG + StaticDiffTestConstant.LOGGER_FACTORY.internalName(),

            }, new String[]{
                    StaticDiffTestConstant.EAGER_LOAD_SUCCESS_KEYWORD_IN_LOG + StaticDiffTestConstant.MY_LOG4J_LOGGER.internalName(),
            }));
}
