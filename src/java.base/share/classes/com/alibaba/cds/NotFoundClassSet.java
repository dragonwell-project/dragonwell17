package com.alibaba.cds;

import sun.security.action.GetPropertyAction;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class NotFoundClassSet {
    // The strings should be consistent with those defined in vm
    // please see in src/hotspot/share/classfile/systemDictionaryShared.hpp
    private final static String INITIATING_LOADER_STR = "initiating_loader_hash: ";
    private final static String NOT_FOUND_CLASS_STR = "source: not.found.class";

    private Set<String> notFoundClasses = null;
    @SuppressWarnings("removal")
    private NotFoundClassSet() {
        String notFoundClassList = null;
        try {
            notFoundClassList = java.security.AccessController.doPrivileged(
                    new GetPropertyAction("com.alibaba.cds.listPath"));
            buildNotFoundClassSet(notFoundClassList);
        } catch (Exception e) {
            System.out.println("[CDS Error] init not found map failed to : " + e.toString() + " " + notFoundClassList);
            e.printStackTrace(System.out);
        }
    }

    private static String toKey(String name, int hash) {
        return name + ":" + Integer.toHexString(hash);
    }

    public static boolean isNotFound(String name, int hash) {
        return InstanceHolder.holder.findClass(toKey(name, hash));
    }

    private void buildNotFoundClassSet(String name) throws Exception {
        notFoundClasses = new BufferedReader(new InputStreamReader(new FileInputStream(name))).lines()
                .filter(l -> l.contains(NOT_FOUND_CLASS_STR)).map(line -> {
                    int tagStart = line.indexOf(INITIATING_LOADER_STR);
                    assert tagStart != -1;
                    int end = line.indexOf(" ", tagStart + INITIATING_LOADER_STR.length());
                    int hash = Integer.parseUnsignedInt(line.substring(tagStart + INITIATING_LOADER_STR.length(), end == -1 ? line.length() : end), 16);
                    return toKey(line.substring(0, line.indexOf(" ")), hash);
                }).collect(Collectors.toSet());
    }

    private boolean findClass(String name) {
        return notFoundClasses != null && notFoundClasses.contains(name);
    }

    private static class InstanceHolder {
        private static final NotFoundClassSet holder = new NotFoundClassSet();
    }
}
