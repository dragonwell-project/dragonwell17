package com.alibaba.util;

import jdk.internal.misc.JavaLangClassLoaderAccess;
import jdk.internal.access.SharedSecrets;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
}
