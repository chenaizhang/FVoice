#include <jni.h>
#include <android/log.h>
#include <algorithm>
#include <cmath>
#include <cstdint>
#include <cstring>
#include <vector>

#include "rnnoise.h"

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, "FVoiceRNNoise", __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, "FVoiceRNNoise", __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "FVoiceRNNoise", __VA_ARGS__)

static void throw_invalid_audio_format(JNIEnv *env) {
    jclass exception_class = env->FindClass("java/lang/IllegalArgumentException");
    if (exception_class != nullptr) {
        env->ThrowNew(
            exception_class,
            "RNNoise only supports 16-bit PCM/WAV audio with at least one channel."
        );
    }
}

struct RnNoiseState {
    DenoiseState *rnnoise = nullptr;
};

struct WavInfo {
    int sample_rate = 16000;
    int channels = 1;
    int bits_per_sample = 16;
    int data_offset = 0;
    int data_size = 0;
};

static uint16_t read_u16_le(const std::vector<jbyte> &bytes, size_t offset) {
    return static_cast<uint16_t>(static_cast<uint8_t>(bytes[offset]) |
                                 (static_cast<uint8_t>(bytes[offset + 1]) << 8));
}

static uint32_t read_u32_le(const std::vector<jbyte> &bytes, size_t offset) {
    return static_cast<uint32_t>(static_cast<uint8_t>(bytes[offset]) |
                                 (static_cast<uint8_t>(bytes[offset + 1]) << 8) |
                                 (static_cast<uint8_t>(bytes[offset + 2]) << 16) |
                                 (static_cast<uint8_t>(bytes[offset + 3]) << 24));
}

static WavInfo parse_wav(const std::vector<jbyte> &bytes, int fallback_sample_rate) {
    WavInfo info;
    info.sample_rate = fallback_sample_rate > 0 ? fallback_sample_rate : 16000;
    info.data_size = static_cast<int>(bytes.size());

    if (bytes.size() < 44 ||
        std::memcmp(bytes.data(), "RIFF", 4) != 0 ||
        std::memcmp(bytes.data() + 8, "WAVE", 4) != 0) {
        return info;
    }

    size_t offset = 12;
    while (offset + 8 <= bytes.size()) {
        const char *chunk_id = reinterpret_cast<const char *>(bytes.data() + offset);
        uint32_t chunk_size = read_u32_le(bytes, offset + 4);
        size_t payload = offset + 8;
        if (payload + chunk_size > bytes.size()) break;

        if (std::memcmp(chunk_id, "fmt ", 4) == 0 && chunk_size >= 16) {
            info.channels = std::max(1, static_cast<int>(read_u16_le(bytes, payload + 2)));
            info.sample_rate = std::max(1, static_cast<int>(read_u32_le(bytes, payload + 4)));
            info.bits_per_sample = static_cast<int>(read_u16_le(bytes, payload + 14));
        } else if (std::memcmp(chunk_id, "data", 4) == 0) {
            info.data_offset = static_cast<int>(payload);
            info.data_size = static_cast<int>(chunk_size);
        }

        offset = payload + chunk_size + (chunk_size % 2);
    }

    return info;
}

static std::vector<float> pcm16_to_mono_float(
    const std::vector<jbyte> &bytes,
    const WavInfo &info
) {
    int data_end = std::min(static_cast<int>(bytes.size()), info.data_offset + info.data_size);
    int frame_count = (data_end - info.data_offset) / 2 / info.channels;
    std::vector<float> mono(std::max(0, frame_count));
    int index = info.data_offset;

    for (int frame = 0; frame < frame_count; ++frame) {
        float sum = 0.0f;
        for (int ch = 0; ch < info.channels; ++ch) {
            int16_t sample = static_cast<int16_t>(
                (static_cast<uint8_t>(bytes[index + 1]) << 8) |
                static_cast<uint8_t>(bytes[index])
            );
            sum += static_cast<float>(sample);
            index += 2;
        }
        mono[frame] = sum / static_cast<float>(info.channels);
    }
    return mono;
}

static std::vector<float> resample_linear(
    const std::vector<float> &input,
    int source_rate,
    int target_rate
) {
    if (input.empty() || source_rate <= 0 || target_rate <= 0 || source_rate == target_rate) {
        return input;
    }

    size_t output_size = std::max<size_t>(
        1,
        static_cast<size_t>((static_cast<double>(input.size()) * target_rate) / source_rate)
    );
    std::vector<float> output(output_size);
    double ratio = static_cast<double>(source_rate) / static_cast<double>(target_rate);

    for (size_t i = 0; i < output.size(); ++i) {
        double source_position = static_cast<double>(i) * ratio;
        size_t left = std::min(static_cast<size_t>(source_position), input.size() - 1);
        size_t right = std::min(left + 1, input.size() - 1);
        float fraction = static_cast<float>(source_position - static_cast<double>(left));
        output[i] = input[left] + (input[right] - input[left]) * fraction;
    }

    return output;
}

static std::vector<float> apply_rnnoise(DenoiseState *state, const std::vector<float> &samples48k) {
    const int frame_size = rnnoise_get_frame_size();
    std::vector<float> output(samples48k.size(), 0.0f);
    std::vector<float> frame_in(frame_size, 0.0f);
    std::vector<float> frame_out(frame_size, 0.0f);

    for (size_t pos = 0; pos < samples48k.size(); pos += frame_size) {
        std::fill(frame_in.begin(), frame_in.end(), 0.0f);
        size_t count = std::min(static_cast<size_t>(frame_size), samples48k.size() - pos);
        std::copy(samples48k.begin() + pos, samples48k.begin() + pos + count, frame_in.begin());

        rnnoise_process_frame(state, frame_out.data(), frame_in.data());
        std::copy(frame_out.begin(), frame_out.begin() + count, output.begin() + pos);
    }

    return output;
}

static void write_mono_to_pcm16(std::vector<jbyte> &bytes, const WavInfo &info, const std::vector<float> &mono) {
    int data_end = std::min(static_cast<int>(bytes.size()), info.data_offset + info.data_size);
    int frame_count = std::min(static_cast<int>(mono.size()), (data_end - info.data_offset) / 2 / info.channels);
    int index = info.data_offset;

    for (int frame = 0; frame < frame_count; ++frame) {
        float clamped = std::max(-32768.0f, std::min(32767.0f, mono[frame]));
        int16_t sample = static_cast<int16_t>(std::lrintf(clamped));
        for (int ch = 0; ch < info.channels; ++ch) {
            bytes[index] = static_cast<jbyte>(sample & 0xFF);
            bytes[index + 1] = static_cast<jbyte>((sample >> 8) & 0xFF);
            index += 2;
        }
    }
}

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_fvoice_app_core_jni_RnNoiseJni_init(JNIEnv *env, jobject thiz) {
    auto *state = new RnNoiseState();
    state->rnnoise = rnnoise_create(nullptr);
    if (!state->rnnoise) {
        delete state;
        LOGE("Failed to create RNNoise state");
        return 0;
    }
    LOGI("RNNoise initialized");
    return reinterpret_cast<jlong>(state);
}

JNIEXPORT void JNICALL
Java_com_fvoice_app_core_jni_RnNoiseJni_free(JNIEnv *env, jobject thiz, jlong ctx) {
    if (ctx == 0) return;
    auto *state = reinterpret_cast<RnNoiseState *>(ctx);
    if (state->rnnoise) {
        rnnoise_destroy(state->rnnoise);
    }
    delete state;
    LOGI("RNNoise freed");
}

JNIEXPORT jbyteArray JNICALL
Java_com_fvoice_app_core_jni_RnNoiseJni_process(
    JNIEnv *env,
    jobject thiz,
    jlong ctx,
    jbyteArray pcm_in,
    jint sample_rate
) {
    if (ctx == 0) {
        LOGE("Invalid RNNoise context");
        return nullptr;
    }

    jsize len = env->GetArrayLength(pcm_in);
    jbyte *input = env->GetByteArrayElements(pcm_in, nullptr);
    if (!input) return nullptr;

    std::vector<jbyte> bytes(input, input + len);
    env->ReleaseByteArrayElements(pcm_in, input, JNI_ABORT);

    WavInfo info = parse_wav(bytes, sample_rate);
    if (info.bits_per_sample != 16 || info.channels < 1 || info.data_size <= 0) {
        LOGE("Unsupported audio format");
        throw_invalid_audio_format(env);
        return nullptr;
    } else {
        auto *state = reinterpret_cast<RnNoiseState *>(ctx);
        std::vector<float> mono = pcm16_to_mono_float(bytes, info);
        std::vector<float> samples48k = resample_linear(mono, info.sample_rate, 48000);
        std::vector<float> denoised48k = apply_rnnoise(state->rnnoise, samples48k);
        std::vector<float> denoised = resample_linear(denoised48k, 48000, info.sample_rate);
        write_mono_to_pcm16(bytes, info, denoised);
        LOGI("RNNoise processed %zu samples at %d Hz", mono.size(), info.sample_rate);
    }

    jbyteArray out = env->NewByteArray(static_cast<jsize>(bytes.size()));
    env->SetByteArrayRegion(out, 0, static_cast<jsize>(bytes.size()), bytes.data());
    return out;
}

} // extern "C"
