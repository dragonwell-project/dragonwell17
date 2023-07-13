package com.alibaba.util.perf;

import com.alibaba.util.log.LogLevel;
import com.alibaba.util.log.Logger;

import java.io.PrintStream;
import java.text.NumberFormat;

public abstract class AbstractPerfCounter {

    protected static final NumberFormat nf = NumberFormat.getNumberInstance();
    static {
        nf.setMaximumFractionDigits(6);
    }

    protected long time;
    protected long counter;

    public long getCounter() {
        return counter;
    }

    public long getTime() {
        return time;
    }

    public void print(PrintStream ps, String name, long every) {
        if (every <= 0 /* recognize it as print data every time */ || counter % every == 0) {
            ps.println("[PerfCounter <" + name + ">] " + counter + " times cost: " + nf.format(((double)time)/ 1000_000_000) + " s");
        }
    }

    public void print(Logger logger, LogLevel logLevel, String name, long every) {
        if (every <= 0 /* recognize it as print data every time */ || counter % every == 0) {
            logger.log(logLevel, "[PerfCounter <" + name + ">] " + counter + " times cost: " + nf.format(((double)time)/1000_000_000) + " s");
        }
    }

}
