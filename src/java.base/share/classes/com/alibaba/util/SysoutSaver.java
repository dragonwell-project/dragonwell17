package com.alibaba.util;

import java.io.PrintStream;

public class SysoutSaver {
    private SysoutSaver() {}
    public static final PrintStream out;
    public static final PrintStream err;

    static {
        out = System.out;
        err = System.err;
    }

    public static void initialize() {
        // dummy: calling this to trigger <clinit> before main.
    }

}
