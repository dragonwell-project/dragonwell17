package com.alibaba.cds.dynamic.metadata;

import com.alibaba.cds.dynamic.CDSLogger;
import com.alibaba.cds.dynamic.ValidState;
import com.alibaba.cds.dynamic.metadata.mid.DirInfo;
import com.alibaba.cds.dynamic.metadata.mid.FatJarEntryDirInfo;
import com.alibaba.cds.dynamic.metadata.mid.FatJarEntryPlainJarInfo;
import com.alibaba.cds.dynamic.metadata.mid.PlainJarInfo;
import com.alibaba.cds.dynamic.metadata.top.FatJarInfo;
import com.alibaba.util.log.LogLevel;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import static com.alibaba.cds.dynamic.checksum.Crc32.CHECKSUM_PLACEHOLDER;
import static com.alibaba.cds.dynamic.metadata.top.FatJarInfo.VIRTUAL_ROOT_LAYER_FAT_JAR;

public class SourceParser {

    public enum Type {
        DIR,
        PLAIN_JAR,
        FAT_JAR,

        IMAGE,

        UNKNOWN;

        public boolean isPlainJar() {
            return this == PLAIN_JAR;
        }
        public boolean isDir() {
            return this == DIR;
        }
        public boolean isFatJar() {
            return this == FAT_JAR;
        }
        public boolean isJRT() {
            return this == IMAGE;
        }
        public boolean isUnknown() {
            return this == UNKNOWN;
        }
    }

    private final String originalSource;

    private Type type = Type.UNKNOWN;

    private final URL url;

    // This is used only for fat jar. For example:
    //   jar:file:<fat-jar>!/BOOT-INF/lib/<inner-plain-jar>!/
    // about the var 'url':
    //   url.protocol      <- 'jar'
    //   url.path          <- 'file:<fat-jar>!/BOOT-INF/lib/<inner-plain-jar>!/'
    // So we should parse the inner url:
    // so about the var 'urlInner':
    //   urlInner.protocol <- 'file'
    //   urlInner.path     <- '<fat-jar>!/BOOT-INF/lib/<inner-plain-jar>!/'
    private String[] urlInnerPathSplit = null;

    private SourceParser(String originalSource) throws MalformedURLException {
        this.originalSource = originalSource;
        this.url = new URL(originalSource);
        if (maybeJImageSlowCheck()) {
            this.type = Type.IMAGE;
        } else if (maybeDirSlowCheck()) {
            this.type = Type.DIR;
        } else if (maybePlainJarSlowCheck()) {
            this.type = Type.PLAIN_JAR;
        } else if (maybeFatJarSlowCheck()) {
            this.type = Type.FAT_JAR;
            URL urlInner = new URL(this.url.getPath());
            this.urlInnerPathSplit = urlInner.getPath().split("!/");
            if (this.urlInnerPathSplit.length != 1 && this.urlInnerPathSplit.length != 2) {
                // Note:
                //   it seems in the first step even inside a fat jar (Note that demo-0.0.1-SNAPSHOT.jar is a fat jar)
                //   org/springframework/boot/loader/jar/JarURLConnection$1 id: 935 origin: file:/home/demo/target/demo-0.0.1-SNAPSHOT.jar
                throw new MalformedURLException("A legal fatjar source must have 2 parts: " + urlInner.getPath() + " " + this.urlInnerPathSplit.length);
            }
        } else {
            throw new MalformedURLException("Unsupported protocol: " + originalSource);
        }
    }

    private boolean maybeDirSlowCheck() {
        return url.getProtocol().equals("file") && url.getPath().endsWith("/");
    }

    private boolean maybePlainJarSlowCheck() {
        return url.getProtocol().equals("file") && !url.getPath().endsWith("/");
    }

    private boolean maybeDirOrPlainJarSlowCheck() {
        return url.getProtocol().equals("file");
    }

    private boolean maybeFatJarSlowCheck() {
        return url.getProtocol().equals("jar");
    }

    private boolean maybeJImageSlowCheck() {
        return url.getProtocol().equals("jrt");
    }

    public static SourceParser generate(String originalSource) throws MalformedURLException {
        // Note:
        //   The originalSource is a legal URL at most time; but it can also be
        //     __JVM_DefineClass__
        //     __Lookup_defineClass__
        //     /<...>/jdk/lib/modules
        //   Or
        //     Other user defined source.
        //
        //   We cannot calculate crc32 from these obscure sources.
        //   We just tolerate these, with an Exception thrown out.
        //   Just please ignore it, mark it as untrustworthy, and continue.
        return new SourceParser(originalSource);
    }

    public String getOriginalSource() {
        return originalSource;
    }

    public boolean maybeDir() {
        return type.isDir();
    }

    public boolean maybePlainJar() {
        return type.isPlainJar();
    }

    public boolean maybeDirOrPlainJar() {
        return type.isDir() || type.isPlainJar();
    }

    public boolean maybeFatJar() {
        return type.isFatJar();
    }

    public boolean maybeJImage() {
        return type.isJRT();
    }

    public String getTopLayer() {
        if (maybeDirOrPlainJar()) {
            return FatJarInfo.VIRTUAL_ROOT_FAT_JAR_PATH;
        } else if (maybeFatJar()) {
            return urlInnerPathSplit[0];
        } else {
            throw new Error("Upper level should prevent from reaching here");
        }
    }

    public String getMidLayer() {
        if (maybeDirOrPlainJar()) {
            return url.getPath();
        } else if (maybeFatJar()) {
            if (urlInnerPathSplit.length == 1) {
                // this entry is for recording the fat jar's crc32.
                return null;
            } else {
                // length == 2
                return urlInnerPathSplit[1];
            }
        } else {
            throw new Error("Upper level should prevent from reaching here");
        }
    }

    public String updateFatJarOriginalSource(String newFatJar) {
        assert maybeFatJar() : "sanity";
        if (urlInnerPathSplit.length != 2) {
            return null;
        }
        return "jar:file:" + newFatJar + "!/" + urlInnerPathSplit[1] + "!/";
    }

    public String updatePlainJarOrDirOriginalSource(String newPlainJarOrDir) {
        assert maybePlainJar() : "sanity";
        return "file:" + newPlainJarOrDir;
    }

    // parse a String source to Info[]
    public static Info[] parseInfo(String originalSource, long recordedCrc32, String line /* debugging purpose */) throws IOException {
        SourceParser parser;
        try {
            parser = SourceParser.generate(originalSource);
        } catch (MalformedURLException e) {
            return null;
        }
        Info[] info = new Info[2];
        if (parser.maybeDirOrPlainJar()) {
            info[0] = VIRTUAL_ROOT_LAYER_FAT_JAR;
            String src = parser.getMidLayer();

            // Note: the file may not really exist here.
            boolean exists = new File(src).exists();
            final ValidState state = !exists ? ValidState.NOTFOUND : ValidState.UNKNOWN;

            if (parser.maybeDir()) {
                info[1] = new DirInfo(src, state);
            } else {
                info[1] = new PlainJarInfo(src, recordedCrc32, state);
            }
        } else if (parser.maybeFatJar()) {
            String src = parser.getTopLayer();

            // Note: the file may not really exist here.
            final ValidState state = !new File(src).exists() ? ValidState.NOTFOUND : ValidState.UNKNOWN;

            // check we have already added the fat jar before (we have processed other entries in the fat jar already)
            FatJarInfo top = (FatJarInfo) Info.info.get(src);
            if (top == null) {
                top = new FatJarInfo(src, CHECKSUM_PLACEHOLDER, state);
                Info.info.put(src, top);
            }

            info[0] = top;

            String mid = parser.getMidLayer();
            if (mid == null) {
                // this is an entry for recording the fat jar's crc32.
                // e.g. jar:file:<fat-jar-path> , in which mid is null here.
                CDSLogger.log(LogLevel.DEBUG, "[Patch fat jar's crc32] patch [" + src + "]'s crc32 to " + Long.toHexString(recordedCrc32));
                top.setRecordedCrc32(recordedCrc32);
            } else if (mid.endsWith(Info.JAR_POSTFIX)) {
                info[1] = new FatJarEntryPlainJarInfo(mid, recordedCrc32, ValidState.UNKNOWN);
            } else {
                info[1] = new FatJarEntryDirInfo(mid, top);
            }
        } else {
            // Unsupported source.
            return null;
        }
        return info;
    }

}
