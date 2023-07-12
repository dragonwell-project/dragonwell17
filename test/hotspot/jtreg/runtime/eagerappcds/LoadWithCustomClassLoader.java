import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;

public class LoadWithCustomClassLoader {
    private static final Path CLASSES_DIR = Paths.get(System.getProperty("test.classes"));

    public static void main(String... args) throws Exception {
        //  class loader with name
        testURLClassLoader();
    }

    public static void testURLClassLoader() throws Exception {
        URL[] urls = new URL[] { CLASSES_DIR.toUri().toURL() };
        ClassLoader parent = ClassLoader.getSystemClassLoader();
        URLClassLoader loader = new URLClassLoader(urls, parent);  // a custom classloader

        Class<?> c = Class.forName("generatePackageInfo.Simple", true, loader);
        System.out.println(c);
    }

}

