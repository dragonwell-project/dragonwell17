package com.alibaba.cds.dynamic;

import com.alibaba.util.log.LogLevel;
import com.alibaba.util.log.Logger;

import java.io.PrintStream;

public class CDSLogger {

    private static Logger logger = null;

    public static void initialize(LogLevel level, PrintStream ps) {
        logger = new Logger(level, ps);
    }

    public static Logger getLogger() {
        return logger;
    }

    public static boolean shouldLog(LogLevel logLevel) {
        return logger.shouldLog(logLevel);
    }

    public static void log(String message) {
        logger.log(message);
    }

    public static void log(LogLevel logLevel, String message) {
        logger.log(logLevel, message);
    }

}
