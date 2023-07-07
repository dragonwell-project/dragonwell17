package com.alibaba.util;

import java.util.ArrayList;
import java.util.List;

public class QuickStart {

    private QuickStart(){}

    private static native void registerNatives();

    static {
        registerNatives();
    }

    /**
     * The enumeration is the same as VM level `enum QuickStart::QuickStartRole`
     */
    public enum QuickStartRole { NORMAL, TRACER, REPLAYER }

    private static QuickStartRole role = QuickStartRole.NORMAL;

    private final static List<Runnable> dumpHooks = new ArrayList<>();

    // JVM will set these fields
    protected static String resourcePath;

    // called by JVM
    private static void initialize(boolean isTracer, String resourcePath) {
        role = isTracer ? QuickStartRole.TRACER : QuickStartRole.REPLAYER;
        QuickStart.resourcePath = resourcePath;

        Runtime.getRuntime().addShutdownHook(new Thread(QuickStart::notifyDump));
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

    public static String resourcePath() {
        return resourcePath;
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
