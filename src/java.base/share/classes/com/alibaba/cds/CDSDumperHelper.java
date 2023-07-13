package com.alibaba.cds;

import com.alibaba.util.CDSDumpHook;
import com.alibaba.util.QuickStart;
import com.alibaba.util.Utils;
import jdk.internal.misc.VM;

import java.nio.file.Path;

public class CDSDumperHelper {

    private static String cdsDumper;
    public static void setCdsDumper(String cdsDumper) {
        CDSDumperHelper.cdsDumper = cdsDumper;
    }

    private static String nonNullString(String str) {
        return str == null ? "" : str;
    }

    private static String[] restoreCommandLineOptions(String[] arguments) {
        // if user specifies '-Dcom.alibaba.wisp.threadAsWisp.black=name:process\ reaper\;name:epollEventLoopGroup\*' as the command line:
        // VM will change it to '-Dcom.alibaba.wisp.threadAsWisp.black=name:process reaper;name:epollEventLoopGroup*',
        // in which the escape character is removed. We will concat all of them with a ' ' (space character) because
        // users could use any character inside the command line. So we need to restore the ' ' inside the option back
        // to '\ ': '-Dcom.alibaba.wisp.threadAsWisp.black=name:process\ reaper;name:epollEventLoopGroup*'
        for (int i = 0; i < arguments.length; i++) {
            arguments[i] = arguments[i].replace(" ", "\\ ");
        }
        return arguments;
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
                info.jarFileLst,                                        // arg[3]: String finalClassListName
                Boolean.toString(info.eager),                           // arg[4]: boolean eager
                Boolean.toString(info.EagerAppCDSDynamicClassDiffCheck), // arg[5]: boolean EagerAppCDSDynamicClassDiffCheck
                info.jsaName,                                           // arg[6]: String jsaName
                Boolean.toString(verbose),                              // arg[7]: boolean verbose
                String.join(" ", restoreCommandLineOptions(VM.getRuntimeArguments())),
                                                                        // arg[8]: String runtimeCommandLine
                System.getProperty("java.class.path")                   // arg[9]: String cp
        );
    }
}
