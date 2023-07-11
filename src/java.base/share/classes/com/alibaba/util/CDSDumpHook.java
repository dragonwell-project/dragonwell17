package com.alibaba.util;

import com.alibaba.cds.CDSDumper;

public class CDSDumpHook {

    // JVM will set these fields
    private static String cdsOriginClassList;
    private static String cdsFinalClassList;
    private static String cdsJsa;
    private static String eagerAppCDSAgent;
    private static boolean useEagerAppCDS;
    private static boolean verbose;

    // called by JVM
    private static void initialize(String cdsOriginClassList, String cdsFinalClassList, String cdsJSA, String agent, boolean useEagerAppCDS, boolean verbose) {
        CDSDumpHook.cdsOriginClassList = cdsOriginClassList;
        CDSDumpHook.cdsFinalClassList = cdsFinalClassList;
        CDSDumpHook.cdsJsa = cdsJSA;
        CDSDumpHook.useEagerAppCDS = useEagerAppCDS;
        CDSDumpHook.eagerAppCDSAgent = agent;
        CDSDumpHook.verbose = verbose;

        CDSDumpHook.setup();
    }

    private static void setup() {
        QuickStart.addDumpHook(() -> {
            try {
                CDSDumper.dumpJSA(
                        useEagerAppCDS,
                        QuickStart.cachePath(),
                        cdsOriginClassList,
                        cdsFinalClassList,
                        cdsJsa,
                        eagerAppCDSAgent,
                        verbose);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private CDSDumpHook() {}

}
