package com.alibaba.cds.dynamic.metadata.leaf;

import com.alibaba.cds.dynamic.ValidState;
import com.alibaba.cds.dynamic.checksum.Fingerprint;

// Klasses inside a plain jar / Klasses inside a plain jar of a fat jar

public class KlassInfo extends LeafLayerInfo {

    public KlassInfo(String klassName, long recordedFingerPrint) {
        super(klassName, recordedFingerPrint, ValidState.UNKNOWN);
    }

    @Override
    public long getRecordedCrc32() {
        return Fingerprint.getCrc32(this.recordedChecksum);
    }

}
