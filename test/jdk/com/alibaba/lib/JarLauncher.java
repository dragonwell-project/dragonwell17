import java.io.File;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;

public class JarLauncher {

    public static void run(Class c, String[] args) throws Exception {
        ProtectionDomain protectionDomain = c.getProtectionDomain();
        CodeSource codeSource = protectionDomain.getCodeSource();
        URI location = (codeSource != null) ? codeSource.getLocation().toURI() : null;
        String path = (location != null) ? location.getSchemeSpecificPart() : null;
        if (path == null) {
            throw new IllegalStateException("Unable to determine code source archive");
        }
        File root = new File(path);
        if (!root.exists()) {
            throw new IllegalStateException("Unable to determine code source archive from " + root);
        }
        run(root, args);
    }

    private static void run(File root, String[] args) throws Exception {
        List<URL> urlList = new ArrayList<>();
        String startClassName = JarUtil.readFarJar(new FatJar(root), urlList);

        URLClassLoader classLoader = new URLClassLoader(urlList.toArray(new URL[urlList.size()]), null);
        com.alibaba.util.Utils.registerClassLoader(classLoader, "JarLauncher");
        Class startClass = classLoader.loadClass(startClassName);
        Method main = startClass.getDeclaredMethod("main", new Class[]{String[].class});
        main.setAccessible(true);
        main.invoke(null, new Object[]{args});
    }


    public static void main(String[] args) throws Exception {
        run(new File(args[0]), new String[0]);
    }
}
