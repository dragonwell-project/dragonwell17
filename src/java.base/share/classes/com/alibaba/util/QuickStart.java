package com.alibaba.util;

import com.alibaba.cds.CDSDumperHelper;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.security.PrivilegedAction;
import java.util.*;

public class QuickStart {

    private QuickStart(){}
    private static final String CONFIG_NAME               = "quickstart.properties";
    private static final String SERVERLESS_ADAPTER_NAME   = "ServerlessAdapter";
    private static final String CDSDUMPER_NAME            = "CDSDumper";

    // serverless adapter stuff
    private static String serverlessAdapter;
    public static void setServerlessAdapter(String serverlessAdapter) {
        QuickStart.serverlessAdapter = serverlessAdapter;
    }
    public static String getServerlessAdapter() {
        return serverlessAdapter;
    }

    private static native void registerNatives();

    static {
        registerNatives();

        loadQuickStartConfig();
    }

    private static void loadQuickStartConfig() {
        System.out.println("loadQuickStartConfig begin");
        // read the quickstart.properties config file
        @SuppressWarnings("removal")
        Properties p = java.security.AccessController.doPrivileged(
                (PrivilegedAction<Properties>) System::getProperties
        );
        Path path = Path.of(Utils.getJDKHome(), "conf", CONFIG_NAME);
        System.out.println("path:" + path.getFileName());
        try (InputStream is = new BufferedInputStream(new FileInputStream(path.toFile()))) {
            p.load(is);
        } catch (Exception e) {
            e.printStackTrace();
            throw new Error(e);
        }
        CDSDumperHelper.setCdsDumper(p.getProperty(CDSDUMPER_NAME));
        System.out.println("CDSDUMPER_NAME: " + p.getProperty(CDSDUMPER_NAME));
        QuickStart.setServerlessAdapter(p.getProperty(SERVERLESS_ADAPTER_NAME));
        System.out.println("SERVERLESS_ADAPTER_NAME: " + p.getProperty(SERVERLESS_ADAPTER_NAME));
        
    }

    /**
     * The enumeration is the same as VM level `enum QuickStart::QuickStartRole`
     */
    public enum QuickStartRole { NORMAL, TRACER, REPLAYER }

    private static QuickStartRole role = QuickStartRole.NORMAL;

    private final static List<Runnable> dumpHooks = new ArrayList<>();

    private static boolean verbose = false;

    public static boolean isVerbose() { return verbose; }

    // JVM will set these fields
    protected static String cachePath;

    // called by JVM
    private static void initialize(boolean isTracer, String cachePath, boolean verbose) {
        role = isTracer ? QuickStartRole.TRACER : QuickStartRole.REPLAYER;
        QuickStart.cachePath = cachePath;
        QuickStart.verbose = verbose;

        if (isTracer) {
            Runtime.getRuntime().addShutdownHook(new Thread(QuickStart::notifyDump));
        }
    }

    /**
     * Detect whether this Java process is a normal one.
     * Has the same semantics as VM level `!QuickStart::is_enabled()`
     * @return true if this Java process is a normal process.
     */
    public static boolean isNormal() {
        return role == QuickStartRole.NORMAL;
    }

    /**
     * Detect whether this Java process is a tracer.
     * Has the same semantics as VM level `QuickStart::is_tracer()`
     * @return true if this Java process is a tracer.
     */
    public static boolean isTracer() {
        return role == QuickStartRole.TRACER;
    }

    /**
     * Detect whether this Java process is a replayer.
     * Has the same semantics as VM level `QuickStart::is_replayer()`
     * @return true if this Java process is replayer.
     */
    public static boolean isReplayer() {
        return role == QuickStartRole.REPLAYER;
    }

    public static String cachePath() {
        return cachePath;
    }

    public static synchronized void addDumpHook(Runnable runnable) {
        if (notifyCompleted) {
            return;
        }
        dumpHooks.add(runnable);
    }

    public static synchronized void notifyDump() {
        if (notifyCompleted) {
            return;
        }
        for (Runnable dumpHook : dumpHooks) {
            dumpHook.run();
        }
        notifyDump0();
        notifyCompleted = true;
    }

    private static boolean notifyCompleted = false;

    private static native void notifyDump0();
}
