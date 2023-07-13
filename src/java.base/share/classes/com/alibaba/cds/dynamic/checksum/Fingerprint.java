package com.alibaba.cds.dynamic.checksum;

public class Fingerprint {

    // a Klass's fingerprint recorded in the VM

    public static long getCrc32(long fingerprint) {
        // must mirror: ClassFileStream::compute_fingerprint()
        return fingerprint & 0xFFFFFFFFL; // low 32-bit
    }
}
