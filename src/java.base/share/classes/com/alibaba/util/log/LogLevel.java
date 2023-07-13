package com.alibaba.util.log;

public enum LogLevel {

    NOLOG(0),
    ERROR(1),
    WARN(2),
    INFO(3),
    DEBUG(4),
    TRACE(5);

    final int level;

    LogLevel(int level) {
        this.level = level;
    }

}
