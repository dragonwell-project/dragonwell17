import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

public class URLClassLoaderLauncher {
    public static void main(String[] args) throws MalformedURLException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        String mainClass = args[0];
        URL[] urls = new URL[args.length - 1];
        for (int i = 1; i < args.length; i++) {
            urls[i - 1] = new File(args[i]).toURI().toURL();
        }
        URLClassLoader urlClassLoader = new URLClassLoader("URLClassLoaderLauncher", urls, URLClassLoaderLauncher.class.getClassLoader());
        urlClassLoader.loadClass(mainClass).getDeclaredMethod("main", String[].class).invoke(null, new Object[]{args});
    }
}
