#include <jni.h>
#include <android/log.h>

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, "FVoiceCore", __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "FVoiceCore", __VA_ARGS__)

extern "C" JNIEXPORT jstring JNICALL
Java_com_fvoice_app_core_jni_NativeBridge_getBuildInfo(JNIEnv *env, jobject thiz) {
    return env->NewStringUTF("FVoice native build stub");
}
