/*
 * @test
 * @library /test/lib
 * @modules java.base/java.nio.file.spi:+open
 * @summary Test the fix to NPE issue caused by unexpected co-routine yielding on synchronized(lock) in FileSystems.getDefault().provider() during initialization of WispEngine
 * @run main/othervm -XX:+EnableCoroutine -Dcom.alibaba.wisp.transparentWispSwitch=true -XX:+UseWispMonitor TestFileSystemInitCritical
*/


import java.lang.reflect.Field;
import java.nio.file.spi.FileSystemProvider;
import java.util.concurrent.CountDownLatch;

public class TestFileSystemInitCritical {
    public static void main(String[] args) throws Exception {
        Field f = FileSystemProvider.class.getDeclaredField("lock");
        f.setAccessible(true);
        Object fileSystemProviderLock = f.get(null);
        CountDownLatch latch = new CountDownLatch(1);

        Thread t = new Thread(latch::countDown);

        synchronized (fileSystemProviderLock) {
            t.start();
            // Holding fileSystemProviderLock for a while which will eventually blocks the initialization of t' WispEngine
            Thread.sleep(100);
        }

        latch.await();

    }
}
