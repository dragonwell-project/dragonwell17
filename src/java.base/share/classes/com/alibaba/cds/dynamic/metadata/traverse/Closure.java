package com.alibaba.cds.dynamic.metadata.traverse;

import com.alibaba.cds.dynamic.metadata.Info;
import com.alibaba.cds.dynamic.metadata.leaf.LeafLayerInfo;
import com.alibaba.cds.dynamic.metadata.mid.MidLayerInfo;
import com.alibaba.cds.dynamic.metadata.top.TopLayerInfo;

public interface Closure {

    void start();

    boolean shouldSkipCurrentLayer(Info info);
    boolean shouldSkipSubLayer(Info info);

    // do top layers
    void doTopLayerInfo(TopLayerInfo info);
    void postDoTopLayerInfo(TopLayerInfo info);

    // do mid layers
    void doMidLayerInfo(MidLayerInfo info);

    // do leaf layers
    void doLeafLayerInfo(LeafLayerInfo info);

}
