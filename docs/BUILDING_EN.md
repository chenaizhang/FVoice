# Build Guide

## Requirements

| Item | Version |
|---|---|
| Android Studio | Ladybug (2024.2.1) or newer |
| JDK | 17 |
| Android SDK | API 37 (compileSdk) |
| Gradle | 9.4.1 (distributed with project Wrapper) |
| CMake | 3.22.1+ (for native engine compilation) |
| Android NDK | 27.0.12077973+ (for whisper.cpp / DeepFilterNet compilation) |

## Quick Start

```bash
# 1. Clone the repository
git clone https://github.com/chenaizhang/FVoice.git
cd FVoice

# 2. Build Debug APK with Gradle Wrapper
./gradlew :app:assembleDebug

# 3. Install on a connected device
./gradlew :app:installDebug
```

Build output: `app/build/outputs/apk/debug/app-debug.apk`

## Full Build Workflow

### 1. Import the Project

Open the root `build.gradle.kts` with Android Studio. The IDE will sync Gradle configuration automatically.

### 2. Sync Dependencies

On first open, Android Studio downloads all dependencies automatically. If sync fails:

```bash
./gradlew --refresh-dependencies
```

### 3. Enable Native Engine Compilation (Optional)

By default, native compilation is commented out. To enable whisper.cpp / DeepFilterNet:

**Step A: Install CMake and NDK**

Via Android Studio SDK Manager:
- SDK Tools → CMake 3.22.1+
- SDK Tools → NDK 27.0.12077973+

**Step B: Download Engine Source**

```bash
cd app/src/main/cpp

# Download whisper.cpp
git clone --depth 1 https://github.com/ggerganov/whisper.cpp.git

# Download DeepFilterNet (requires prepared C API wrapper)
# git clone --depth 1 https://github.com/Rikorose/DeepFilterNet.git
```

**Step C: Uncomment Native Build Configuration**

In `app/build.gradle.kts`, uncomment:

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

**Step D: Sync and Build**

```bash
./gradlew :app:assembleDebug
```


### 4. Run Tests

```bash
# Run unit tests
./gradlew :app:testDebugUnitTest

# Run instrumented tests (requires connected device or emulator)
./gradlew :app:connectedDebugAndroidTest
```

## FAQ

### Q: Build fails with "CMake not found"

A: Install CMake 3.22.1+ via Android Studio SDK Manager, or set the `ANDROID_HOME` environment variable to point to the correct SDK path.

### Q: Native compilation fails — whisper.cpp header not found

A: Verify that `app/src/main/cpp/whisper.cpp/whisper.h` exists. If not, CMake will automatically compile the stub version without errors, but inference will fall back to FakeEngine.

### Q: ONNX Runtime dependency download fails

A: Check your network connection, or try adding a mirror repository in `settings.gradle.kts`.

### Q: Build succeeds but app crashes on launch

A: Check logs for `FVoiceException` or `UnsatisfiedLinkError`. If the native library is not loaded, the engine automatically falls back to Fake mode and should not crash. If a crash occurs, please file an Issue with the full log.

## Release Build

```bash
./gradlew :app:assembleRelease
```

The Release APK is at `app/build/outputs/apk/release/app-release-unsigned.apk`. It must be signed with a keystore before distribution.
