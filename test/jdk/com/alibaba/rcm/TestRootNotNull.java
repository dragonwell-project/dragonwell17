/*
 * @test
 * @summary Test default ResourceContainer is root() instead of null
 * @library /test/lib
 * @modules java.base/jdk.internal.access
 * @modules java.base/com.alibaba.rcm.internal:+open
 * @run main TestRootNotNull
 */

import jdk.internal.access.SharedSecrets;

import static jdk.test.lib.Asserts.assertNotNull;

public class TestRootNotNull {
    public static void main(String[] args) {
        assertNotNull(SharedSecrets.getJavaLangAccess().getResourceContainer(Thread.currentThread()));
    }
}
