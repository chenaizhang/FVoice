# 构建指南

## 环境要求

| 项目 | 版本要求 |
|---|---|
| Android Studio | Ladybug (2024.2.1) 或更新版本 |
| JDK | 17 |
| Android SDK | API 37 (compileSdk) |
| Gradle | 9.4.1（随项目 Wrapper 分发） |
| CMake | 3.22.1+（如需编译原生引擎） |
| Android NDK | 27.0.12077973+（如需编译 whisper.cpp / DeepFilterNet） |

## 快速开始

```bash
# 1. 克隆仓库
git clone https://github.com/chenaizhang/FVoice.git
cd FVoice

# 2. 使用 Gradle Wrapper 构建 Debug APK
./gradlew :app:assembleDebug

# 3. 安装到已连接设备
./gradlew :app:installDebug
```

构建产物路径：`app/build/outputs/apk/debug/app-debug.apk`

## 完整构建流程

### 1. 导入项目

使用 Android Studio 打开项目根目录的 `build.gradle.kts`，IDE 会自动同步 Gradle 配置。

### 2. 同步依赖

首次打开时，Android Studio 会自动下载所有依赖。如果遇到同步失败：

```bash
./gradlew --refresh-dependencies
```

### 3. 启用原生引擎编译（可选）

默认配置下，native 编译已被注释。如需启用 whisper.cpp / DeepFilterNet：

**步骤 A：安装 CMake 和 NDK**

通过 Android Studio SDK Manager：
- SDK Tools → CMake 3.22.1+
- SDK Tools → NDK 27.0.12077973+

**步骤 B：下载引擎源码**

```bash
cd app/src/main/cpp

# 下载 whisper.cpp
git clone --depth 1 https://github.com/ggerganov/whisper.cpp.git

# 下载 DeepFilterNet（需自行准备 C API 封装）
# git clone --depth 1 https://github.com/Rikorose/DeepFilterNet.git
```

**步骤 C：取消注释 native 编译配置**

编辑 `app/build.gradle.kts`，取消以下注释：

```kotlin
externalNativeBuild {
    cmake {
        path = file("src/main/cpp/CMakeLists.txt")
    }
}
defaultConfig {
    externalNativeBuild {
        cmake {
            cppFlags += "-std=c++17 -O3 -ffast-math"
            arguments += "-DANDROID_STL=c++_shared"
        }
    }
    ndk {
        abiFilters += listOf("arm64-v8a", "x86_64")
    }
}
```

**步骤 D：同步并构建**

```bash
./gradlew :app:assembleDebug
```


### 4. 运行测试

```bash
# 运行单元测试
./gradlew :app:testDebugUnitTest

# 运行 Android 仪器测试（需连接设备或模拟器）
./gradlew :app:connectedDebugAndroidTest
```

## 常见问题

### Q: 构建时提示 "CMake not found"

A: 确保通过 Android Studio SDK Manager 安装了 CMake 3.22.1+，或设置环境变量 `ANDROID_HOME` 指向正确的 SDK 路径。

### Q: 原生编译失败，whisper.cpp 头文件找不到

A: 确认 `app/src/main/cpp/whisper.cpp/whisper.h` 存在。若不存在，CMake 会自动编译 stub 版本，不会报错，但推理会降级到 FakeEngine。

### Q: ONNX Runtime 依赖下载失败

A: 检查网络连接，或尝试在 `settings.gradle.kts` 中添加国内镜像源。

### Q: 构建成功但运行时闪退

A: 检查日志中的 `FVoiceException` 或 `UnsatisfiedLinkError`。若 native 库未加载，引擎会自动 fallback 到 Fake 模式，不应闪退。如有闪退，请提交 Issue 并附上完整日志。

## 发布构建

```bash
./gradlew :app:assembleRelease
```

Release APK 位于 `app/build/outputs/apk/release/app-release-unsigned.apk`，需使用密钥库签名后方可分发。
