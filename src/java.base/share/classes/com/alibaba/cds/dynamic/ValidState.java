package com.alibaba.cds.dynamic;

public enum ValidState {

    UNKNOWN,
    NOTFOUND,
    INVALID,

    HEURISTIC_VALID,
    VALID;

    public boolean isUnknown() {
        return this == UNKNOWN;
    }

    public boolean isHeuristicValid() {
        return this == HEURISTIC_VALID;
    }

    public boolean isValid() {
        return this == VALID;
    }

    public boolean isNotFound() {
        return this == NOTFOUND;
    }

    public boolean isInvalid() {
        return this == UNKNOWN || this == INVALID;
    }

}
