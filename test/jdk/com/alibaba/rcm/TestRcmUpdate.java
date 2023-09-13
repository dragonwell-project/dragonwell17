/*
 * @test
 * @library /test/lib
 * @build TestRcmUpdate RcmUtils
 * @summary test RCM updating API.
 * @modules java.base/jdk.internal.access
 * @modules java.base/com.alibaba.rcm.internal:+open
 * @modules java.base/com.alibaba.wisp.engine:+open
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseWisp2 -XX:ActiveProcessorCount=4 TestRcmUpdate
 */

import com.alibaba.rcm.ResourceContainer;
import com.alibaba.rcm.ResourceType;

import java.util.concurrent.*;

import static jdk.test.lib.Asserts.assertTrue;
import static jdk.test.lib.Asserts.assertFalse;

public class TestRcmUpdate {
    public static void main(String[] args) throws Exception {
        ResourceContainer rc0 = RcmUtils.createContainer(ResourceType.CPU_PERCENT.newConstraint(40));
        assertTrue(rc0.getConstraints().iterator().next().getValues()[0] == 40);
        rc0.updateConstraint(ResourceType.CPU_PERCENT.newConstraint(1));
        assertTrue(rc0.getConstraints().iterator().next().getValues()[0] == 1);
    }
}
