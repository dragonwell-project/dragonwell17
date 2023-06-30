package jdk.internal.misc;

import java.lang.ClassLoader;

public interface JavaLangClassLoaderAccess {
    public int getSignature(ClassLoader cl);

    public void setSignature(ClassLoader cl, int value);
}
