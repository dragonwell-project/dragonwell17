import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;

public class FatJarFile extends JarFile {
    private final byte[] data;
    private final String pathFromRoot;
    private final JarFile rootJar;
    private boolean isDir;
    private Map<String, byte[]> nestEntryDataMap = new HashMap<>();
    private Map<String, JarEntry> nestEntryMap = new HashMap<>();

    public FatJarFile(File root, String pathFromRoot, byte[] data, boolean isDir, JarFile rootJar) throws IOException {
        super(root);
        this.pathFromRoot = pathFromRoot;
        this.data = data;
        if (data != null) {
            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            JarInputStream jis = new JarInputStream(bais);
            JarEntry jarEntry = (JarEntry) jis.getNextEntry();
            while (jarEntry != null) {
                byte[] buffer = new byte[4096];
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                int total = 0;
                int len = 0;
                while ((len = jis.read(buffer)) > 0) {
                    baos.write(buffer, 0, len);
                    total += len;
                }
                nestEntryDataMap.put(jarEntry.getName(), baos.toByteArray());
                nestEntryMap.put(jarEntry.getName(), jarEntry);
                jarEntry = (JarEntry) jis.getNextEntry();
            }
        }
        this.isDir = isDir;
        this.rootJar = rootJar;
    }

    @Override
    public JarEntry getJarEntry(String name) {
        if (isDir) {
            return (JarEntry) rootJar.getEntry(pathFromRoot + "/" + name);
        } else {
            return nestEntryMap.get(name);
        }
    }

    @Override
    public ZipEntry getEntry(String name) {
        if (isDir) {
            return rootJar.getEntry(pathFromRoot + "/" + name);
        } else {
            return nestEntryMap.get(name);
        }
    }

    @Override
    public synchronized InputStream getInputStream(ZipEntry ze) throws IOException {
        return super.getInputStream(ze);
    }

    public synchronized InputStream getInputStream() {
        if (data != null) {
            return new ByteArrayInputStream(data);
        } else {
            JarEntry jarEntry = (JarEntry) rootJar.getEntry(pathFromRoot);
            if (jarEntry != null) {
                try {
                    return rootJar.getInputStream(jarEntry);
                } catch (IOException e) {
                    return null;
                }
            } else {
                return null;
            }
        }
    }

    public InputStream getInnerInputStream(String name) throws IOException {
        if (isDir) {
            JarEntry jarEntry = (JarEntry) rootJar.getEntry(pathFromRoot + "/" + name);
            if (jarEntry != null) {
                return rootJar.getInputStream(jarEntry);
            }
            return null;
        } else {
            byte[] data = nestEntryDataMap.get(name);
            if (data != null) {
                return new ByteArrayInputStream(data);
            } else {
                return null;
            }
        }
    }

    public int getInnerContentLength(String name) {
        if (isDir) {
            JarEntry jarEntry = (JarEntry) rootJar.getEntry(pathFromRoot + "/" + name);
            if (jarEntry != null) {
                return (int) jarEntry.getSize();
            } else {
                return -1;
            }
        } else {
            byte[] data = nestEntryDataMap.get(name);
            return data != null ? data.length : -1;
        }
    }

    public int getContentLength() {
        if (isDir) {
            ZipEntry zipEntry = rootJar.getEntry(pathFromRoot);
            if (zipEntry != null) {
                return (int) zipEntry.getSize();
            } else {
                return -1;
            }
        } else {
            return data.length;
        }
    }

}
