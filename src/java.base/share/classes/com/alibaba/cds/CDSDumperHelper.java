package com.alibaba.cds;

import com.alibaba.util.CDSDumpHook;
import com.alibaba.util.QuickStart;
import com.alibaba.util.Utils;
import jdk.internal.misc.VM;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Collectors;

public class CDSDumperHelper {

    private static String cdsDumper;
    public static void setCdsDumper(String cdsDumper) {
        CDSDumperHelper.cdsDumper = cdsDumper;
    }

    private static String nonNullString(String str) {
        return str == null ? "" : str;
    }

    public static void invokeCDSDumper() {
        CDSDumpHook.Info info = CDSDumpHook.getInfo();
        boolean verbose = QuickStart.isVerbose();

        String jdkHome = Utils.getJDKHome();
        Utils.runProcess(verbose, "[CDSDumper] ", (pb) -> {
                    // clear up agent options because cds dump phase cannot live with java agent in peace
                    String toolOp = Utils.removeAgentOp();
                    if (toolOp != null) {
                        pb.environment().put(Utils.JAVA_TOOL_OPTIONS, toolOp);
                    }
                },
                Path.of(jdkHome, "bin", "java").toString(),
                "-cp",
                Path.of(jdkHome, "lib", QuickStart.getServerlessAdapter()).toString(),
                "--add-exports=java.base/jdk.internal.misc=ALL-UNNAMED",
                cdsDumper,                                              // invoke CDSDumper
                QuickStart.cachePath(),                                 // arg[0]: String dirPath
                info.originClassListName,                               // arg[1]: String originClassListName
                info.finalClassListName,                                // arg[2]: String finalClassListName
                Boolean.toString(info.eager),                           // arg[3]: boolean eager
                info.jsaName,                                           // arg[4]: String jsaName
                nonNullString(info.agent),                              // arg[5]: String agent
                Boolean.toString(verbose),                              // arg[6]: boolean verbose
                Arrays.stream(VM.getRuntimeArguments()).
                                collect(Collectors.joining(" ")),       // arg[7]: String runtimeCommandLine
                System.getProperty("java.class.path")                   // arg[8]: String cp
        );
    }
}
