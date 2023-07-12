/*
 * @test
 * @library /test/lib
 * @summary Test jstack steal counter
 * @modules java.base/com.alibaba.wisp.engine:+open
 * @modules java.base/java.dyn:+open
 * @run main/othervm -XX:+EnableCoroutine -XX:+UseWispMonitor -Dcom.alibaba.transparentAsync=true -Dcom.alibaba.wisp.version=2 -XX:ActiveProcessorCount=2 JStackTest
 */

import com.alibaba.wisp.engine.WispEngine;
import com.alibaba.wisp.engine.WispTask;

import java.dyn.CoroutineBase;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.lang.reflect.Field;

import static jdk.test.lib.Asserts.*;

public class JStackTest {

    private static Field wisptask = null;
    private static Field stealCount = null;
    private static Field ctx = null;
    private static Field data = null;

    static {
        try {
            wisptask = WispEngine.class.getDeclaredField("current");
            wisptask.setAccessible(true);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        try {
            stealCount = WispTask.class.getDeclaredField("stealCount");
            stealCount.setAccessible(true);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        try {
            ctx = WispTask.class.getDeclaredField("ctx");
            ctx.setAccessible(true);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        try {
            data = CoroutineBase.class.getDeclaredField("nativeCoroutine");
            data.setAccessible(true);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    private static Map<String, Integer> map = new HashMap<>();

    private static void test() throws Exception {
        ReentrantLock lock = new ReentrantLock();
        lock.lock();
        for (int i = 0; i < 100; i++) {
            WispEngine.dispatch(() -> {
                Object o = new Object();
                synchronized (o) {
                    lock.lock(); // block until outter call unlock()
                    try {
                        WispTask current = (WispTask) wisptask.get(WispEngine.current());
                        long coroutine = (Long) data.get(ctx.get(current));
                        int count = (Integer) stealCount.get(current);
                        map.put("0x"+Long.toHexString(coroutine), count);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                    lock.unlock();
                }
            });
        }

        Thread.sleep(100);
        lock.unlock();

        List<String> result = jstack();

        int i = 0;
        for (; i < result.size(); i++) {
            if (result.get(i).contains("- Coroutine [")) {
                String str = result.get(i);
                String coroutine = matchCoroutine(str);
                int stealCount = Integer.parseInt(matchStealCount(str));
                assertTrue(map.get(coroutine) == stealCount);
            }
        }
    }

    private static String matchCoroutine(String str) {
        Pattern pattern = Pattern.compile(".*\\[(.*)\\].*");
        Matcher matcher = pattern.matcher(str);
        if (matcher.find()) {
            return matcher.group(1);
        }
        throw new RuntimeException("ShouldNotReachHere");
    }

    private static String matchStealCount(String str) {
        Pattern pattern = Pattern.compile(".*steal=(\\d+).*");
        Matcher matcher = pattern.matcher(str);
        if (matcher.find()) {
            return matcher.group(1);
        }
        throw new RuntimeException("ShouldNotReachHere");
    }

    private static List<String> jstack() throws Exception {
        List<String> statusLines = Files.readAllLines(Paths.get("/proc/self/status"));
        String pidLine = statusLines.stream().filter(l -> l.startsWith("Pid:")).findAny().orElse("1 -1");
        int pid = Integer.valueOf(pidLine.split("\\s+")[1]);

        Process p = Runtime.getRuntime().exec(System.getProperty("java.home") + "/bin/jstack " + pid);
        List<String> result = new BufferedReader(new InputStreamReader(p.getInputStream())).lines().collect(Collectors.toList());
        return result;
    }

    public static void main(String[] args) throws Exception {
        test();
    }
}
