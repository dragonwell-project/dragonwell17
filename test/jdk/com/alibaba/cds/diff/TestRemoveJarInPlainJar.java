/**
 * @test
 * @summary A jar in fat jar was removed
 * @library /test/lib /com/alibaba/lib
 * @requires os.arch=="amd64"
 * @run main/othervm TestRemoveJarInPlainJar static
 * @run main/othervm TestRemoveJarInPlainJar dynamic
 */
public class TestRemoveJarInPlainJar extends PairProjectProvider {

    public TestRemoveJarInPlainJar(String arg) {
        super(arg);
    }

    public static void main(String[] args) throws Exception {
        new DiffTestRunner().runTest(new TestRemoveJarInPlainJar(args[0]));
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
                    Artifact.createPlainJar("logger", "lib", "logger.jar", null, new JavaSource[]{Constant.LOGGER}, new ArtifactOption[]{ArtifactOption.LOAD_BY_URLCLASSLOADER}),
                    Artifact.createPlainJar("mylog4j", "lib", "mylog4j.jar", new String[]{"logger"}, new JavaSource[]{Constant.MY_LOG4J_LOGGER}, new ArtifactOption[]{ArtifactOption.LOAD_BY_URLCLASSLOADER}),
                    Artifact.createPlainJar("mylogback", "lib", "mylogback.jar", new String[]{"logger"}, new JavaSource[]{Constant.MY_LOGBACK_LOGGER}, new ArtifactOption[]{ArtifactOption.LOAD_BY_URLCLASSLOADER}),
                    Artifact.createPlainJar("loggerfactory", "lib", "loggerfactory.jar", new String[]{"logger"}, new JavaSource[]{Constant.LOGGER_FACTORY}, new ArtifactOption[]{ArtifactOption.LOAD_BY_URLCLASSLOADER}),
                    Artifact.createPlainJar("entry", "lib", "entry.jar", new String[]{"loggerfactory", "logger"}, new JavaSource[]{Constant.TEST_LOGGER_MAIN}, new ArtifactOption[]{ArtifactOption.LOAD_BY_URLCLASSLOADER})
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
                    Artifact.createPlainJar("logger", "lib", "logger.jar", null, new JavaSource[]{Constant.LOGGER}, new ArtifactOption[]{ArtifactOption.LOAD_BY_URLCLASSLOADER}),
                    Artifact.createPlainJar("mylogback", "lib", "mylogback.jar", new String[]{"logger"}, new JavaSource[]{Constant.MY_LOGBACK_LOGGER}, new ArtifactOption[]{ArtifactOption.LOAD_BY_URLCLASSLOADER}),
                    Artifact.createPlainJar("loggerfactory", "lib", "loggerfactory.jar", new String[]{"logger"}, new JavaSource[]{Constant.LOGGER_FACTORY}, new ArtifactOption[]{ArtifactOption.LOAD_BY_URLCLASSLOADER}),
                    Artifact.createPlainJar("entry", "lib", "entry.jar", new String[]{"loggerfactory", "logger"}, new JavaSource[]{Constant.TEST_LOGGER_MAIN}, new ArtifactOption[]{ArtifactOption.LOAD_BY_URLCLASSLOADER})
            },
            new ExpectOutput(new String[]{"[logback]hello world",
                    Constant.EAGER_LOAD_SUCCESS_KEYWORD_IN_LOG + Constant.LOGGER.internalName(),
                    Constant.EAGER_LOAD_SUCCESS_KEYWORD_IN_LOG + Constant.LOGGER_FACTORY.internalName(),

            }, new String[]{
                    Constant.EAGER_LOAD_SUCCESS_KEYWORD_IN_LOG + Constant.MY_LOG4J_LOGGER.internalName(),
            }));
}
