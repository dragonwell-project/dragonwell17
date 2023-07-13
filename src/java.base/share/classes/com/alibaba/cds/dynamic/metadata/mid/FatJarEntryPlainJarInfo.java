package com.alibaba.cds.dynamic.metadata.mid;

import com.alibaba.cds.dynamic.ValidState;
import com.alibaba.cds.dynamic.heuristic.Heuristic;
import com.alibaba.cds.dynamic.metadata.leaf.LeafLayerInfo;
import com.alibaba.cds.dynamic.metadata.SourceParser;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

public class FatJarEntryPlainJarInfo extends MidLayerInfo {

    private SoftReference<JarEntry> plainJar = null;
    private SoftReference<byte[]>   plainJarBuffer = null;

    public void setPlainJarEntry(JarEntry jarEntry, byte[] buffer) {
        plainJar = new SoftReference<>(jarEntry);
        plainJarBuffer = new SoftReference<>(buffer);
    }

    public FatJarEntryPlainJarInfo(String entry, long recordedCrc32, ValidState state) {
        super(entry, recordedCrc32, state);
    }

    @Override
    public void loadAndVerifyNonLeafLevel() {
        // should call setPlainJarEntry() before calling this method

        if (getState().isValid()) {
            // this whole level is in the white list. ignore it.
            return;
        }

        if (plainJar == null) {
            // This plain jar in the fat jar recorded in the tracing step
            // doesn't exist in the real runtime.
            // bail out.
            return;
        }

        // check recorded and real crc32
        long realCrc32 = Objects.requireNonNull(plainJar.get()).getCrc();
        setRealCrc32(realCrc32);
        if (getRecordedCrc32() == getRealCrc32()) {
            setState(ValidState.VALID);
            resetSubLayerMap();
        } else {
            setState(ValidState.INVALID);
        }
    }

    @Override
    public void loadAndVerifyNotFoundNonLeafLevelHeuristically(Heuristic heuristic) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void loadAndVerifyLeafLevel() {
        // should call setPlainJarEntry() before calling this method

        if (getState().isValid()) {
            // this whole level is in the white list. ignore it.
            return;
        }

        if (plainJarBuffer == null) {
            // This plain jar in the fat jar recorded in the tracing step
            // doesn't exist in the real runtime.
            // bail out and remove it.
            return;
        }

        try {
            JarInputStream jis = new JarInputStream(new ByteArrayInputStream(Objects.requireNonNull(plainJarBuffer.get())));
            for (JarEntry klassEntry = jis.getNextJarEntry(); klassEntry != null; klassEntry = jis.getNextJarEntry()) {
                jis.readAllBytes();  // We must read all bytes here, so that it can calculate crc32.
                LeafLayerInfo klassInfo = leafMap.get(klassEntry.getName());
                if (klassInfo == null) {
                    continue;
                }
                assert klassInfo.getState().isUnknown() : "settle only once here";
                // check recorded and real crc32
                klassInfo.setRealCrc32AndState(klassEntry.getCrc());
            }
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    @Override
    public void tryInitialize() {
        tryInitializeHelper(() -> {
            loadAndVerifyNonLeafLevel();
            loadAndVerifyLeafLevel();
        }, this::endInitialization);
    }

    @Override
    public boolean checkKlassValidation(SourceParser parser, String klass) {
        tryInitialize();

        if (getState().isValid()) {
            return true;
        }

        return checkKlassValidationForMidLevelEntry(parser, klass);
    }

}