package com.alibaba.util.perf;

public class SimplePerfCounter extends AbstractPerfCounter {

    // Single threaded.

    protected int recursive;
    protected long recordedNanoTime;

    private void resetRecordedNanoTime() {
        recordedNanoTime = 0;
    }

    private boolean isRecordedNanoTimeReset() {
        return recordedNanoTime == 0;
    }

    public void start() {
        counter++;
        if (!isRecordedNanoTimeReset()) {
            recursive++;
            return;
        }
        recordedNanoTime = System.nanoTime();
    }

    public void end() {
        // Should use it in a "finally" block.
        if (recursive != 0) {
            recursive--;
            return;
        }

        time += (System.nanoTime() - recordedNanoTime);
        resetRecordedNanoTime();
    }

}
