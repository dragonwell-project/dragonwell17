import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.jar.JarFile;

public class FatJarURLConnection extends JarURLConnection {
    private FatJarFile jarFile;
    private String realFile;

    public FatJarURLConnection(URL url, FatJarFile jar) throws MalformedURLException {
        super(url);
        this.url = url;
        this.jarFile = jar;
    }

    public FatJarURLConnection(URL url, FatJarFile jar, String realFile) throws MalformedURLException {
        super(url);
        this.url = url;
        this.jarFile = jar;
        this.realFile = realFile;
    }

    @Override
    public JarFile getJarFile() throws IOException {
        return jarFile;
    }

    @Override
    public void connect() throws IOException {
    }

    @Override
    public InputStream getInputStream() throws IOException {
        if (jarFile == null) {
            throw new FileNotFoundException(realFile);
        }
        return realFile == null ? jarFile.getInputStream() : jarFile.getInnerInputStream(realFile);
    }

    @Override
    public int getContentLength() {
        return realFile == null ? jarFile.getContentLength() : jarFile.getInnerContentLength(realFile);
    }
}
