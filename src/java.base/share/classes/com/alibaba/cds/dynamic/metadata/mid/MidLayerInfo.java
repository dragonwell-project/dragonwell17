package com.alibaba.cds.dynamic.metadata.mid;

import com.alibaba.cds.dynamic.DynamicCDSCheck;
import com.alibaba.cds.dynamic.ValidState;
import com.alibaba.cds.dynamic.metadata.Info;
import com.alibaba.cds.dynamic.metadata.LayersInfo;
import com.alibaba.cds.dynamic.metadata.SourceParser;
import com.alibaba.cds.dynamic.metadata.leaf.KlassInfo;
import com.alibaba.cds.dynamic.metadata.leaf.LeafLayerInfo;
import com.alibaba.cds.dynamic.metadata.traverse.Closure;

import java.util.HashMap;
import java.util.Map;

public abstract class MidLayerInfo extends LayersInfo {

    protected HashMap<String, LeafLayerInfo> leafMap = new HashMap<>();

    public MidLayerInfo(String pathOrName, long recordedCrc32, ValidState state) {
        super(pathOrName, recordedCrc32, state);
    }

    public String getSource() {
        return getRecordedPathOrName();
    }

    @Override
    public void resetSubLayerMap() {
        this.leafMap = null;
    }

    @Override
    public boolean hasSubLayerMap() {
        return this.leafMap != null;
    }

    @Override
    public Info get(String leafPathOrName) {
        return leafMap.get(leafPathOrName);
    }

    @Override
    public void put(String leafPathOrName, Info info) {
        leafMap.put(leafPathOrName, (LeafLayerInfo) info);
    }

    // return whether we succeed.
    public boolean recordLeaf(String klass, long fingerprint) {
        if (getState().isValid()) {
            assert !hasSubLayerMap() : "remove the leafMap for optimizations";
            // this whole jar is valid. so ignore this klass.
            return false;
        }

        put(klass + CLASS_POSTFIX, new KlassInfo(klass, fingerprint));
        return true;
    }

    protected boolean checkKlassValidationForMidLevelEntry(SourceParser parser, String klass) {
        klass = klass + CLASS_POSTFIX;
        LeafLayerInfo leaf = (LeafLayerInfo) get(klass);
        if (leaf == null) {
            // black
            return false;
        }

        return leaf.getState().isValid();
    }

    // Verify Leaf layers by loading disk files.
    public abstract void loadAndVerifyLeafLevel();

    @Override
    public String toString() {
        if (getState().isValid()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        int i = 0;
        for (Map.Entry<String, LeafLayerInfo> entry : leafMap.entrySet()) {
            LeafLayerInfo klassInfo = entry.getValue();
            if (!klassInfo.getState().isValid()) {  // print not-valid only (blacklist, fewer).
                sb.append("[").append(klassInfo.getState()).append(" ").append(i).append("] ").
                        append(getSource()).append(": ").append(klassInfo).
                        append(DynamicCDSCheck.JTREG_TEST ? " found in invalid classes list " : ""/* to pass tests */).
                        append(Long.toHexString(klassInfo.getRecordedCrc32()));
                if (i++ > 0) {
                    sb.append(System.lineSeparator());
                }
            }
        }

        // If this is a DirInfo and all klasses inside it is legal, here we print nothing.

        return sb.toString();
    }

    @Override
    public void traverseInner(Closure closure) {
        if (closure.shouldSkipCurrentLayer(this)) {
            return;
        }

        closure.doMidLayerInfo(this);

        if (closure.shouldSkipSubLayer(this)) {
            return;
        }

        if (leafMap == null) {
            return;
        }
        for (Map.Entry<String, LeafLayerInfo> entry : leafMap.entrySet()) {
            final LeafLayerInfo leaf = entry.getValue();
            leaf.traverseInner(closure);
        }
    }
}
