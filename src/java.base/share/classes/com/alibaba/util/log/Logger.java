package com.alibaba.util.log;

import java.io.PrintStream;

public class Logger {

    private final LogLevel logLevel;
    private final PrintStream ps;

    public Logger(LogLevel logLevel, PrintStream ps) {
        this.logLevel = logLevel;
        this.ps = ps;
    }

    public static boolean shouldLog(LogLevel config, LogLevel level) {
        return config.level <= level.level;
    }

    public boolean shouldLog(LogLevel level) {
        return shouldLog(level, this.logLevel);
    }

    public void log(LogLevel logLevel, String message) {
        if (shouldLog(logLevel)) {
            ps.println(message);
        }
    }

    public void log(String message) {
        ps.println(message);
    }

    public PrintStream getPrintStream() {
        return ps;
    }

}
