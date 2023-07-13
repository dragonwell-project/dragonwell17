package com.alibaba.cds.dynamic.metadata.traverse;

import com.alibaba.cds.dynamic.metadata.Info;
import com.alibaba.cds.dynamic.metadata.leaf.LeafLayerInfo;
import com.alibaba.cds.dynamic.metadata.mid.MidLayerInfo;
import com.alibaba.cds.dynamic.metadata.mid.PlainJarInfo;
import com.alibaba.cds.dynamic.metadata.top.TopLayerInfo;

import java.util.Map;
import java.util.concurrent.ExecutorService;

public class SubmitTaskClosure implements Closure {

    final private ExecutorService executor;

    public SubmitTaskClosure(ExecutorService executor) {
        this.executor = executor;
    }

    @Override
    public void start() {}

    @Override
    public boolean shouldSkipCurrentLayer(Info info) {
        return false;
    }

    @Override
    public boolean shouldSkipSubLayer(Info info) {
        if (info instanceof TopLayerInfo && ((TopLayerInfo) info).isFake()) {
            return false;
        }
        // If it's a real fat jar, we fix it in doTopLayerInfo because fat jar's sub layers
        // depend on this fat jar.
        return true;
    }

    @Override
    public void doTopLayerInfo(TopLayerInfo info) {
        if (info.isFake()) {
            executor.submit(info::tryInitialize);
        } else {
            executor.submit(() -> {
                info.tryInitialize();
                // A fat jar should first initialize itself then its belongings.
                for (Map.Entry<String, MidLayerInfo> entry : info.getMidMap().entrySet()) {
                    final MidLayerInfo mid = entry.getValue();
                    executor.submit(mid::tryInitialize);
                }
            });
        }
    }

    @Override
    public void postDoTopLayerInfo(TopLayerInfo info) {}

    @Override
    public void doMidLayerInfo(MidLayerInfo info) {
        assert info instanceof PlainJarInfo : "only a plain jar reaches here";
        executor.submit(info::tryInitialize);
    }

    @Override
    public void doLeafLayerInfo(LeafLayerInfo info) {
        throw new Error("Should not call this");
    }
}
