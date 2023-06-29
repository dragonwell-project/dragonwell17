/*
 * @test
 * @library /test/lib
 * @summary ensure thread.isAlive() is false after thread.join()
 * @run main/othervm -XX:+UseWisp2 ConcurrentThreadJoinTest
 */

import static jdk.test.lib.Asserts.assertFalse;

public class ConcurrentThreadJoinTest {
    public static void main(String[] args) throws Exception {
        long start = System.currentTimeMillis();

        while (System.currentTimeMillis() - start < 3000) {
            Thread thread = new Thread(() -> {
            });
            thread.start();
            thread.join(1000);
            assertFalse(thread.isAlive());
        }
    }
}
