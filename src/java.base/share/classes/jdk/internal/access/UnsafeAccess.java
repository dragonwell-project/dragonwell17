package jdk.internal.access;

public interface UnsafeAccess {
    void unpark0(Object thread);

    void park0(boolean isAbsolute, long time);
}
