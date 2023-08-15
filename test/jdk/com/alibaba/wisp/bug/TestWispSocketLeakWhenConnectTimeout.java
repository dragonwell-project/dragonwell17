/*
 * @test
 * @library /test/lib
 * @summary test the fix to fd leakage when socket connect timeout
 * @run main/othervm -XX:+EnableCoroutine -Dcom.alibaba.wisp.transparentWispSwitch=true TestWispSocketLeakWhenConnectTimeout
*/

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Properties;

import static jdk.test.lib.Asserts.assertTrue;

public class TestWispSocketLeakWhenConnectTimeout {

    static Properties p;
    static String socketAddr;
    static {
        p = java.security.AccessController.doPrivileged(
                new java.security.PrivilegedAction<Properties>() {
                    public Properties run() {
                        return System.getProperties();
                    }
                }
        );
        socketAddr = (String)p.get("test.wisp.socketAddress");
        if (socketAddr == null) {
            socketAddr = "www.example.com";
        }
    }

    public static void main(String[] args) throws IOException {
        Socket so = new Socket();
        boolean timeout = false;
        try {
            so.connect(new InetSocketAddress(socketAddr, 80), 5);
        } catch (SocketTimeoutException e) {
            assertTrue(so.isClosed());
            timeout = true;
        }

        assertTrue(timeout, "SocketTimeoutException should been thrown");
    }
}
