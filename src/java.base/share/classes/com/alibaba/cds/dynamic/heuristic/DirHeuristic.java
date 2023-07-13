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
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class DirHeuristic implements Heuristic {

    private final ConcurrentHashMap<String, String> dirMappings = new ConcurrentHashMap<>();

    protected DirHeuristic() {}

    @Override
    public boolean doInfo(Info info) {
        // Dir has been changed, e.g.
        // /opt/taobao/install/tomcat-7.0.108.ecj/bin/tomcat-juli.jar
        //   ->
        // /opt/taobao/install/tomcat-7.0.109/bin/tomcat-juli.jar

        assert info instanceof FatJarInfo || info instanceof PlainJarInfo : "currently only for jars";

        Path fullFilePath = Path.of(info.getRecordedPathOrName());
        Path dirPath = fullFilePath.getParent();
        assert !dirPath.toFile().exists() : "should not reach here: " + dirPath;
        Path filename = fullFilePath.getFileName();

        // search the cache first.
        String cache = dirMappings.get(dirPath.toString());
        if (cache != null) {
            Path newPath = Path.of(cache).resolve(filename);
            if (!newPath.toFile().exists()) {
                CDSLogger.log(LogLevel.ERROR, "[DirHeuristic] doesn't exist: " + newPath);
                return false;
            }
            info.setRemappedPathOrName(newPath.toString());
            CDSLogger.log(LogLevel.DEBUG, "[DirHeuristic] cached " + info.getRecordedPathOrName() + " -> " + info.getRemappedPathOrName());
            return true;
        }

        Path parent = fullFilePath;
        // Stack has synchronizations, which will harm performance.
        // We use a lock-free LinkedList here, to keep the best performance.
        LinkedList<Path> stack = new LinkedList<>();
        while ((parent = parent.getParent()) != null) {
            if (parent.toFile().exists()) {
                break;
            }
            stack.offerFirst(parent.getFileName());
        }

        assert !stack.isEmpty() : "must be";

        if (parent == null) {
            return false;
        }

        Trie t = new Trie();
        try {
            Files.list(parent).filter(p -> p.toFile().isDirectory()).forEach(p -> t.add(p.getFileName().toString()));
        } catch (IOException e) {
            // if an IO Exception happens: we ignore it and return.
            e.printStackTrace();
            return false;
        }

        // get the LCP with the different recorded dir
        String problematicDir = stack.pollFirst().toString();
        List<String> candidates = t.getCandidatesWithLCP(problematicDir);

        if (candidates.isEmpty()) {
            return false;
        }

        // we find some real-existed dirs.
        // see if one of them has the target file.
        Path remaining = Path.of("");
        while (!stack.isEmpty()) {
            remaining = remaining.resolve(stack.pollFirst());
        }
        remaining = remaining.resolve(filename);

        Path newPath = null;
        for (String candidate : candidates) {
            Path tmp = parent.resolve(candidate).resolve(remaining);
            if (tmp.toFile().exists()) {
                // successfully find the new file with the same name.
                newPath = tmp;
                break;
            }
        }

        if (newPath == null) {
            return false;
        }

        // record the cache
        dirMappings.put(dirPath.toString(), newPath.getParent().toString());

        info.setRemappedPathOrName(newPath.toString());

        CDSLogger.log(LogLevel.DEBUG, "[DirHeuristic] new " + info.getRecordedPathOrName() + " -> " + info.getRemappedPathOrName());

        return true;
    }

}
