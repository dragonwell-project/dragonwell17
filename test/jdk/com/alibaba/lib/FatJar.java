import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class FatJar {
    private final File root;
    private JarFile rootJar;
    private volatile boolean init;

    private Map<String, FatJarFile> nestJarMap = new HashMap<>();

    public FatJar(File root) throws IOException {
        this.root = root;
        rootJar = new JarFile(root);
    }

    public File getRoot() {
        return this.root;
    }

    public FatJarFile getNestJar(String nestJar) throws IOException {
        if (!init) {
            synchronized (this) {
                if (!init) {
                    readNestedJar();
                    init = true;
                }
            }
        }
        return nestJarMap.get(nestJar);
    }

    private void readNestedJar() throws IOException {
        byte[] wholeFile = new byte[(int) root.length()];
        FileInputStream fis = new FileInputStream(root);
        fis.read(wholeFile);
        fis.close();

        ByteArrayInputStream bais = new ByteArrayInputStream(wholeFile);
        ZipInputStream zis = new ZipInputStream(bais);
        ZipEntry zipEntry = zis.getNextEntry();

        byte[] buffer = new byte[4096];
        while (zipEntry != null) {
            ByteArrayOutputStream baos = readEntryContent(zis, buffer);
            int len;
            if (!zipEntry.isDirectory() && zipEntry.getName().endsWith(".jar")) {
                nestJarMap.put(zipEntry.getName(), new FatJarFile(root, zipEntry.getName(), baos.toByteArray(), false, rootJar));
            }
            zipEntry = zis.getNextEntry();
        }
        zis.close();
    }

    private ByteArrayOutputStream readEntryContent(ZipInputStream zis, byte[] buffer) throws IOException {
        int len = 0;
        int total = 0;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        while ((len = zis.read(buffer)) > 0) {
            baos.write(buffer, 0, len);
            total += len;
        }
        return baos;
    }

    public FatJarFile getNestDir(String dir) throws IOException {
        return new FatJarFile(root, dir, null, true, rootJar);
    }
}
