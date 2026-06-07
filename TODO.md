# Clarivo 待实现清单

> 基于代码实际验证，仅列出确认未实现的项目。

---

## P1 — 核心功能不完整

### 实时处理
- [ ] **实时降噪** — 录音仅保存 WAV 后走离线处理链路，未实现流式 PCM 降噪（`RecordingSession.kt`）
- [ ] **实时转写** — `TranscriptionProcessor.kt:38` VAD 分段后仍将整个文件传入 ASR，未按 segment 切片 PCM；无流式 ASR 路径

### 大文件处理
- [ ] **大文件警告弹窗** — `ProcessTaskManager.kt:176` 仅 TODO 注释，4GB/2h 阈值弹窗未实现
- [ ] **分片处理** — `ProcessTaskManager.kt:177` 仅 TODO 注释，长音频整段处理，未内部分片

### 模型管理
- [ ] **模型完整性校验** — `ModelVerifier.kt:23` 仅检查文件存在+大小>0，缺少哈希/MD5/SHA256、格式魔术字节校验
- [ ] **模型版本管理** — `ModelManager.kt` 中 `version` 仅为静态字符串元数据，无版本对比/更新检查逻辑

### 异常恢复
- [ ] **细粒度断点恢复** — `ProcessTask.kt:25` `completedStages` 字段已定义但全代码库无任何读写，状态机未按阶段重写

---

## P2 — 功能增强缺失

### 导出
- [ ] **视频回封装** — `MediaExportManager.kt:17` 仅 TODO 注释，降噪后音频写回原视频容器需 ffmpeg
- [ ] **MP4/M4A 导出** — `MediaExportManager.kt:18` 仅 TODO 注释，当前仅支持 WAV 格式
- [ ] **输出路径自定义** — `MediaExportManager.kt:19` 仅 TODO 注释，固定导出到 Downloads/Clarivo，不支持 SAF 选择

---

## P3 — 工程化

### 测试
- [ ] **单元测试** — `ExampleUnitTest.kt` 仅含 `2+2=4`，TODO 注释列出待测项（TranscriptExporter / ProcessStateStore / JSON 解析 / 日志轮转）但无实现
- [ ] **基准测试** — 未测量各模型在目标设备上的推理速度/内存占用

---

*最后更新：2026-06-07*
