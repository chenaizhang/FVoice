#include <jni.h>
#include <android/log.h>

#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "FVoiceDF", __VA_ARGS__)

static void throw_deepfilternet_unavailable(JNIEnv *env) {
    jclass exception_class = env->FindClass("java/lang/IllegalStateException");
    if (exception_class != nullptr) {
        env->ThrowNew(
            exception_class,
            "DeepFilterNet native library is not linked. Build libDF for this ABI and place it under app/src/main/cpp/deepfilternet/<abi>/libdf.a or libdf.so."
        );
    }
}

extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_fvoice_app_core_jni_DeepFilterNetJni_isNativeEngineAvailable(JNIEnv *env, jobject thiz) {
    return JNI_FALSE;
}

JNIEXPORT jlong JNICALL
Java_com_fvoice_app_core_jni_DeepFilterNetJni_init(JNIEnv *env, jobject thiz, jstring model_path) {
    LOGE("DeepFilterNet init failed: libDF is not linked");
    throw_deepfilternet_unavailable(env);
    return 0;
}

JNIEXPORT void JNICALL
Java_com_fvoice_app_core_jni_DeepFilterNetJni_free(JNIEnv *env, jobject thiz, jlong ctx) {
}

JNIEXPORT jbyteArray JNICALL
Java_com_fvoice_app_core_jni_DeepFilterNetJni_process(
    JNIEnv *env,
    jobject thiz,
    jlong ctx,
    jbyteArray pcm_in,
    jint sample_rate
) {
    LOGE("DeepFilterNet process failed: libDF is not linked");
    throw_deepfilternet_unavailable(env);
    return nullptr;
}

} // extern "C"
