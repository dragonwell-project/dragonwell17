package com.alibaba.cds.dynamic.metadata.traverse;

import com.alibaba.cds.dynamic.metadata.Info;
import com.alibaba.cds.dynamic.metadata.leaf.LeafLayerInfo;
import com.alibaba.cds.dynamic.metadata.mid.MidLayerInfo;
import com.alibaba.cds.dynamic.metadata.top.TopLayerInfo;
import com.alibaba.cds.dynamic.metadata.mid.DirInfo;
import com.alibaba.cds.dynamic.metadata.mid.FatJarEntryDirInfo;
import com.alibaba.cds.dynamic.metadata.mid.FatJarEntryPlainJarInfo;
import com.alibaba.cds.dynamic.metadata.mid.PlainJarInfo;
import com.alibaba.cds.dynamic.metadata.top.FatJarInfo;

import java.io.PrintStream;

public class PrettyPrintClosure implements Closure {

    protected PrintStream ps;
    protected boolean verbose;

    public PrettyPrintClosure(PrintStream ps, boolean verbose) {
        this.ps = ps;
        this.verbose = verbose;
    }

    protected static final int TOP_LAYER_ALIGNMENT  = 0;
    protected static final int MID_LAYER_ALIGNMENT  = 10;
    protected static final int LEAF_LAYER_ALIGNMENT = 20;

    protected static void printAlign(PrintStream ps, int num) {
        ps.print(" ".repeat(Math.max(0, num)));
    }

    @Override
    public void start() {
        ps.println("========================================");
    }

    @Override
    public boolean shouldSkipCurrentLayer(Info info) {
        if (verbose) {
            return false;   // if verbose, do not skip and print it.
        }
        return info.getState().isValid();
    }

    @Override
    public boolean shouldSkipSubLayer(Info info) {
        return info.getState().isNotFound() /* not found in Runtime */ ||
            (info != FatJarInfo.VIRTUAL_ROOT_LAYER_FAT_JAR && info.getState().isUnknown())  /* not used in Runtime */;
    }

    @Override
    public void doTopLayerInfo(TopLayerInfo info) {
        // alignment
        printAlign(ps, TOP_LAYER_ALIGNMENT);
        // <fat jar>
        ps.print(info.getRecordedPathOrName() + " " + info.getState());
        if (info.isFake()) {
            // do nothing
        } else if (info.getState().isNotFound() || info.getState().isUnknown()) {
            // do nothing
        } else {
            if (info.getRemappedPathOrName() != null) {
                ps.print(" [Remapping to: " + info.getRemappedPathOrName() + "]");
            }
            ps.print(" (" + Long.toHexString(info.getRecordedCrc32()) + " " + Long.toHexString(info.getRealCrc32()) + ")");
        }
        ps.println();
    }

    @Override
    public void postDoTopLayerInfo(TopLayerInfo info) {
        ps.println("========================================");
    }

    @Override
    public void doMidLayerInfo(MidLayerInfo info) {
        // alignment
        printAlign(ps, MID_LAYER_ALIGNMENT);

        ps.print(" -> " + info.getRecordedPathOrName() + " " + info.getState());
        if (info.getState().isNotFound() || info.getState().isUnknown()) {
            // do nothing
        } else if (info instanceof DirInfo || info instanceof FatJarEntryDirInfo) {
            // do nothing
        } else if (info instanceof PlainJarInfo || info instanceof FatJarEntryPlainJarInfo) {
            if (info instanceof PlainJarInfo && info.getRemappedPathOrName() != null) {
                ps.print(" [Remapping to: " + info.getRemappedPathOrName() + "]");
            }
            ps.print(" (" + Long.toHexString(info.getRecordedCrc32()) + " " + Long.toHexString(info.getRealCrc32()) + ")");
        } else {
            throw new Error("Should not reach here");
        }
        ps.println();
    }

    @Override
    public void doLeafLayerInfo(LeafLayerInfo info) {
        // alignment
        printAlign(ps, LEAF_LAYER_ALIGNMENT);

        ps.print(" -> " + info.getRecordedPathOrName() + Info.CLASS_POSTFIX /* Note: add a class postfix to keep it as a JarEntry. */ + " " + info.getState());
        if (info.getState().isNotFound() || info.getState().isUnknown()) {
            // do nothing
        } else {
            ps.print(" (" + Long.toHexString(info.getRecordedCrc32()) + " " + Long.toHexString(info.getRealCrc32()) + ")");
        }
        ps.println();
    }
}
