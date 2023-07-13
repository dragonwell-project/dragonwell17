package com.alibaba.cds.dynamic.metadata.traverse;

import com.alibaba.cds.dynamic.metadata.Info;
import com.alibaba.cds.dynamic.metadata.leaf.LeafLayerInfo;
import com.alibaba.cds.dynamic.metadata.mid.MidLayerInfo;
import com.alibaba.cds.dynamic.metadata.top.TopLayerInfo;

public class StatisticsClosure implements Closure {

    private static final int BLACK_TOP_LAYER_INDEX  = 0;
    private static final int BLACK_MID_LAYER_INDEX  = 1;
    private static final int BLACK_LEAF_LAYER_INDEX = 2;
    private static final int FINAL_INDEX = BLACK_LEAF_LAYER_INDEX + 1;

    private final int[] blackListStatistics;

    public StatisticsClosure() {
        blackListStatistics = new int[FINAL_INDEX];
    }

    public int getBlackTopLayerNum() {
        return blackListStatistics[BLACK_TOP_LAYER_INDEX];
    }

    public int getBlackMidLayerNum() {
        return blackListStatistics[BLACK_MID_LAYER_INDEX];
    }

    public int getBlackLeafLayerNum() {
        return blackListStatistics[BLACK_LEAF_LAYER_INDEX];
    }

    @Override
    public void start() {}

    @Override
    public boolean shouldSkipCurrentLayer(Info info) {
        return info.getState().isValid();
    }

    @Override
    public boolean shouldSkipSubLayer(Info info) {
        return false;
    }

    @Override
    public void doTopLayerInfo(TopLayerInfo info) {
        blackListStatistics[BLACK_TOP_LAYER_INDEX]++;
    }

    @Override
    public void postDoTopLayerInfo(TopLayerInfo info) {}

    @Override
    public void doMidLayerInfo(MidLayerInfo info) {
        blackListStatistics[BLACK_MID_LAYER_INDEX]++;
    }

    @Override
    public void doLeafLayerInfo(LeafLayerInfo info) {
        blackListStatistics[BLACK_LEAF_LAYER_INDEX]++;
    }
}
