# Third-Party Notices

This project uses the following open-source components and their dependencies. Each component retains its original license. This document serves as a summary notice.

## Core Dependencies

| Component | License | Purpose |
|---|---|---|
| [AndroidX / Jetpack](https://developer.android.com/jetpack/androidx/releases) | Apache-2.0 | Official Android extension libraries (Core, Compose, Lifecycle, Navigation, etc.) |
| [Kotlin](https://github.com/JetBrains/kotlin) | Apache-2.0 | Programming language |
| [Kotlinx Serialization](https://github.com/Kotlin/kotlinx.serialization) | Apache-2.0 | JSON serialization |
| [Jetpack Compose](https://developer.android.com/jetpack/compose) | Apache-2.0 | Declarative UI framework |

## UI Components

| Component | License | Purpose |
|---|---|---|
| [Miuix](https://github.com/miuix-krteam/miuix) | Apache-2.0 | Xiaomi-style Compose UI component library |
| [MaterialKolor](https://github.com/jordond/MaterialKolor) | MIT | Material You dynamic color extraction |
| [Navigation3](https://developer.android.com/guide/navigation/design) | Apache-2.0 | Type-safe navigation framework |

## AI / ML Engines

| Component | License | Purpose |
|---|---|---|
| [whisper.cpp](https://github.com/ggerganov/whisper.cpp) | MIT | On-device speech recognition (ASR) engine |
| [OpenAI Whisper Model](https://github.com/openai/whisper) | MIT | Pre-trained speech-to-text model (ggml format) |
| [DeepFilterNet](https://github.com/Rikorose/DeepFilterNet) | MIT / Apache-2.0 | Real-time speech denoising and enhancement |
| [Silero VAD](https://github.com/snakers4/silero-vad) | AGPL-3.0 | Voice activity detection model (ONNX format) |
| [ONNX Runtime](https://github.com/microsoft/onnxruntime) | MIT | Cross-platform machine learning inference runtime |

## Utility Libraries

| Component | License | Purpose |
|---|---|---|
| [HiddenApiBypass](https://github.com/LSPosed/HiddenApiBypass) | Apache-2.0 | Bypass Android hidden API restrictions |

## License Texts

Full license texts are available at:

- **Apache-2.0**: https://www.apache.org/licenses/LICENSE-2.0
- **MIT**: https://opensource.org/licenses/MIT
- **AGPL-3.0**: https://www.gnu.org/licenses/agpl-3.0.html

## Notes

- The main build pipeline **contains no GPL dependencies**.
- If FFmpeg is introduced, it should only be an **optional module**, with clear notice to users regarding its GPL license implications.
- The Silero VAD ONNX model file is under the AGPL-3.0 license. Users who download and import this model are responsible for assessing their own compliance.
