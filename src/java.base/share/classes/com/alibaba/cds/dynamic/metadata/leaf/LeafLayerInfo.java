package com.alibaba.cds.dynamic.metadata.leaf;

import com.alibaba.cds.dynamic.ValidState;
import com.alibaba.cds.dynamic.metadata.Info;
import com.alibaba.cds.dynamic.metadata.SourceParser;
import com.alibaba.cds.dynamic.metadata.traverse.Closure;

public abstract class LeafLayerInfo extends Info {

    protected LeafLayerInfo(String klassName, long recordedFingerprint, ValidState state) {
        super(klassName, recordedFingerprint, state);
    }

    public String getLeafName() {
        return getRecordedPathOrName();
    }

    @Override
    public String toString() {
        return getLeafName();
    }

    @Override
    public void traverseInner(Closure closure) {
        if (closure.shouldSkipCurrentLayer(this)) {
            return;
        }

        closure.doLeafLayerInfo(this);
    }

    public void setRealCrc32AndState(long realCrc32) {
        setRealCrc32(realCrc32);
        // check recorded and real crc32
        if (getRecordedCrc32() == realCrc32) {
            setState(ValidState.VALID);
        } else {
            setState(ValidState.INVALID);
        }
    }
}
