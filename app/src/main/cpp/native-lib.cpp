#include <jni.h>
#include <string>
#include <android/log.h>


#define LOGE(FORMAT, ...) __android_log_print(ANDROID_LOG_ERROR,"xxx",FORMAT,##__VA_ARGS__);

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_mediacode01_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}