#include <jni.h>
#include <string>

JNIEXPORT jstring JNICALL
Java_cau_cse_capstone_blindaid_Camera_MainActivity_stringFromJNI(JNIEnv *env, jobject instance) {

    // TODO


    const char *returnValue;
    return env->NewStringUTF(returnValue);
}

extern "C"
JNIEXPORT jstring JNICALL
Java_cau_cse_capstone_blindaid_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}
