/*
 * @test
 * @summary Test ONLY inheritedResourceContainer is inherited from parent
 * @library /test/lib
 * @modules java.base/jdk.internal.access
 * @modules java.base/com.alibaba.rcm.internal:+open
 * @run main TestInheritedContainer
 */

import com.alibaba.rcm.ResourceContainer;
import demo.MyResourceFactory;
import jdk.internal.access.SharedSecrets;
import jdk.internal.access.JavaLangAccess;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import static jdk.test.lib.Asserts.assertEQ;


public class TestInheritedContainer {
    public static void main(String[] args) throws Exception {
        ResourceContainer rc = MyResourceFactory.INSTANCE.createContainer(Collections.emptyList());
        JavaLangAccess JLA = SharedSecrets.getJavaLangAccess();
        rc.run(() -> {
            try {
                assertEQ(ResourceContainer.root(),
                        CompletableFuture.supplyAsync(() -> JLA.getResourceContainer(Thread.currentThread())).get());
                assertEQ(rc,
                        CompletableFuture.supplyAsync(() -> JLA.getInheritedResourceContainer(Thread.currentThread())).get());
            } catch (Exception e) {
                throw new AssertionError(e);
            }
        });
    }
}
