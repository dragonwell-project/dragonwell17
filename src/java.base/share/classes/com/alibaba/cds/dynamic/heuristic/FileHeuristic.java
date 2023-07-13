package com.alibaba.cds.dynamic.heuristic;

import com.alibaba.cds.dynamic.CDSLogger;
import com.alibaba.cds.dynamic.metadata.Info;
import com.alibaba.cds.dynamic.metadata.mid.PlainJarInfo;
import com.alibaba.cds.dynamic.metadata.top.FatJarInfo;
import com.alibaba.util.adt.Trie;
import com.alibaba.util.log.LogLevel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileHeuristic implements Heuristic {

    // Heuristically guessing a new jar's name if it is not found in the replay step.
    // For example, a jar may get recorded in the tracing phase as 'xxx-1.0.1.jar',
    // but it might be changed into 'xxx-1.0.3.jar' in the replay step, for it has
    // been modified in code and re-published with a new version number.
    // So we need a way to re-map the recorded jar file with a one existed on the file system.

    // So, the strategy is actually a Trie.

    // Complexity comparison:
    // (X) If we match a jar name with all prefixes in a file array in a dir,
    //     let N be the prefixes num, and let M be the average length of a filename,
    //     it would be O(N*M).
    //
    // (V) But using a Trie to take care of the structure would result in O(M).
    //
    // So
    //   O(M), in which M is the average filename length.
    // We use a Trie structure to map it all.

    // A dir -> Trie cache to speed it up
    private final ConcurrentHashMap<String, Trie> dirToJars = new ConcurrentHashMap<>();

    private FailedReason failedReason = FailedReason.UNKNOWN;

    protected FileHeuristic() {}

    // There are really jars that look totally the same.
    // e.g.
    // an old original source
    // [/home/admin/buy2/target/buy2.war/WEB-INF/lib/business-shared-model-3.5.87-RELEASE-2022052302.jar]
    // matches both
    // [business-shared-model-1.0.5.jar, business-shared-model-3.6.3-RELEASE-2022072703.jar]
    // on the disk...
    // so we check if we are matched by the same pattern first,
    // then if both exactly (1) equals, we shall look at (2) or (3).

    //  (1)     (2)        (3)     In which: (1) must be exactly the same; (2) (3) could be a similar one, such as "RELEASE-20221132" or
    // ----1.0.1---20191201---.jar
    protected Pattern p0 = Pattern.compile("(.*?)[-_](?:\\d+\\.){2,}\\d+(.*?)\\d{4,}(.*?)\\.jar$");

    //  (1)        (2)
    // ----1.0.1-------.jar
    protected Pattern p1 = Pattern.compile("(.*?)[-_](?:\\d+\\.){2,}\\d+(.*?)\\.jar$");

    //  (1)        (2)
    // ----20191201----.jar     (maybe only month+date, like "0516" as well, so we at least match 4 numbers)
    protected Pattern p2 = Pattern.compile("(.*?)[-_]\\d{4,}(.*?)\\.jar$");

    protected Matcher match(String s) {
        // The order is important!
        // A jar name could be matched (maybe) by both p0 and p2.
        // This order is deliberately designed to be firstly using p0, then p1, and then p2.
        // Then we can match almost all ordinary jar names.
        // (Note: some fat jars not ending with ".jar" are not counted here.)
        Matcher matcher = p0.matcher(s);
        if (!matcher.find()) {
            matcher = p1.matcher(s);
            if (!matcher.find()) {
                matcher = p2.matcher(s);
                if (!matcher.find()) {
                    // Under my testing, it might be the dir is changed. For example:
                    //   /opt/taobao/install/tomcat-7.0.108.ecj/bin/tomcat-juli.jar
                    // Or, the jar is really not needed by applications any more so is removed.

                    // Note that the 's' might be a simple jar without version, such as 'tomcat-juli.jar'.
                    // p0, p1, and p2 cannot match such a case. But please note that, we use Heuristic only
                    // if we cannot find the file with the same name. So we should not worry about that here.
                    // the logic must be handled before.
                    CDSLogger.log(LogLevel.WARN, "[TrieHeuristic] Doesn't match: " + s);
                    return null;
                }
            }
        }
        // Under my testing, nearly all not-found jars could be matched through these rules.
        return matcher;
    }

    @Override
    public boolean doInfo(Info info) {
        assert info instanceof FatJarInfo || info instanceof PlainJarInfo : "currently only for jars";

        Path fullFilePath = Path.of(info.getRecordedPathOrName());
        Path dirPath = fullFilePath.getParent();
        if (!dirPath.toFile().exists()) {
            failedReason = FailedReason.MAYBE_DIR_MODIFIED;
            // this dir doesn't exist.
            CDSLogger.log(LogLevel.DEBUG, "[TrieHeuristic] Dir [" + dirPath + "] doesn't exists: " + fullFilePath);
            return false;
        }

        // build trie if needed.
        final String dir = dirPath.toString();
        final Trie trie = dirToJars.computeIfAbsent(dir, (d) -> {
            Trie t = new Trie();
            try {
                Files.list(Paths.get(d)).filter(
                        p -> !p.toFile().isDirectory() && p.getFileName().toString().endsWith(Info.JAR_POSTFIX)
                ).forEach(p -> t.add(p.getFileName().toString()));
            } catch (IOException e) {
                // if an IO Exception happens: we ignore it and return an empty trie.
                e.printStackTrace();
            }
            return t;
        });


        final String recordedJarFileName = fullFilePath.getFileName().toString();
        // search the trie for a potentially real jar.
        Matcher recordedM = match(recordedJarFileName);
        if (recordedM == null) {
            return false;
        }

        final String recordedPrefix = recordedM.group(1);

        List<String> candidatesOnDisk = trie.getCandidatesWithPrefix(recordedPrefix);
        if (candidatesOnDisk == null || candidatesOnDisk.isEmpty()) {
            return false;
        }

        String remappedRealJarName = null;
        String remappedRealJarPath = null;
        if (candidatesOnDisk.size() == 1) {
            // Note: pattern may be different here:
            //   someone would remove the years.
            //   hyperlocal-notouchdelivery-product-1.0.12-RELEASE-20220224.jar
            //     ->
            //   hyperlocal-notouchdelivery-product-1.0.15-RELEASE.jar
            remappedRealJarName = candidatesOnDisk.get(0);
            remappedRealJarPath = fullFilePath.getParent().resolve(remappedRealJarName).toString();
            CDSLogger.log(LogLevel.DEBUG, "[Mapping] [" + info.getRecordedPathOrName() + "] -> [" + remappedRealJarPath + "]");
        } else {
            // choose the nearest one.
            for (final String candidate : candidatesOnDisk) {
                Matcher realM = match(candidate);
                // cannot be matched. ignore this.
                if (realM == null) {
                    continue;
                }
                // 1. pattern need to have the same pattern.
                if (recordedM.pattern() != realM.pattern()) {
                    continue;
                }
                // 2. prefix must be equal.
                if (!recordedPrefix.equals(realM.group(1))) {
                    continue;
                }

                // we may choose this one.
                remappedRealJarName = candidate;
                remappedRealJarPath = fullFilePath.getParent().resolve(remappedRealJarName).toString();
                break;
            }
            if (remappedRealJarName == null) {
                // no one matches. bail out.
                if (CDSLogger.shouldLog(LogLevel.ERROR)) {
                    CDSLogger.log("[Failed matching] matched " + candidatesOnDisk.size() + ": [" + info.getRecordedPathOrName() + "]");
                    CDSLogger.log("  " + Arrays.toString(candidatesOnDisk.toArray(String[]::new)));
                    CDSLogger.log("  " + "[Mapping Failed] [" + info.getRecordedPathOrName() + "] -> [" + null + "]");
                }
                return false;
            }
            // some printing.
            if (CDSLogger.shouldLog(LogLevel.DEBUG)) {
                CDSLogger.log("[Selecting] matched " + candidatesOnDisk.size() + ": [" + info.getRecordedPathOrName() + "]");
                CDSLogger.log("  " + Arrays.toString(candidatesOnDisk.toArray(String[]::new)));
                CDSLogger.log("  " + "[Mapping] [" + info.getRecordedPathOrName() + "] -> [" + remappedRealJarPath + "]");
            }
        }

        info.setRemappedPathOrName(remappedRealJarPath);

        return true;
    }

    public FailedReason getFailedReason() {
        return failedReason;
    }

    public void resetFailedReason() {
        failedReason = FailedReason.UNKNOWN;
    }

}
