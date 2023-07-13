package com.alibaba.cds.dynamic;

import com.alibaba.cds.dynamic.metadata.Info;
import com.alibaba.cds.dynamic.metadata.mid.MidLayerInfo;
import com.alibaba.cds.dynamic.metadata.SourceParser;
import com.alibaba.cds.dynamic.metadata.top.TopLayerInfo;
import com.alibaba.cds.dynamic.metadata.traverse.PrettyPrintClosure;
import com.alibaba.cds.dynamic.metadata.traverse.StatisticsClosure;
import com.alibaba.cds.dynamic.metadata.traverse.SubmitTaskClosure;
import com.alibaba.util.SysoutSaver;
import com.alibaba.util.log.LogLevel;
import com.alibaba.util.perf.SimplePerfCounter;
import com.alibaba.util.perf.SimpleMultiThreadPerfCounter;

import java.io.*;
import java.net.MalformedURLException;
import java.security.PrivilegedAction;
import java.text.NumberFormat;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.zip.CRC32;

import static com.alibaba.cds.dynamic.metadata.top.FatJarInfo.VIRTUAL_ROOT_FAT_JAR_PATH;
import static com.alibaba.cds.dynamic.metadata.top.FatJarInfo.VIRTUAL_ROOT_LAYER_FAT_JAR;

public class DynamicCDSCheck {

    public static final String DYNAMIC_CDS_HEURISTIC  = "com.alibaba.cds.dynamic.heuristic";
    public static final String DYNAMIC_CDS_LOGLEVEL   = "com.alibaba.cds.dynamic.logLevel";
    public static final String DYNAMIC_CDS_JTREG_TEST = "com.alibaba.cds.dynamic.jtregTest";

    public static LogLevel LOG_LEVEL = LogLevel.NOLOG;
    public static boolean JTREG_TEST = false;
    public static boolean HEURISTIC  = true;

    static {
        @SuppressWarnings("removal")
        Properties p = java.security.AccessController.doPrivileged(
                (PrivilegedAction<Properties>) System::getProperties
        );

        String value;
        if ((value = p.getProperty(DynamicCDSCheck.DYNAMIC_CDS_LOGLEVEL)) != null) {
            try {
                LOG_LEVEL = LogLevel.valueOf(value);
            } catch (Exception e) {
                e.printStackTrace();
                // VERBOSE is set to null.
            }
        }
        if ((value = p.getProperty(DynamicCDSCheck.DYNAMIC_CDS_JTREG_TEST)) != null) {
            JTREG_TEST = Boolean.parseBoolean(value);
        }
        if ((value = p.getProperty(DynamicCDSCheck.DYNAMIC_CDS_HEURISTIC)) != null) {
            HEURISTIC = Boolean.parseBoolean(value);
        }

        // Users may change System.out. So we save it here to use it.
        SysoutSaver.initialize();

        // Register Logger.
        CDSLogger.initialize(LOG_LEVEL, SysoutSaver.out);

        CDSLogger.log(LogLevel.INFO, "[DEBUG] DynamicCDSCheck LOG_LEVEL: " + LOG_LEVEL + " JTREG_TEST: " + JTREG_TEST + " HEURISTIC: " + HEURISTIC);
    }

    private static boolean isEnabled = false;

    public static boolean isEnabled() {
        return DynamicCDSCheck.isEnabled;
    }

    // called from VM
    private static void initialize(String jarFileList, String cdsFinalList) {
        SimplePerfCounter perf = new SimplePerfCounter();
        perf.start();

        // add the fake fat jar layer for plain jars and dirs.
        Info.info.put(VIRTUAL_ROOT_FAT_JAR_PATH, VIRTUAL_ROOT_LAYER_FAT_JAR);

        try {
            readFromRecordedSourceList(jarFileList);

            readFromRecordedCDSFinalList(cdsFinalList);

            initializeDataStructureWithThreadPool();
        } catch (Throwable t) {
            // print and rethrow
            t.printStackTrace();
            throw t;
        } finally {
            perf.end();
            perf.print(CDSLogger.getLogger(), LogLevel.INFO, "DynamicCDSCheck initialize", 0);
        }
    }

    // 1. read from Jar file list
    private static void readFromRecordedSourceList(String sourceList) {
        SimplePerfCounter perf = new SimplePerfCounter();
        perf.start();
        final String separator = "\\|";
        try (BufferedReader br = new BufferedReader(new FileReader(sourceList))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] info = line.split(separator);
                // info[0]: original_source
                // info[1]: crc32
                long recordedCrc32 = Long.parseLong(info[1], 16);
                Info[] layers = SourceParser.parseInfo(info[0], recordedCrc32, line);
                if (layers != null) {
                    Info.linkInfo(layers);
                }
            }
        } catch (Throwable e) {
            // Fatal error.
            throwNewError(e, null);
        } finally {
            perf.end();
            perf.print(CDSLogger.getLogger(), LogLevel.INFO, "[step 1]", 0);
        }
    }

    // 2. read from CDS final list
    private static void readFromRecordedCDSFinalList(String cdsFinalList) {
        final String ID_ORIGINAL_SOURCE = "origin: ";
        final String ID_FINGERPRINT = "fingerprint: 0x";

        SimplePerfCounter perf = new SimplePerfCounter();
        perf.start();

        // White list Rules:
        // 1. record only klasses that have fingerprint
        // 2. ignore source == __Lookup_defineClass__, for they are not loaded from jars (unable to check crc32).
        // 3. only jars and klasses whose crc32 matches could they be added to the white list.
        // We strongly and conservatively depend on crc32 checks.
        try (BufferedReader br = new BufferedReader(new FileReader(cdsFinalList))) {
            String line;
            while ((line = br.readLine()) != null) {
                // if we have the important fingerprint
                if (line.contains(ID_FINGERPRINT)) {
                    String klassName = line.substring(0, line.indexOf(" "));
                    String fingerprintStr = line.substring(line.indexOf(ID_FINGERPRINT) + ID_FINGERPRINT.length()).trim();
                    long fingerprint = Long.parseLong(fingerprintStr, 16);
                    if (line.contains(ID_ORIGINAL_SOURCE)) {
                        int originBegin = line.indexOf(ID_ORIGINAL_SOURCE) + ID_ORIGINAL_SOURCE.length();
                        int originEnd   = line.indexOf(' ', originBegin);
                        String origin = line.substring(originBegin, originEnd);

                        SourceParser parser;
                        try {
                            parser = SourceParser.generate(origin);
                        } catch (MalformedURLException e) {
                            continue;
                        }
                        if (parser.maybeDirOrPlainJar()) {
                            String mid = parser.getMidLayer();
                            VIRTUAL_ROOT_LAYER_FAT_JAR.recordLeaf(mid, klassName, fingerprint);
                        } else if (parser.maybeFatJar()) {
                            String top = parser.getTopLayer();
                            TopLayerInfo info = Info.info.get(top);
                            if (info != null) {
                                String mid = parser.getMidLayer();
                                info.recordLeaf(mid, klassName, fingerprint);
                            }
                        } // ignore unrecognized source like "__JVM_DefineClass__", "__Lookup_defineClass__", or user defined ones.
                    }
                } // if no fingerprint recorded, we won't add record the klass into the Info.whiteList.
            }
        } catch (Throwable e) {
            // Fatal error.
            throwNewError(e, null);
        } finally {
            perf.end();
            perf.print(CDSLogger.getLogger(), LogLevel.INFO, "[step 2]", 0);
        }
    }

    private static void initializeDataStructureWithThreadPool() {
        // another thread: asynchronously
        Thread t = new Thread(() -> {
            ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() / 2);
            SubmitTaskClosure closure = new SubmitTaskClosure(executor);
            Info.traverse(closure);
            executor.shutdown();
            try {
                executor.awaitTermination(20, TimeUnit.SECONDS);
                CDSLogger.log(LogLevel.INFO, "DynamicCDSCheck initialization succeeded");
                isEnabled = true;
            } catch (InterruptedException e) {
                // set disabled
                CDSLogger.log(LogLevel.ERROR, "DynamicCDSCheck initialization failed! Disabling DynamicCDSCheck");
                isEnabled = false;

                e.printStackTrace();
                throw new Error(e);
            }
        }, "EagerAppCDS-DynamicClassDataStructureInitializer");
        t.start();

        if (JTREG_TEST) {
            // if we are in jtreg testing, we wait until this is done.
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private static void printRecordedWhiteList(PrintStream ps, boolean verbose) {
        PrettyPrintClosure closure = new PrettyPrintClosure(ps, verbose);
        Info.traverse(closure);
    }

    // Jcmd support
    private static void dumpCDSInfo(int jsaKlassesNumInTotal,
                                    boolean verbose,
                                    String filePath /* if null, then stdout */
                                    ) {
        // create PrintStream
        PrintStream ps = SysoutSaver.out;
        if (filePath != null) {
            File file = new File(filePath);
            try {
                if (!file.exists()) {
                    file.createNewFile();
                }
                ps = new PrintStream(new FileOutputStream(file));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // print details
        printRecordedWhiteList(ps, verbose);
        // print summary
        StatisticsClosure closure = new StatisticsClosure();
        Info.traverse(closure);
        final int total = jsaKlassesNumInTotal;
        final int black = closure.getBlackLeafLayerNum();
        final int white = total - black;
        ps.println();
        ps.println("[Summary] ");
        ps.println("  TotalKlassNumInJSA: " + total);
        ps.println("  BlackKlassNum:      " + black);
        ps.println("  WhiteKlassNum:      " + white);
        NumberFormat nf = NumberFormat.getNumberInstance();
        nf.setMaximumFractionDigits(2);
        ps.println("[JSA validation rate] " + nf.format(((double) white) / total * 100) + "%");
        ps.println();
        ps.println("(Black FatJar Num:    " + (closure.getBlackTopLayerNum() - 1 /* the fake top layer */) + ")");
        ps.println("(Black Mid Layer Num: " + closure.getBlackMidLayerNum() + ")");
    }

    public static long calculateFileCrc32(File file) {
        CRC32 c = new CRC32();

        byte[] buffer = new byte[40960];
        try (FileInputStream fis = new FileInputStream(file)) {
            int count;
            while ((count = fis.read(buffer)) != -1 /* EOF */) {
                c.update(buffer, 0, count);
            }
        } catch (IOException e) {
            throwNewError(e, null);
        }

        return c.getValue();
    }

    private static final SimpleMultiThreadPerfCounter perfCounter = new SimpleMultiThreadPerfCounter();
    // called from JVM
    private static boolean checkKlassValidation(String originalSource, String klass) {
        long start = perfCounter.start();

        SourceParser parser;
        try {
            parser = SourceParser.generate(originalSource);
        } catch (MalformedURLException e) {
            new Error("checkKlassValidation: " + originalSource, e).printStackTrace();
            return false;
        }

        Boolean res = null;
        try {
            res = checkKlassValidationHelper(parser, klass, false);
            return res;
        } finally {
            perfCounter.end(start);

            if (CDSLogger.shouldLog(LogLevel.TRACE)) {
                CDSLogger.log("[CheckKlassValidationHelper] source: [" + parser.getOriginalSource() + " " + klass + " " + res + "]");
            }

            perfCounter.print(CDSLogger.getLogger(), LogLevel.INFO, "checkKlassValidation", 10000);
        }
    }

    private static boolean checkKlassValidationHelper(SourceParser parser, String klass, boolean dummy) {
        // jimage has been processed in hotspot, before calling this
        if (parser.maybeFatJar()) {
            TopLayerInfo fatJarInfo = Info.info.get(parser.getTopLayer());
            if (fatJarInfo == null) {
                return false;
            }
            return fatJarInfo.checkKlassValidation(parser, klass);
        } else if (parser.maybeDirOrPlainJar()) {
            return VIRTUAL_ROOT_LAYER_FAT_JAR.checkKlassValidation(parser, klass);
        }
        return false;
    }

    private static final ConcurrentHashMap<String, String> updateSourceCache = new ConcurrentHashMap<>();
    public static String getSourceJarRemapping(String recordedOrigin) {
        assert DynamicCDSCheck.isEnabled() && DynamicCDSCheck.HEURISTIC : "sanity";

        String newSource = updateSourceCache.get(recordedOrigin);
        if (newSource != null) {
            return newSource;
        }

        // newSource is null here.

        try {
            SourceParser parser = SourceParser.generate(recordedOrigin);
            if (parser.maybeFatJar()) {
                TopLayerInfo top = Info.info.get(parser.getTopLayer());
                if (top != null && top.getRemappedPathOrName() != null) {
                    newSource = parser.updateFatJarOriginalSource(top.getRemappedPathOrName());
                }
            } else if (parser.maybeDirOrPlainJar()) {
                MidLayerInfo mid = (MidLayerInfo) VIRTUAL_ROOT_LAYER_FAT_JAR.get(parser.getMidLayer());
                if (mid != null && mid.getRemappedPathOrName() != null) {
                    newSource = parser.updatePlainJarOrDirOriginalSource(mid.getRemappedPathOrName());
                }
            } else {
                new Exception("Unknown source: " + recordedOrigin).printStackTrace();
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (newSource == null) {
            return null;
        }

        updateSourceCache.put(recordedOrigin, newSource);
        CDSLogger.getLogger().log(LogLevel.TRACE, "[UpdateSource] " + recordedOrigin + " to " + newSource);
        return newSource;
    }

    private static void throwNewError(Throwable e, String msg) {
        e.printStackTrace(SysoutSaver.out);   // print on the tty
        throw new Error(msg, e);
    }
}
