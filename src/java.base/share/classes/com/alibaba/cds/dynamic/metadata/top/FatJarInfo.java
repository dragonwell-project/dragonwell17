package com.alibaba.cds.dynamic.metadata.top;

import com.alibaba.cds.dynamic.ValidState;
import com.alibaba.cds.dynamic.checksum.Crc32;
import com.alibaba.cds.dynamic.heuristic.Heuristic;
import com.alibaba.cds.dynamic.heuristic.Heuristics;
import com.alibaba.cds.dynamic.metadata.mid.MidLayerInfo;
import com.alibaba.cds.dynamic.metadata.SourceParser;
import com.alibaba.cds.dynamic.metadata.mid.FatJarEntryPlainJarInfo;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;

public class FatJarInfo extends TopLayerInfo {

    public final static String VIRTUAL_ROOT_FAT_JAR_PATH = "/";

    // The path '/' will be an impossible fat jar path. So feel safe to use it as a fake one.
    public final static FatJarInfo VIRTUAL_ROOT_LAYER_FAT_JAR = new FatJarInfo(VIRTUAL_ROOT_FAT_JAR_PATH, Crc32.CHECKSUM_SKIP, ValidState.UNKNOWN);
    static {
        VIRTUAL_ROOT_LAYER_FAT_JAR.initStatus.set(HAS_INITIALIZED);
    }

    private SoftReference<JarFile> jar;

    public FatJarInfo(String fatJarPath, long recordedCrc32, ValidState state) {
        super(fatJarPath, recordedCrc32, state);
    }

    public JarFile getJar() {
        return jar.get();
    }

    public void setJar(JarFile jar) {
        this.jar = new SoftReference<>(jar);
    }

    @Override
    public boolean isFake() {
        return this == VIRTUAL_ROOT_LAYER_FAT_JAR;
    }

    @Override
    public void loadAndVerifyNonLeafLevel() {
        if (isFake()) {
            // We will update plain jars in their checkKlassValidation().
            return;
        }

        if (!loadAndVerifyAJar(maybeGetRemappedFilePath())) {
            return;
        }

        // fast path: this fat jar is valid: directly return
        if (getState().isValid()) {
            return;
        } else if (getState().isNotFound()) {
            return;
        }

        // verify the mid level plain jars if this fat jar's crc32 changed.
        File fatJar = new File(getRecordedPathOrName());
        byte[] file = new byte[(int) fatJar.length()];
        try {
            FileInputStream fis = new FileInputStream(fatJar);
            fis.read(file);
            fis.close();

            JarInputStream jis = new JarInputStream(new ByteArrayInputStream(file));
            JarEntry jarEntry = jis.getNextJarEntry();

            while (jarEntry != null) {
                // the jarEntry could be a jar, but also could be a dir, for example, 'BOOT-INC/classes'.
                MidLayerInfo mid = (MidLayerInfo) get(jarEntry.getName());
                if (mid instanceof FatJarEntryPlainJarInfo) {
                    byte[] entryBuffer = jis.readAllBytes();  // We must read all bytes here, so that it can calculate crc32.
                    // set the real JarEntry and byte buffer for FatJarEntyInfo
                    ((FatJarEntryPlainJarInfo) mid).setPlainJarEntry(jarEntry, entryBuffer);
                }

                jarEntry = jis.getNextJarEntry();
            }
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    @Override
    public void loadAndVerifyNotFoundNonLeafLevelHeuristically(Heuristic heuristics) {
        // Currently, we do it only for FatJarInfo / PlainJarInfo.
        if (!getState().isNotFound() || isFake() /* We will update plain jars in their checkKlassValidation() */) {
            return;
        }

        heuristicallyGuessLoadAndVerifyAJar(heuristics);
    }

    @Override
    public void tryInitialize() {
        if (!needInitialization()) {
            return;
        }

        try {
            loadAndVerifyNonLeafLevel();
            loadAndVerifyNotFoundNonLeafLevelHeuristically(Heuristics.getInstance());
        } catch (Throwable t) {
            t.printStackTrace();
            // If error happens, sub layers are not filled.
            // So they are naturally marked as 'black'.
        } finally {
            endInitialization();
        }
    }

    @Override
    public boolean checkKlassValidation(SourceParser parser, String klass) {
        tryInitialize();

        if (getState().isValid()) {
            return true;
        } else if (getState().isNotFound()) {
            return false;
        }

        MidLayerInfo mid = (MidLayerInfo) get(parser.getMidLayer());
        if (mid == null) {
            return false;
        }
        return mid.checkKlassValidation(parser, klass);
    }
}
