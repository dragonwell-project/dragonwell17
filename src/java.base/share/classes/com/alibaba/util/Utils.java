package com.alibaba.util;

import jdk.internal.misc.JavaLangClassLoaderAccess;
import jdk.internal.access.SharedSecrets;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class Utils {

    private static JavaLangClassLoaderAccess JLCA = SharedSecrets.getJavaLangClassLoaderAccess();

    /* For class loader, use WeakReference to avoid additional handling when unloading the class loader
     * For the entry, in order to ensure the uniqueness during the life cyle of the java process,
     * it won't be removed when unloading the class loader.
     * */
    private static Map<Integer, WeakReference<ClassLoader>> hash2Loader = new ConcurrentHashMap<>();

    public static void registerClassLoader(ClassLoader loader, String identifier) {
        if (identifier == null || loader == null) {
            throw new IllegalArgumentException("[Register CL Exception] identifier or loader is null");
        }
        try {
            registerClassLoader(loader, identifier.hashCode());
        } catch (IllegalStateException e) {
            throw new IllegalStateException("[Register CL Exception] the identifier " + identifier + " with signature: " +
                                            Integer.toHexString(identifier.hashCode()) + " has already bean registered for loader " + loader);
        }
    }

    public static synchronized void registerClassLoader(ClassLoader loader, int signature) {
        if (signature == 0) {
            throw new IllegalArgumentException("[Register CL Exception] signature is zero");
        }
        if (JLCA.getSignature(loader) != 0) {
            throw new IllegalStateException("[Register CL Exception] loader with signature " + Integer.toHexString(signature) +
                                            " has already bean registered");
        }
        if (hash2Loader.containsKey(signature)) {
            throw new IllegalStateException("[Register CL Exception] has conflict: " + Integer.toHexString(signature) +
                                            " for loader " + loader);
        }
        hash2Loader.put(signature, new WeakReference<>(loader));
        JLCA.setSignature(loader, signature);
    }

    private Utils() {}
    public static WeakReference<ClassLoader> getClassLoader(int signature) {
        return hash2Loader.get(signature);
    }

    public static void printArgs(List<String> arguments, String msg, boolean verbose) {
        if (!verbose) {
            return;
        }
        System.out.print(msg);
        for (String s : arguments) {
            System.out.print(s + " ");
        }
        System.out.println();
    }

    public static final String JAVA_TOOL_OPTIONS = "JAVA_TOOL_OPTIONS";
    public static String removeAgentOp() {
        String toolOp = System.getenv(JAVA_TOOL_OPTIONS);
        return toolOp == null ? null : toolOp.replaceAll("-javaagent\\S*\\s?", " ");
    }

    public static void runProcess(List<String> arguments, boolean verbose, Consumer<ProcessBuilder> op) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(arguments).inheritIO();
        if (op != null) {
            op.accept(pb);
        }
        if (verbose) {
            pb.redirectErrorStream(true);
        } else {
            // ignore output to prevent child stucking at outputstream
            pb.redirectOutput(ProcessBuilder.Redirect.DISCARD).
                    redirectError(ProcessBuilder.Redirect.DISCARD);
        }
        Process p = pb.start();

        int ret = p.waitFor();
        boolean hasError;
        if ((hasError = (ret != 0)) || verbose) {
            System.out.println("return value: " + ret);
            if (hasError) {
                throw new Exception("Process failed");
            }
        }
    }

    public static void runProcess(boolean verbose, String msg, Consumer<ProcessBuilder> op, String... args) {
        List<String> command = List.of(args);
        Utils.printArgs(command, msg, verbose);
        try {
            Utils.runProcess(command, verbose, op);
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    public static String getJDKHome() {
        String jdkHome = System.getProperty("java.home");
        if (!new File(jdkHome).exists()) {
            throw new Error("Fatal error, cannot find jdk path: [" + jdkHome + "] doesn't exist!");
        }
        return jdkHome;
    }

    public static native String[] getModuleNames();
    public static native String getJDKBootClassPathAppend();
}
