package com.alibaba.cds.dynamic.checksum;

public class Crc32 {

    // no need to check the checksum(e.g. the crc32): e.g. for a dir.
    public static final long CHECKSUM_SKIP = -1;
    // we haven't filled the crc32 yet, will be patched later.
    public static final long CHECKSUM_PLACEHOLDER = -2;

}
