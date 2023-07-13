package com.alibaba.cds.dynamic.metadata;

import com.alibaba.cds.dynamic.CDSLogger;
import com.alibaba.cds.dynamic.DynamicCDSCheck;
import com.alibaba.cds.dynamic.ValidState;
import com.alibaba.cds.dynamic.heuristic.Heuristic;
import com.alibaba.cds.dynamic.metadata.mid.PlainJarInfo;
import com.alibaba.cds.dynamic.metadata.top.FatJarInfo;
import com.alibaba.util.log.LogLevel;

import java.io.File;
import java.io.IOException;
import java.util.jar.JarFile;

/**
 * [1]Top -> [2]Mid -> [3]Leaf
 * This interface represents [1] and [2].
 */
public abstract class LayersInfo extends Info {

    protected LayersInfo(String recordedPathOrName, long recordedChecksum, ValidState state) {
        super(recordedPathOrName, recordedChecksum, state);
    }

    public abstract Info get(String subLayerName);
    public abstract void put(String subLayerName, Info info);

    public abstract void resetSubLayerMap();
    public abstract boolean hasSubLayerMap();

    // Verify Top/Mid layers by loading disk files.
    public abstract void loadAndVerifyNonLeafLevel();

    // A Jar would find itself missing when replaying, for the jar's version get updated,
    // so it has been changed to another name. We use this strategy to heuristically
    // guessing what it has become.
    public abstract void loadAndVerifyNotFoundNonLeafLevelHeuristically(Heuristic heuristic);

    public abstract boolean checkKlassValidation(SourceParser parser, String klass);

    // return if this jar exists in runtime.
    protected boolean loadAndVerifyAJar(String path) {
        assert this instanceof PlainJarInfo || this instanceof FatJarInfo : "sanity";

        File jarFile = new File(path);
        if (!jarFile.exists()) {
            // at real time we don't have this recorded jar. very sad but we could do nothing here.
            setState(ValidState.NOTFOUND);
            CDSLogger.log(LogLevel.DEBUG, "[Not Found] didn't find [" + path + "] in runtime time! " + getState());
            return false;
        }

        try {
            if (this instanceof PlainJarInfo) {
                ((PlainJarInfo) this).setJar(new JarFile(jarFile));
            } else {
                ((FatJarInfo) this).setJar(new JarFile(jarFile));
            }
        } catch (IOException e) {
            throw new Error(e);
        }

        // first, test if this jar's real crc32 matches the recorded crc32
        long realJarCrc32 = DynamicCDSCheck.calculateFileCrc32(jarFile);
        CDSLogger.log(LogLevel.TRACE, "[DynamicCDSCheck real Jar] " + jarFile + " " + Long.toHexString(getRecordedCrc32()) + " " + Long.toHexString(realJarCrc32));
        setRealCrc32(realJarCrc32);
        if (getRecordedCrc32() != getRealCrc32()) {
            setState(ValidState.INVALID);
        } else {
            // successfully matched.
            setState(ValidState.VALID);
            resetSubLayerMap();         // the klasses in this jar are treated as white -- useless then.
        }
        return true;
    }

    // return if heuristically guessing is succeeded.
    protected void heuristicallyGuessLoadAndVerifyAJar(Heuristic heuristics) {
        assert getState().isNotFound() : "Only a not-found jar needs heuristically guessing";

        if (!DynamicCDSCheck.HEURISTIC) {
            return;
        }

        // heuristically map a jar for this guy.
        if (!heuristics.doInfo(this)) {
            // We didn't succeed, it is maybe:
            // 1. the whole directory in which the jar lays is changed
            // 2. the jar itself are actually removed by applications.
            // 3. etc.
            return;
        }

        // set the new state: NotFound -> Heuristic
        //  (a transient status)
        setState(ValidState.HEURISTIC_VALID);

        // redo this non-leaf level.
        loadAndVerifyNonLeafLevel();
    }

    protected boolean needInitialization() {
        // a simple do-once lock.
        while (true) {
            // check if initialized.
            final int status = this.initStatus.get();
            // if initialized before (or by other threads), we return.
            if (status == HAS_INITIALIZED) {
                return false;
            }
            // if not initialized, we try to acquire the lock.
            if (status == NOT_INITIALIZED &&
                    this.initStatus.compareAndSet(NOT_INITIALIZED, INITIALIZING)) {
                return true;
            }
        }
    }

    protected void endInitialization() {
        this.initStatus.set(HAS_INITIALIZED);
    }

    // Initialize the whole structure (only once)
    public abstract void tryInitialize();

    public void tryInitializeHelper(Runnable runnable, Runnable cleanup) {
        if (!needInitialization()) {
            return;
        }

        try {
            runnable.run();

            if (DynamicCDSCheck.JTREG_TEST) {
                CDSLogger.getLogger().getPrintStream().println(this);
            }
        } catch (Throwable t) {
            t.printStackTrace();
            // If error happens, sub layers are not filled.
            // So they are naturally marked as 'black'.
        } finally {
            cleanup.run();
        }
    }
}
