#include <jni.h>
#include <android/log.h>
#include <cstdio>
#include <string>
#include <vector>
#include "whisper.h"

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, "FVoiceWhisper", __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "FVoiceWhisper", __VA_ARGS__)

static std::string json_escape(const char *input) {
    std::string out;
    if (!input) return out;
    for (const char *p = input; *p; ++p) {
        switch (*p) {
            case '\\': out += "\\\\"; break;
            case '"': out += "\\\""; break;
            case '\b': out += "\\b"; break;
            case '\f': out += "\\f"; break;
            case '\n': out += "\\n"; break;
            case '\r': out += "\\r"; break;
            case '\t': out += "\\t"; break;
            default:
                if (static_cast<unsigned char>(*p) < 0x20) {
                    char buf[7];
                    snprintf(buf, sizeof(buf), "\\u%04x", *p);
                    out += buf;
                } else {
                    out += *p;
                }
        }
    }
    return out;
}

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_fvoice_app_core_jni_WhisperCppJni_init(JNIEnv *env, jobject thiz, jstring model_path) {
    const char *path = env->GetStringUTFChars(model_path, nullptr);
    if (!path) {
        LOGE("Model path is null");
        return 0;
    }

    struct whisper_context_params cparams = whisper_context_default_params();
    cparams.use_gpu = false; // Use CPU for broader compatibility

    struct whisper_context *ctx = whisper_init_from_file_with_params(path, cparams);
    env->ReleaseStringUTFChars(model_path, path);

    if (!ctx) {
        LOGE("Failed to load whisper model");
        return 0;
    }

    LOGI("Whisper model loaded successfully");
    return reinterpret_cast<jlong>(ctx);
}

JNIEXPORT void JNICALL
Java_com_fvoice_app_core_jni_WhisperCppJni_free(JNIEnv *env, jobject thiz, jlong ctx_ptr) {
    if (ctx_ptr == 0) return;
    struct whisper_context *ctx = reinterpret_cast<struct whisper_context *>(ctx_ptr);
    whisper_free(ctx);
    LOGI("Whisper context freed");
}

JNIEXPORT jstring JNICALL
Java_com_fvoice_app_core_jni_WhisperCppJni_transcribe(
    JNIEnv *env,
    jobject thiz,
    jlong ctx_ptr,
    jfloatArray samples,
    jint n_samples,
    jstring language
) {
    if (ctx_ptr == 0) {
        return env->NewStringUTF("{\"error\":\"invalid context\"}");
    }

    struct whisper_context *ctx = reinterpret_cast<struct whisper_context *>(ctx_ptr);
    jfloat *sample_data = env->GetFloatArrayElements(samples, nullptr);
    if (!sample_data) {
        return env->NewStringUTF("{\"error\":\"failed to get samples\"}");
    }

    const char *lang = env->GetStringUTFChars(language, nullptr);
    std::string lang_str = lang ? lang : "auto";
    if (lang) env->ReleaseStringUTFChars(language, lang);

    struct whisper_full_params wparams = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    wparams.translate = false;
    wparams.language = lang_str == "auto" ? nullptr : lang_str.c_str();
    wparams.n_threads = 4;
    wparams.print_progress = false;
    wparams.print_special = false;
    wparams.print_realtime = false;
    wparams.print_timestamps = false;

    int ret = whisper_full(ctx, wparams, sample_data, n_samples);
    env->ReleaseFloatArrayElements(samples, sample_data, 0);

    if (ret != 0) {
        LOGE("Whisper full failed: %d", ret);
        return env->NewStringUTF("{\"error\":\"transcription failed\"}");
    }

    // Build JSON result
    int n_segments = whisper_full_n_segments(ctx);
    std::string json = "{\"segments\":[";
    for (int i = 0; i < n_segments; i++) {
        int64_t t0 = whisper_full_get_segment_t0(ctx, i);
        int64_t t1 = whisper_full_get_segment_t1(ctx, i);
        const char *text = whisper_full_get_segment_text(ctx, i);
        if (i > 0) json += ",";
        json += "{";
        json += "\"start\":" + std::to_string(t0 / 100.0) + ",";
        json += "\"end\":" + std::to_string(t1 / 100.0) + ",";
        json += "\"text\":\"" + json_escape(text) + "\"";
        json += "}";
    }
    json += "]}";

    return env->NewStringUTF(json.c_str());
}

} // extern "C"
