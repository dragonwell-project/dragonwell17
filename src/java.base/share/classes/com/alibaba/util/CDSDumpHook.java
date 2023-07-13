package com.alibaba.util;

import com.alibaba.cds.CDSDumperHelper;

public class CDSDumpHook {

    public static class Info {
        public String originClassListName;
        public String finalClassListName;
        public String jsaName;
        public String jarFileLst;
        public final boolean eager;
        public final boolean EagerAppCDSDynamicClassDiffCheck;
        public Info(String cdsOriginClassList,
                    String cdsFinalClassList,
                    String cdsJsa,
                    String jarFileLst,  // Used for EagerAppCDSDynamicClassDiffCheck
                    boolean useEagerAppCDS,
                    boolean EagerAppCDSDynamicClassDiffCheck) {

            this.originClassListName = cdsOriginClassList;
            this.finalClassListName = cdsFinalClassList;
            this.jsaName = cdsJsa;
            this.jarFileLst = jarFileLst;
            this.eager = useEagerAppCDS;
            this.EagerAppCDSDynamicClassDiffCheck = EagerAppCDSDynamicClassDiffCheck;
        }
    }
    private static Info info;
    public static Info getInfo() { return info; }

    // called by JVM
    private static void initialize(String cdsOriginClassList, String cdsFinalClassList, String cdsJSA, String jarFileLst, boolean useEagerAppCDS, boolean EagerAppCDSDynamicClassDiffCheck) {
        info = new Info(cdsOriginClassList, cdsFinalClassList, cdsJSA, jarFileLst, useEagerAppCDS, EagerAppCDSDynamicClassDiffCheck);

        CDSDumpHook.setup();
    }

    private static void setup() {
        QuickStart.addDumpHook(() -> {
            try {
                CDSDumperHelper.invokeCDSDumper();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private CDSDumpHook() {}

}
