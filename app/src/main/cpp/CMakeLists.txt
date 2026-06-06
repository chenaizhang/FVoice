cmake_minimum_required(VERSION 3.22.1)
cmake_policy(SET CMP0077 NEW)
project("fvoice")

set(CMAKE_CXX_STANDARD 17)

# =============================================================================
# Core native library (always built)
# =============================================================================
add_library(fvoice-core SHARED fvoice_core.cpp)
find_library(log-lib log)
target_link_libraries(fvoice-core ${log-lib})

# =============================================================================
# RNNoise denoiser
# =============================================================================
add_library(
    rnnoise STATIC
    rnnoise/src/denoise.c
    rnnoise/src/rnn.c
    rnnoise/src/pitch.c
    rnnoise/src/kiss_fft.c
    rnnoise/src/celt_lpc.c
    rnnoise/src/nnet.c
    rnnoise/src/nnet_default.c
    rnnoise/src/parse_lpcnet_weights.c
    rnnoise/src/rnnoise_data.c
    rnnoise/src/rnnoise_tables.c
)
target_include_directories(rnnoise PUBLIC rnnoise/include rnnoise/src)
target_compile_definitions(rnnoise PRIVATE RNNOISE_BUILD)
target_link_libraries(rnnoise m)

add_library(fvoice-rnnoise SHARED fvoice_rnnoise.cpp)
target_include_directories(fvoice-rnnoise PRIVATE rnnoise/include)
target_link_libraries(fvoice-rnnoise rnnoise ${log-lib})

# =============================================================================
# DeepFilterNet denoiser
# =============================================================================
set(DEEPFILTER_LIB "${CMAKE_SOURCE_DIR}/deepfilternet/${CMAKE_ANDROID_ARCH_ABI}/libdf.a")
if(EXISTS "${DEEPFILTER_LIB}")
    message(STATUS "DeepFilterNet libDF found for ${CMAKE_ANDROID_ARCH_ABI}, enabling official JNI")
    add_library(deepfilter STATIC IMPORTED)
    set_target_properties(deepfilter PROPERTIES IMPORTED_LOCATION "${DEEPFILTER_LIB}")

    add_library(fvoice-df SHARED fvoice_deepfilter.cpp)
    target_include_directories(fvoice-df PRIVATE deepfilternet)
    target_link_libraries(fvoice-df deepfilter ${log-lib} m dl)
else()
    message(STATUS "DeepFilterNet libDF not found for ${CMAKE_ANDROID_ARCH_ABI}, building error-only JNI stub")
    add_library(fvoice-df SHARED fvoice_deepfilter_stub.cpp)
    target_link_libraries(fvoice-df ${log-lib})
endif()

# =============================================================================
# Optional: whisper.cpp integration
# Expected layout: app/src/main/cpp/whisper.cpp/
# =============================================================================
if(EXISTS "${CMAKE_SOURCE_DIR}/whisper.cpp/include/whisper.h")
    message(STATUS "whisper.cpp found, enabling Whisper JNI")

    # Build whisper.cpp as static library
    set(WHISPER_BUILD_TESTS OFF CACHE BOOL "" FORCE)
    set(WHISPER_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
    set(WHISPER_BUILD_SERVER OFF CACHE BOOL "" FORCE)
    set(WHISPER_CURL OFF CACHE BOOL "" FORCE)
    set(GGML_NATIVE OFF CACHE BOOL "" FORCE)
    set(GGML_OPENMP OFF CACHE BOOL "" FORCE)
    set(WHISPER_BUILD_SHARED_LIBRARY_DEFAULT OFF)
    set(BUILD_SHARED_LIBS OFF)
    add_subdirectory(whisper.cpp whisper.cpp_build)

    add_library(fvoice-whisper SHARED fvoice_whisper.cpp)
    target_include_directories(fvoice-whisper PRIVATE whisper.cpp/include)
    target_link_libraries(fvoice-whisper whisper ${log-lib})
else()
    message(STATUS "whisper.cpp not found, building Whisper stub")
    add_library(fvoice-whisper SHARED fvoice_whisper_stub.cpp)
    target_link_libraries(fvoice-whisper ${log-lib})
endif()
