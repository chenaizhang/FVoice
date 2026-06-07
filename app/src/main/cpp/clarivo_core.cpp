#include <jni.h>
#include <android/log.h>

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, "ClarivoCore", __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "ClarivoCore", __VA_ARGS__)

extern "C" JNIEXPORT jstring JNICALL
Java_com_clarivo_app_core_jni_NativeBridge_getBuildInfo(JNIEnv *env, jobject thiz) {
    return env->NewStringUTF("Clarivo native build stub");
}
