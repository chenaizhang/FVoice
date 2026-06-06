# ProGuard rules for FVoice

# Keep Compose
-keep class androidx.compose.** { *; }
-keepclassmembers class androidx.compose.** { *; }

# Keep Kotlin serialization
-keepattributes *Annotation*, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeInvisibleAnnotations
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keep class kotlinx.serialization.json.** { *; }

# Keep Miuix
-keep class top.yukonga.miuix.** { *; }

# Keep ONNX Runtime
-keep class ai.onnxruntime.** { *; }

# Keep JNI classes
-keep class com.fvoice.app.core.jni.** { *; }

# General Android
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
