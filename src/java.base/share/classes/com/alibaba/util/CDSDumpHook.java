package com.alibaba.util;

import com.alibaba.cds.CDSDumper;

public class CDSDumpHook {

    // JVM will set these fields
    private static String cdsOriginClassList;
    private static String cdsFinalClassList;
    private static String cdsJsa;
    private static String eagerAppCDSAgent;
    private static boolean useEagerAppCDS;

    // called by JVM
    private static void initialize(String cdsOriginClassList, String cdsFinalClassList, String cdsJSA, String agent, boolean useEagerAppCDS) {
        CDSDumpHook.cdsOriginClassList = cdsOriginClassList;
        CDSDumpHook.cdsFinalClassList = cdsFinalClassList;
        CDSDumpHook.cdsJsa = cdsJSA;
        CDSDumpHook.useEagerAppCDS = useEagerAppCDS;
        CDSDumpHook.eagerAppCDSAgent = agent;

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
                        eagerAppCDSAgent);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private CDSDumpHook() {}

}
