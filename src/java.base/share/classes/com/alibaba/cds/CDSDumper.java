package com.alibaba.cds;

import com.alibaba.util.QuickStart;
import jdk.internal.misc.VM;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class CDSDumper {

    private static boolean verbose = false;

    /// auto dump

    private static void printArgs(List<String> arguments, String msg) {
        if (!verbose) {
            return;
        }
        System.out.print(msg);
        for (String s : arguments) {
            System.out.print(s + " ");
        }
        System.out.println();
    }


    private static final String JAVA_TOOL_OPTIONS = "JAVA_TOOL_OPTIONS";
    private static String removeAgentOp() {
        String toolOp = System.getenv(JAVA_TOOL_OPTIONS);
        return toolOp == null ? null : toolOp.replaceAll("-javaagent\\S*\\s?", " ");
    }

    private static void runProcess(List<String> arguments) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(arguments);
        String toolOp = removeAgentOp();
        if (toolOp != null) {
            pb.environment().put(JAVA_TOOL_OPTIONS, toolOp); // clear up agent options because cds dump phase cannot live with java agent in peace
        }
        pb.redirectErrorStream(true);
        Process p = pb.start();

        StringBuilder sb = new StringBuilder();
        BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line + System.getProperty("line.separator"));
        }
        int ret = p.waitFor();
        br.close();
        if (ret != 0 || verbose) {
            System.out.println("return value: " + ret);
            System.out.println(sb.toString().trim());
        }
    }

    public static void runClasses4CDS(boolean eager, String jdkHome, String originClassListName, String finalClassListName) throws Exception {
        if (!eager)  return;

        List<String> command = new ArrayList<>();
        command.add(jdkHome + File.separator + "bin" + File.separator + "java");
        command.add(jdkHome + File.separator + "lib" + File.separator +
                "EagerAppCDS" + File.separator + "Classes4CDS.java");
        command.add(originClassListName);
        command.add(finalClassListName);
        command.add(QuickStart.cachePath());

        printArgs(command, "[Classes4CDS] ");

        runProcess(command);
    }

    public static void runDumpJSA(boolean eager, String jdkHome, String finalClassListName, String jsaPath, String agent, List<String> arguments) throws Exception {
        List<String> command = new ArrayList<>();
        command.add(jdkHome + File.separator + "bin" + File.separator + "java");
        command.addAll(arguments);
        command.add("-XX:+UnlockDiagnosticVMOptions");
        command.add("-Xshare:dump");
        command.add("-XX:SharedClassListFile=" + finalClassListName);
        command.add("-XX:SharedArchiveFile=" + jsaPath);
        if (eager) {
            command.add("-XX:+EagerAppCDS");
            command.add("-XX:+UnlockExperimentalVMOptions");
            command.add("-XX:+EagerAppCDSLegacyVerisonSupport");
            command.add("-Xbootclasspath/a:" + jdkHome + File.separator + "lib" + File.separator + agent);
        }
        // append classpath if has
        String cp = System.getProperty("java.class.path");
        if (cp != null && !cp.isEmpty()) {
            command.add("-cp");
            command.add(cp);
        }

        printArgs(command, "[Dump JSA Command] ");

        runProcess(command);
    }

    public static void dumpJSA(boolean eager, String dirPath, String originClassListName, String finalClassListName, String jsaName, String agent, boolean verbose) throws Exception {
        if (dirPath == null || originClassListName == null || finalClassListName == null || jsaName == null) {
            throw new RuntimeException("path is null? " + dirPath + " " + originClassListName + " " + finalClassListName + " " + jsaName);
        }
        CDSDumper.verbose = verbose;
        if (!dirPath.endsWith(File.separator))  dirPath += File.separator;
        File dir = new File(dirPath);
        if (!dir.exists() || !dir.isDirectory()) {
            throw new RuntimeException("dir is not a dir? " + dirPath);
        }
        dumpJSA(eager, dirPath + originClassListName, dirPath + finalClassListName, dirPath + jsaName, agent);
    }

    private static void dumpJSA(boolean eager, String originClassListName, String finalClassListName, String jsaPath, String agent) throws Exception {
        if (originClassListName == null || jsaPath == null) {
            throw new NullPointerException(originClassListName + " " + jsaPath);
        }

        // get JDK tools path
        String jdkHome = System.getProperty("java.home");
        if (!new File(jdkHome).exists()) {
            throw new Exception("Fatal error, cannot find jdk path: [" + jdkHome + "] doesn't exist!");
        }

        // check originClassListName existence
        if (!new File(originClassListName).exists()) {
            throw new FileNotFoundException(originClassListName + " doesn't exist!");
        }

        // run classes4cds
        runClasses4CDS(eager, jdkHome, originClassListName, finalClassListName);

        // deal with all jdk options
        List<String> arguments = new LinkedList<>(Arrays.asList(VM.getRuntimeArguments()));
        arguments.removeIf(arg -> arg.startsWith("-Xshare:off") ||
                arg.startsWith("-XX:SharedClassListFile") ||
                arg.startsWith("-XX:DumpLoadedClassList") ||
                arg.startsWith("-Xquickstart") ||
                arg.startsWith("-javaagent"));  /* disable javaagent on the dump step */

        printArgs(arguments, "[Current JVM commands] ");

        // run the shell script
        runDumpJSA(eager, jdkHome, eager ? finalClassListName : originClassListName, jsaPath, agent, arguments);
    }

}