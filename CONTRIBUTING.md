# 参与贡献 FVoice

感谢你对 FVoice 的关注！欢迎提交 Pull Request、Bug 报告和功能建议。

## 开发环境

1. **克隆仓库**
   ```bash
   git clone https://github.com/chenaizhang/FVoice.git
   cd FVoice
   ```

2. **在 Android Studio 中打开**
   - 使用 Android Studio Koala 或更新版本
   - 需要 JDK 17

3. **构建项目**
   ```bash
   ./gradlew :app:assembleDebug
   ```

## 代码规范

- 遵循项目现有的 Kotlin 编码约定
- 使用有意义的变量名和函数名
- 公共 API 添加 KDoc 注释
- Composable 函数保持专注和可复用

## 提交信息

请遵循 [Conventional Commits](https://www.conventionalcommits.org/) 规范：

- `feat:` — 新功能
- `fix:` — Bug 修复
- `docs:` — 文档变更
- `style:` — 代码风格调整（格式化、空格等）
- `refactor:` — 既不修 Bug 也不加功能的代码重构
- `perf:` — 性能优化
- `test:` — 添加或修正测试
- `chore:` — 构建流程或辅助工具变更

示例：
```
feat: 添加实时录音波形可视化
```

## Pull Request 流程

1. Fork 本仓库并从 `main` 分支创建你的分支
2. 确保代码能编译并通过基本检查
3. 必要时更新文档
4. 提交 Pull Request 并附上清晰的变更说明

## 原生引擎开发

如果你在开发 whisper.cpp 或 DeepFilterNet 集成：

1. 将第三方源码下载到 `app/src/main/cpp/`
2. 取消 `app/build.gradle.kts` 中 `externalNativeBuild` 的注释
3. 确安装 CMake 3.22.1+ 和 NDK
4. 参见 [构建指南](docs/BUILDING.md) 中的原生引擎编译章节

## 报告 Bug

请包含以下信息：
- 设备型号和 Android 版本
- 复现步骤
- 预期行为 vs 实际行为
- 相关日志（如有）
