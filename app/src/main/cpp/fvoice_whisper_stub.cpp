#include <jni.h>
#include <android/log.h>

#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "FVoiceWhisper", __VA_ARGS__)

static void throw_whisper_unavailable(JNIEnv *env) {
    jclass exception_class = env->FindClass("java/lang/IllegalStateException");
    if (exception_class != nullptr) {
        env->ThrowNew(
            exception_class,
            "whisper.cpp native library is not integrated in this build. Add whisper.cpp/include/whisper.h and rebuild the app."
        );
    }
}

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_fvoice_app_core_jni_WhisperCppJni_init(JNIEnv *env, jobject thiz, jstring model_path) {
    LOGE("WhisperCpp init failed: whisper.cpp is not integrated");
    throw_whisper_unavailable(env);
    return 0;
}

JNIEXPORT void JNICALL
Java_com_fvoice_app_core_jni_WhisperCppJni_free(JNIEnv *env, jobject thiz, jlong ctx) {
}

JNIEXPORT jstring JNICALL
Java_com_fvoice_app_core_jni_WhisperCppJni_transcribe(
    JNIEnv *env,
    jobject thiz,
    jlong ctx,
    jfloatArray samples,
    jint n_samples,
    jstring language
) {
    LOGE("WhisperCpp transcribe failed: whisper.cpp is not integrated");
    throw_whisper_unavailable(env);
    return nullptr;
}

} // extern "C"
