package com.alibaba.cds.dynamic.metadata;

import com.alibaba.cds.dynamic.ValidState;
import com.alibaba.cds.dynamic.metadata.mid.MidLayerInfo;
import com.alibaba.cds.dynamic.metadata.top.TopLayerInfo;
import com.alibaba.cds.dynamic.metadata.traverse.Closure;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static com.alibaba.cds.dynamic.checksum.Crc32.CHECKSUM_SKIP;

/**
 * Structures
 *
 * Info (Abstract)
 *   |
 *   -> NonLeafLayersInfo (Abstract)
 *   |    |
 *   |    -> TopLayerInfo (Abstract)
 *   |    |    |
 *   |    |    -> [1] FatJarInfo
 *   |    |
 *   |    -> MidLayerInfo (Abstract)
 *   |         |
 *   |         -> [2] DirInfo
 *   |         |
 *   |         -> [3] PlainJarInfo
 *   |         |
 *   |         -> [4] FatJarEntryDirInfo
 *   |         |
 *   |         -> [5] FatJarEntryPlainJarInfo
 *   |
 *   -> LeafLayerInfo (Abstract)
 *        |
 *        -> [6] KlassInfo
 */
public abstract class Info {

    public static final String CLASS_POSTFIX = ".class";
    public static final String JAR_POSTFIX   = ".jar";

    // <String jar, JarInfo info>
    public static HashMap<String, TopLayerInfo> info = new HashMap<>();

    // We use a 3-layer map to store all data:
    //
    // <fat jar>        / <thin jar inside the fat jar> / <klass>
    // <FAKE_TOP_LAYER> / <plain jar>                   / <klass>
    // <FAKE_TOP_LAYER> / <dir>                         / <klass>
    //
    // So for a plain jar which will be stored in the second layer,
    // we use a fake fat jar placeholder.

    protected final String recordedPathOrName;
    protected String       remappedPathOrName;   // Only for Heuristic: not null only when recordedPathOrName is not found when replaying.
    protected long         recordedChecksum;
    protected long         realChecksum;
    protected ValidState   state;

    // for lazy initialization:
    protected static final int NOT_INITIALIZED = 0;
    protected static final int INITIALIZING    = 1;
    protected static final int HAS_INITIALIZED = 2;
    protected AtomicInteger initStatus;

    protected Info(String recordedPathOrName, long recordedChecksum, ValidState state) {
        this.recordedPathOrName = recordedPathOrName;
        this.remappedPathOrName = null;
        this.recordedChecksum = recordedChecksum;
        this.realChecksum = CHECKSUM_SKIP;
        this.state = state;
        this.initStatus = new AtomicInteger(NOT_INITIALIZED);
    }

    public String getRecordedPathOrName() {
        return recordedPathOrName;
    }

    public String getRemappedPathOrName() {
        return remappedPathOrName;
    }

    public void setRemappedPathOrName(String remappedPathOrName) {
        this.remappedPathOrName = remappedPathOrName;
    }

    public long getRecordedCrc32() {
        return recordedChecksum;
    }

    public void setRecordedCrc32(long recordedChecksum) {
        this.recordedChecksum = recordedChecksum;
    }

    public long getRealCrc32() {
        return realChecksum;
    }

    public void setRealCrc32(long recordedChecksum) {
        this.realChecksum = recordedChecksum;
    }

    public ValidState getState() {
        return state;
    }

    public void setState(ValidState state) {
        this.state = state;
    }

    protected String maybeGetRemappedFilePath() {
        // A recorded jar might not be found when replaying.
        // We may return a heuristically remapped jar here.
        if (getRemappedPathOrName() != null) {
            return getRemappedPathOrName();
        }
        return getRecordedPathOrName();
    }

    public static void linkInfo(Info[] layers) {
        TopLayerInfo  top  = (TopLayerInfo) layers[0];
        MidLayerInfo mid  = (MidLayerInfo) layers[1];
        if (mid != null && top.get(mid.getRecordedPathOrName()) == null) {
            top.put(mid.getRecordedPathOrName(), mid);
        }
    }

    public abstract void traverseInner(Closure closure);

    public static void traverse(Closure closure) {
        closure.start();
        for (Map.Entry<String, TopLayerInfo> entry : Info.info.entrySet()) {
            final TopLayerInfo top = entry.getValue();
            top.traverseInner(closure);
            closure.postDoTopLayerInfo(top);
        }
    }

}
