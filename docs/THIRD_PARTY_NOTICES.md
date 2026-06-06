# 第三方代码声明

本项目引用了以下开源组件及其依赖。各组件保留其原始许可证，此处仅作汇总声明。

## 核心依赖

| 组件 | 许可证 | 用途 |
|---|---|---|
| [AndroidX / Jetpack](https://developer.android.com/jetpack/androidx/releases) | Apache-2.0 | Android 官方扩展库（Core、Compose、Lifecycle、Navigation 等） |
| [Kotlin](https://github.com/JetBrains/kotlin) | Apache-2.0 | 编程语言 |
| [Kotlinx Serialization](https://github.com/Kotlin/kotlinx.serialization) | Apache-2.0 | JSON 序列化 |
| [Jetpack Compose](https://developer.android.com/jetpack/compose) | Apache-2.0 | 声明式 UI 框架 |

## UI 组件

| 组件 | 许可证 | 用途 |
|---|---|---|
| [Miuix](https://github.com/miuix-krteam/miuix) | Apache-2.0 | 小米风格 Compose UI 组件库 |
| [MaterialKolor](https://github.com/jordond/MaterialKolor) | MIT | Material You 动态取色 |
| [Navigation3](https://developer.android.com/guide/navigation/design) | Apache-2.0 | 类型安全导航框架 |

## AI / ML 引擎

| 组件 | 许可证 | 用途 |
|---|---|---|
| [whisper.cpp](https://github.com/ggerganov/whisper.cpp) | MIT | 端侧语音识别（ASR）引擎 |
| [OpenAI Whisper 模型](https://github.com/openai/whisper) | MIT | 预训练语音转写模型（ggml 格式） |
| [DeepFilterNet](https://github.com/Rikorose/DeepFilterNet) | MIT / Apache-2.0 | 实时语音降噪增强 |
| [Silero VAD](https://github.com/snakers4/silero-vad) | AGPL-3.0 | 语音活动检测模型（ONNX 格式） |
| [ONNX Runtime](https://github.com/microsoft/onnxruntime) | MIT | 跨平台机器学习推理运行时 |

## 工具库

| 组件 | 许可证 | 用途 |
|---|---|---|
| [HiddenApiBypass](https://github.com/LSPosed/HiddenApiBypass) | Apache-2.0 | 绕过 Android 隐藏 API 限制 |

## 许可证全文

各许可证原文可在以下位置查看：

- **Apache-2.0**: https://www.apache.org/licenses/LICENSE-2.0
- **MIT**: https://opensource.org/licenses/MIT
- **AGPL-3.0**: https://www.gnu.org/licenses/agpl-3.0.html

## 说明

- 本项目主构建链路**不包含 GPL 依赖**。
- FFmpeg 如需引入，仅作为**可选模块**，并需明确告知用户其 GPL 许可证风险。
- Silero VAD 的 ONNX 模型文件采用 AGPL-3.0 许可证，用户自行下载和导入该模型时，需自行评估合规性。
