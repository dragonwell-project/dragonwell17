#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <pthread.h>
#include <unistd.h>


JavaVM *jvm;       /* denotes a Java VM */

jobject lock;
volatile int fooGotLock;

int
main()
{
    JNIEnv *env;
    JavaVMInitArgs vm_args; /* JDK/JRE 6 VM initialization arguments */
    JavaVMOption options[4];
    options[0].optionString = "-XX:+EnableCoroutine";
    options[1].optionString = "-XX:-UseBiasedLocking";
    options[2].optionString = "-Dcom.alibaba.transparentAsync=true";
    options[3].optionString = "-XX:+UseWispMonitor";
    vm_args.version = JNI_VERSION_1_6;
    vm_args.nOptions = 4;
    vm_args.options = options;
    vm_args.ignoreUnrecognized = JNI_FALSE;
    /* load and initialize a Java VM, return a JNI interface
     * pointer in env */
    if (JNI_CreateJavaVM(&jvm, (void** )&env, &vm_args) != JNI_OK) {
        exit(-1);
    }

    jclass cls = (*env)->FindClass(env, "java/nio/file/spi/FileSystemProvider");
    printf("class = %p\n", cls);
    jfieldID fid = (*env)->GetStaticFieldID(env, cls, "lock", "Ljava/lang/Object;");
    printf("fid = %p\n", fid);
    lock = (*env)->GetStaticObjectField(env, cls, fid);
    printf("lock = %p\n", lock);

    if ((*env)->MonitorEnter(env, lock) != JNI_OK) {
        exit(-1);
    }

    if ((*env)->MonitorExit(env, lock) != JNI_OK) {
        if ((*env)->ExceptionOccurred(env)) {
          (*env)->ExceptionDescribe(env); // print the stack trace
        }
        exit(-1);
    }

    return (*jvm)->DestroyJavaVM(jvm) == JNI_OK ? 0 : -1;
}

