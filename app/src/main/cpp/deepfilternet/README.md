# DeepFilterNet native integration

This directory is prepared for the official DeepFilterNet `libDF` C API.

Expected files:

```text
app/src/main/cpp/deepfilternet/libdeep_filter.h
app/src/main/cpp/deepfilternet/arm64-v8a/libdf.a
app/src/main/cpp/deepfilternet/x86_64/libdf.a
```

Shared libraries are also accepted:

```text
app/src/main/cpp/deepfilternet/arm64-v8a/libdf.so
app/src/main/cpp/deepfilternet/x86_64/libdf.so
```

Build `libDF` from the DeepFilterNet Rust crate with the `capi` feature and Android targets. The exported API must match `libdeep_filter.h`. If no ABI-specific prebuilt library is present, the app builds an error-only JNI stub in `fvoice_deepfilter_stub.cpp`; it does not fake DeepFilterNet with another engine.
