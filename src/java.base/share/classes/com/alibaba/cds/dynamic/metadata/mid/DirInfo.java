package com.alibaba.cds.dynamic.metadata.mid;

import com.alibaba.cds.dynamic.DynamicCDSCheck;
import com.alibaba.cds.dynamic.ValidState;
import com.alibaba.cds.dynamic.heuristic.Heuristic;
import com.alibaba.cds.dynamic.metadata.leaf.LeafLayerInfo;
import com.alibaba.cds.dynamic.metadata.SourceParser;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;

import static com.alibaba.cds.dynamic.checksum.Crc32.CHECKSUM_SKIP;

public class DirInfo extends MidLayerInfo {

    public DirInfo(String dirPath, ValidState state) {
        super(dirPath, CHECKSUM_SKIP, state);
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

        if (getState().isNotFound()) {
            return;
        }

        for (Map.Entry<String, LeafLayerInfo> info : leafMap.entrySet()) {
            String klassName = info.getKey();
            LeafLayerInfo klassInfo = info.getValue();
            File file = Path.of(getRecordedPathOrName(), klassName).toFile();
            if (!file.exists()) {
                klassInfo.setState(ValidState.NOTFOUND);
                continue;
            }

            klassInfo.setRealCrc32AndState(DynamicCDSCheck.calculateFileCrc32(file));
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