package com.alibaba.util.perf;

import java.text.NumberFormat;

public class SimpleMultiThreadPerfCounter extends AbstractPerfCounter {

    private static final NumberFormat nf = NumberFormat.getNumberInstance();
    static {
        nf.setMaximumFractionDigits(6);
    }

    public long start() {
        counter++;
        return System.nanoTime();
    }

    public void end(long start) {
        // Should use it in a "finally" block.
        time += (System.nanoTime() - start);
    }

}
