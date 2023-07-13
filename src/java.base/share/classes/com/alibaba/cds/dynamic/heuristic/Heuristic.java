package com.alibaba.cds.dynamic.heuristic;

import com.alibaba.cds.dynamic.metadata.Info;

public interface Heuristic {

    /**
     * To determine the remapping for not-founded jars/dirs heuristically,
     * @param info the not-founded jar/dir
     * @return whether we have found a remapped jar/dir successfully
     */
    boolean doInfo(Info info);

}
