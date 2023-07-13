package com.alibaba.cds.dynamic.heuristic;

public enum FailedReason {

    MAYBE_DIR_MODIFIED,

    UNKNOWN;

    boolean maybeDirModified() {
        return this == MAYBE_DIR_MODIFIED;
    }

}
