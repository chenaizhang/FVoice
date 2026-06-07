<div align="center">

<img alt="LOGO" src="logo.svg" width="128" height="128" />

# 清声 / FVoice

**在 Android 设备上本地完成音频降噪与语音转写**

清理噪声，保留人声。所有处理默认在设备端完成，无需上传音频到云端。

[![GitHub Release](https://img.shields.io/github/v/release/chenaizhang/FVoice?style=flat&label=Latest)](https://github.com/chenaizhang/FVoice/releases/latest)
[![License](https://img.shields.io/github/license/chenaizhang/FVoice?style=flat)](LICENSE)
[![GitHub Stars](https://img.shields.io/github/stars/chenaizhang/FVoice?style=flat)](https://github.com/chenaizhang/FVoice/stargazers)

[下载最新版](https://github.com/chenaizhang/FVoice/releases/latest) · [问题反馈](https://github.com/chenaizhang/FVoice/issues)

**中文** | **[English](README_EN.md)**

</div>

---

> 支持音频/视频降噪，生成文字稿、字幕与可分享的导出文件。默认本地优先。

## 特性

| 特性 | 说明 |
|---|---|
| **音频降噪** | 支持 MP3、WAV、M4A、FLAC、OGG 格式，本地模型降噪 |
| **视频音轨降噪** | 从 MP4、MKV、AVI、MOV 提取音轨并降噪，可选导出降噪音频 |
| **语音转写** | 输出 TXT、SRT、VTT 及结构化 JSON，保留完整时间轴 |
| **VAD 智能切分** | 基于 Silero VAD 自动检测语音区间，优化长音频转写效果 |
| **实时录音** | 麦克风实时录制并自动保存为 WAV，完成后进入离线处理队列 |
| **模型管理** | 随包分发 Whisper / DeepFilterNet 模型，支持扫描本地模型文件并切换 |
| **任务队列** | 单任务串行处理，前台服务保活，支持取消与异常恢复 |
| **双主题** | Miuix 与 Material3 双风格，悬浮底栏、液态玻璃、预测性返回，支持 Monet 取色与深度自定义 |

## 运行要求

| 项目 | 要求 |
|---|---|
| 系统版本 | Android 8.0+（API 26） |
| 设备架构 | arm64-v8a 或 x86_64 |

## 文档

| 文档 | 说明 |
|---|---|
| [构建指南](docs/BUILDING.md) | 从源码构建 APK |
| [第三方代码声明](docs/THIRD_PARTY_NOTICES.md) | 引用的开源组件及许可证 |

## 参与贡献

详见 [CONTRIBUTING.md](CONTRIBUTING.md)。

如果觉得项目有用，欢迎点一个 Star 让更多人看到！

## 致谢

- [Miuix](https://github.com/miuix-krteam/miuix) — 精美的 Compose UI 组件库
- [whisper.cpp](https://github.com/ggerganov/whisper.cpp) — 高性能端侧语音识别
- [DeepFilterNet](https://github.com/Rikorose/DeepFilterNet) — 实时语音增强
- [Silero VAD](https://github.com/snakers4/silero-vad) — 轻量级语音活动检测
- [ONNX Runtime](https://github.com/microsoft/onnxruntime) — 跨平台机器学习推理

## 许可证

本项目以 [Apache-2.0](LICENSE) 许可证发布。第三方代码保留其原始许可证。
