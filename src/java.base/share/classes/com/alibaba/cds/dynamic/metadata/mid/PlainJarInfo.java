package com.alibaba.cds.dynamic.metadata.mid;

import com.alibaba.cds.dynamic.ValidState;
import com.alibaba.cds.dynamic.heuristic.Heuristic;
import com.alibaba.cds.dynamic.heuristic.Heuristics;
import com.alibaba.cds.dynamic.metadata.leaf.LeafLayerInfo;
import com.alibaba.cds.dynamic.metadata.SourceParser;

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class PlainJarInfo extends MidLayerInfo {

    private SoftReference<JarFile> jar;

    public PlainJarInfo(String jarPath, long recordedCrc32, ValidState state) {
        super(jarPath, recordedCrc32, state);
    }

    public JarFile getJar() {
        return jar.get();
    }

    public void setJar(JarFile jar) {
        this.jar = new SoftReference<>(jar);
    }

    @Override
    public void loadAndVerifyNonLeafLevel() {
        loadAndVerifyAJar(maybeGetRemappedFilePath());
    }

    @Override
    public void loadAndVerifyLeafLevel() {
        if (getState().isValid()) {
            // this jar is trustworthy. ignore it.
            return;
        } else if (getState().isNotFound()) {
            // this jar could not be found in runtime.
            return;
        }

        // we need to further check jarEntries inside this plain/fat jar.
        // read all entries inside the jar through IO.
        for (Enumeration<JarEntry> en = getJar().entries(); en.hasMoreElements(); ) {
            JarEntry e = en.nextElement();
            if (e.isDirectory()) {
                continue;
            }

            String name = e.getName();
            if (!name.endsWith(CLASS_POSTFIX)) {
                continue;
            }

            LeafLayerInfo klass = (LeafLayerInfo) get(name);
            if (klass == null) {
                // this jarEntry doesn't get recorded in the trace phase: ignore it.
                continue;
            }

            // check recorded and real crc32
            klass.setRealCrc32AndState(e.getCrc());
        }
    }

    @Override
    public void loadAndVerifyNotFoundNonLeafLevelHeuristically(Heuristic heuristics) {
        if (!getState().isNotFound()) {
            return;
        }

        heuristicallyGuessLoadAndVerifyAJar(heuristics);
    }

    @Override
    public void tryInitialize() {
        tryInitializeHelper(() -> {
            loadAndVerifyNonLeafLevel();
            loadAndVerifyNotFoundNonLeafLevelHeuristically(Heuristics.getInstance());
            loadAndVerifyLeafLevel();
        }, () -> {
            // close the jar file
            try {
                JarFile jarFile = getJar();
                if (jarFile != null) {
                    try {
                        jarFile.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                this.jar = null;
            } finally {
                endInitialization();
            }
        });
    }

    @Override
    public boolean checkKlassValidation(SourceParser parser, String klass) {
        tryInitialize();

        if (getState().isValid()) {
            return true;
        } else if (getState().isNotFound()) {
            return false;
        }

        return checkKlassValidationForMidLevelEntry(parser, klass);
    }

}
