package com.alibaba.cds.dynamic.metadata.mid;

import com.alibaba.cds.dynamic.ValidState;
import com.alibaba.cds.dynamic.heuristic.Heuristic;
import com.alibaba.cds.dynamic.metadata.leaf.LeafLayerInfo;
import com.alibaba.cds.dynamic.metadata.SourceParser;
import com.alibaba.cds.dynamic.metadata.top.FatJarInfo;

import java.io.File;
import java.util.Map;
import java.util.jar.JarEntry;

import static com.alibaba.cds.dynamic.checksum.Crc32.CHECKSUM_SKIP;

public class FatJarEntryDirInfo extends MidLayerInfo {

    private final FatJarInfo top;

    public FatJarEntryDirInfo(String entry, FatJarInfo top) {
        super(entry, CHECKSUM_SKIP, ValidState.UNKNOWN);
        this.top = top;
    }

    @Override
    public void loadAndVerifyNonLeafLevel() {
        // no op
    }

    @Override
    public void loadAndVerifyNotFoundNonLeafLevelHeuristically(Heuristic heuristic) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void loadAndVerifyLeafLevel() {
        assert !getState().isValid() : "Treat dirs as unsafe as always";

        for (Map.Entry<String, LeafLayerInfo> recordedKlass : leafMap.entrySet()) {
            LeafLayerInfo klass = recordedKlass.getValue();
            if (klass.getState().isValid()) {
                // fast path: skip if already valid
                continue;
            }
            // get entry [ 'BOOT-INF/classes' + '/' + 'xxx/xxx/xxx' + '.class' ] in the fat jar
            JarEntry entry = top.getJar().getJarEntry(getRecordedPathOrName() + File.separator + klass.getRecordedPathOrName() + CLASS_POSTFIX);
            if (entry == null) {
                continue;
            }
            // check recorded and real crc32
            klass.setRealCrc32AndState(entry.getCrc());
        }
    }

    @Override
    public void tryInitialize() {
        tryInitializeHelper(this::loadAndVerifyLeafLevel, this::endInitialization);
    }

    @Override
    public boolean checkKlassValidation(SourceParser parser, String klass) {
        tryInitialize();

        return checkKlassValidationForMidLevelEntry(parser, klass);
    }
}