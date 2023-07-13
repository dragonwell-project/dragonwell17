package com.alibaba.cds.dynamic.metadata.top;

import com.alibaba.cds.dynamic.ValidState;
import com.alibaba.cds.dynamic.metadata.Info;
import com.alibaba.cds.dynamic.metadata.LayersInfo;
import com.alibaba.cds.dynamic.metadata.mid.MidLayerInfo;
import com.alibaba.cds.dynamic.metadata.traverse.Closure;

import java.util.HashMap;
import java.util.Map;

public abstract class TopLayerInfo extends LayersInfo {

    protected HashMap<String, MidLayerInfo> midMap = new HashMap<>();

    protected TopLayerInfo(String fatJarPath, long recordedCrc32, ValidState state) {
        super(fatJarPath, recordedCrc32, state);
    }

    public abstract boolean isFake();

    @Override
    public Info get(String midPathOrName) {
        return midMap.get(midPathOrName);
    }

    @Override
    public void put(String midPathOrName, Info midInfo) {
        midMap.put(midPathOrName, (MidLayerInfo) midInfo);
    }

    @Override
    public void resetSubLayerMap() {
        this.midMap = null;
    }

    @Override
    public boolean hasSubLayerMap() {
        return this.midMap != null;
    }

    // return whether we succeed.
    public boolean recordLeaf(String midNameOrPath, String klass, long fingerprint) {
        if (getState().isValid()) {
            assert !hasSubLayerMap() : "remove the midMap for optimizations";
            // this whole jar is valid. so ignore this klass.
            return false;
        }

        MidLayerInfo midLayerInfo = (MidLayerInfo) get(midNameOrPath);
        if (midLayerInfo == null) {
            return false;
        }
        return midLayerInfo.recordLeaf(klass, fingerprint);
    }

    @Override
    public String toString() {
        if (getState().isValid()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        int i = 0;
        for (Map.Entry<String, MidLayerInfo> entry : midMap.entrySet()) {
            MidLayerInfo midLayerInfo = entry.getValue();
            String mid = midLayerInfo.toString();
            if (!mid.isEmpty()) {
                sb.append(mid);
                if (i++ > 0) {
                    sb.append(System.lineSeparator());
                }
            }
        }

        return sb.toString();
    }

    @Override
    public void traverseInner(Closure closure) {
        if (closure.shouldSkipCurrentLayer(this)) {
            return;
        }

        closure.doTopLayerInfo(this);

        if (closure.shouldSkipSubLayer(this)) {
            return;
        }

        if (midMap == null) {
            return;
        }
        for (Map.Entry<String, MidLayerInfo> entry : midMap.entrySet()) {
            final MidLayerInfo mid = entry.getValue();
            mid.traverseInner(closure);
        }
    }

    public HashMap<String, MidLayerInfo> getMidMap() {
        return midMap;
    }
}
