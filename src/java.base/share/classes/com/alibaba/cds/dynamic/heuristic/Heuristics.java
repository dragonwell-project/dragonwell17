package com.alibaba.cds.dynamic.heuristic;

import com.alibaba.cds.dynamic.metadata.Info;

public class Heuristics implements Heuristic {

    // This one is a unification of "Heuristic"s.

    private final FileHeuristic fileHeuristic;
    private final DirHeuristic  dirHeuristic;

    private Heuristics() {
        fileHeuristic = new FileHeuristic();
        dirHeuristic  = new DirHeuristic();
    }

    @Override
    public boolean doInfo(Info info) {
        boolean success = fileHeuristic.doInfo(info);
        final FailedReason reason = fileHeuristic.getFailedReason();
        fileHeuristic.resetFailedReason();
        if (!success && reason.maybeDirModified()) {
            success = dirHeuristic.doInfo(info);
        }
        return success;
    }

    private static final Heuristics theOne = new Heuristics();
    public static Heuristics getInstance() {
        return theOne;
    }
}
