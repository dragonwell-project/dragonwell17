import com.alibaba.util.QuickStart;

import java.lang.management.ClassLoadingMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class TestDump {

    private static long CHECK_INTERVAL_MS;
    private static long CLASS_INC_COUNT;
    private static long MAX_START_SECONDS;
    private static ClassLoadingMXBean mxbean = ManagementFactory.getClassLoadingMXBean();

    static {
        Properties p = AccessController.doPrivileged((PrivilegedAction<Properties>)System::getProperties);
        CHECK_INTERVAL_MS = parsePositiveLongParameter(p, "-DcheckIntervalMS", 200);
        CLASS_INC_COUNT = parsePositiveLongParameter(p, "-DclassIncCount", 200);
        MAX_START_SECONDS = parsePositiveLongParameter(p, "-DmaxStartSeconds", 300);
    }

    private static long parsePositiveLongParameter(Properties p, String key, long defaultVal) {
        String value;
        if ((value = p.getProperty(key)) == null) {
            return defaultVal;
        }
        long res;
        try {
            res = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultVal;
        }
        return res <= 0 ? defaultVal : res;
    }

    private static Policy policy = null;

    interface Policy {
        boolean check();
    }

    static class ClassLoadingPolicy implements Policy {
        private final static long STARTING_MIN_INC = CLASS_INC_COUNT /
                TimeUnit.SECONDS.toMillis(1) * CHECK_INTERVAL_MS;
        private long lastLoadedClasses = 0;

        @Override
        public boolean check() {
            long cnt = mxbean.getLoadedClassCount();
            if (cnt - lastLoadedClasses <= STARTING_MIN_INC) {
                return true;
            }
            lastLoadedClasses = cnt;
            return false;
        }
    }

    static class WatcherThread extends Thread {

        public WatcherThread() {
            super("QuickStart-WatcherThread");
        }

        @Override
        public void run() {
            System.out.println("Watcher Thread begins...");
            long start = System.currentTimeMillis();

            do {
                try {
                    Thread.sleep(CHECK_INTERVAL_MS);
                } catch (InterruptedException e) {
                    return;
                }
            } while (System.currentTimeMillis() - start < TimeUnit.SECONDS.toMillis(MAX_START_SECONDS)
                    && !policy.check());
            QuickStart.notifyDump();
        }
    }

    public static boolean checkDumpUsingAPI() {
        RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
        List<String> arguments = runtimeMxBean.getInputArguments();
        for (String arg : arguments) {
            if (arg.contains("-DtestHooks=true")) {
                return true;
            }
        }
        return false;
    }

    public static final long SLEEP_MILLIS = 5000;
    public static final String ANCHOR = "QuickStart startup finish detected!";

    public static void main(String[] args) {
        if (checkDumpUsingAPI()) {
            policy = new ClassLoadingPolicy();
            WatcherThread thread = new WatcherThread();
            thread.setDaemon(true);
            thread.start();
        }
        QuickStart.addDumpHook(() -> {
            System.out.println(ANCHOR);
        });
        try {
            Thread.sleep(SLEEP_MILLIS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            System.out.println("finished");
        }
    }

}
