#include <jni.h>
#include <android/log.h>
#include <algorithm>
#include <cstdint>
#include <cstring>
#include <vector>

#include "deepfilternet/libdeep_filter.h"

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, "ClarivoDF", __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "ClarivoDF", __VA_ARGS__)

extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_clarivo_app_core_jni_DeepFilterNetJni_isNativeEngineAvailable(JNIEnv *env, jobject thiz) {
    return JNI_TRUE;
}

JNIEXPORT jlong JNICALL
Java_com_clarivo_app_core_jni_DeepFilterNetJni_init(JNIEnv *env, jobject thiz, jstring model_path) {
    const char *path = env->GetStringUTFChars(model_path, nullptr);
    if (!path) {
        LOGE("Model path is null");
        return 0;
    }

    DFState *state = df_create(path, 80.0f, "warn");
    env->ReleaseStringUTFChars(model_path, path);

    if (!state) {
        LOGE("Failed to create DeepFilterNet state");
        return 0;
    }
    df_set_post_filter_beta(state, 0.02f);

    LOGI("DeepFilterNet initialized");
    return reinterpret_cast<jlong>(state);
}

JNIEXPORT void JNICALL
Java_com_clarivo_app_core_jni_DeepFilterNetJni_free(JNIEnv *env, jobject thiz, jlong ctx) {
    if (ctx == 0) return;
    DFState *state = reinterpret_cast<DFState*>(ctx);
    df_free(state);
    LOGI("DeepFilterNet freed");
}

static int find_wav_data_offset(const std::vector<jbyte> &bytes, int *data_size) {
    if (bytes.size() < 44 ||
        std::memcmp(bytes.data(), "RIFF", 4) != 0 ||
        std::memcmp(bytes.data() + 8, "WAVE", 4) != 0) {
        *data_size = static_cast<int>(bytes.size());
        return 0;
    }

    size_t offset = 12;
    while (offset + 8 <= bytes.size()) {
        const char *chunk_id = reinterpret_cast<const char *>(bytes.data() + offset);
        uint32_t chunk_size = static_cast<uint8_t>(bytes[offset + 4]) |
                              (static_cast<uint8_t>(bytes[offset + 5]) << 8) |
                              (static_cast<uint8_t>(bytes[offset + 6]) << 16) |
                              (static_cast<uint8_t>(bytes[offset + 7]) << 24);
        size_t payload = offset + 8;
        if (payload + chunk_size > bytes.size()) break;
        if (std::memcmp(chunk_id, "data", 4) == 0) {
            *data_size = static_cast<int>(chunk_size);
            return static_cast<int>(payload);
        }
        offset = payload + chunk_size + (chunk_size % 2);
    }

    *data_size = static_cast<int>(bytes.size());
    return 0;
}

JNIEXPORT jbyteArray JNICALL
Java_com_clarivo_app_core_jni_DeepFilterNetJni_process(
    JNIEnv *env,
    jobject thiz,
    jlong ctx,
    jbyteArray pcm_in,
    jint sample_rate
) {
    if (ctx == 0) {
        LOGE("Invalid DeepFilterNet context");
        return nullptr;
    }

    DFState *state = reinterpret_cast<DFState*>(ctx);
    jsize len = env->GetArrayLength(pcm_in);
    jbyte *input = env->GetByteArrayElements(pcm_in, nullptr);
    if (!input) return nullptr;

    std::vector<jbyte> bytes(input, input + len);
    env->ReleaseByteArrayElements(pcm_in, input, JNI_ABORT);

    int data_size = 0;
    int data_offset = find_wav_data_offset(bytes, &data_size);
    int data_end = std::min(static_cast<int>(bytes.size()), data_offset + data_size);
    if (data_offset < 0 || data_end - data_offset < 2) {
        LOGE("Invalid PCM/WAV input");
        return nullptr;
    }

    int pcm_len = data_end - data_offset;
    int num_samples = pcm_len / 2;
    std::vector<float> input_float(num_samples);
    for (int i = 0; i < num_samples; i++) {
        int byte_index = data_offset + i * 2;
        int16_t sample = static_cast<int16_t>(
            (static_cast<uint8_t>(bytes[byte_index + 1]) << 8) |
            static_cast<uint8_t>(bytes[byte_index])
        );
        input_float[i] = sample / 32768.0f;
    }

    int frame_len = static_cast<int>(df_get_frame_length(state));
    if (frame_len <= 0) {
        LOGE("Invalid DeepFilterNet frame length");
        return nullptr;
    }

    int tail_pad = frame_len * 2;
    int delay = frame_len;
    std::vector<float> processing_float(num_samples + tail_pad, 0.0f);
    std::copy(input_float.begin(), input_float.end(), processing_float.begin());
    std::vector<float> processing_output(processing_float.size(), 0.0f);
    std::vector<float> frame_in(frame_len);
    std::vector<float> frame_out(frame_len);

    for (size_t pos = 0; pos < processing_float.size(); pos += frame_len) {
        std::fill(frame_in.begin(), frame_in.end(), 0.0f);
        size_t count = std::min(static_cast<size_t>(frame_len), processing_float.size() - pos);
        std::memcpy(frame_in.data(), &processing_float[pos], count * sizeof(float));
        df_process_frame(state, frame_in.data(), frame_out.data());
        std::memcpy(&processing_output[pos], frame_out.data(), count * sizeof(float));
    }

    if (delay + num_samples > static_cast<int>(processing_output.size())) {
        LOGE("DeepFilterNet output too short after delay compensation");
        return nullptr;
    }
    std::vector<float> output_float(num_samples);
    std::copy(
        processing_output.begin() + delay,
        processing_output.begin() + delay + num_samples,
        output_float.begin()
    );
    LOGI(
        "DeepFilterNet processed with official libDF: samples=%d frame=%d tailPad=%d delay=%d",
        num_samples,
        frame_len,
        tail_pad,
        delay
    );

    for (int i = 0; i < num_samples; i++) {
        float clamped = std::max(-1.0f, std::min(1.0f, output_float[i]));
        // Use 32768.0f for symmetric scaling so -1.0 maps exactly to -32768.
        int32_t sample32 = static_cast<int32_t>(clamped * 32768.0f);
        int16_t sample = static_cast<int16_t>(std::max(-32768, std::min(32767, sample32)));
        int byte_index = data_offset + i * 2;
        bytes[byte_index] = static_cast<jbyte>(sample & 0xFF);
        bytes[byte_index + 1] = static_cast<jbyte>((sample >> 8) & 0xFF);
    }

    jbyteArray out = env->NewByteArray(static_cast<jsize>(bytes.size()));
    env->SetByteArrayRegion(out, 0, static_cast<jsize>(bytes.size()), bytes.data());
    return out;
}

} // extern "C"
