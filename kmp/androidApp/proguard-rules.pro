# ==============================================================================
# Finsim ComposeApp - ProGuard / R8 Optimization Rules
# ==============================================================================
# Configured with proguard-android-optimize.txt for maximum shrinking and performance.

# ------------------------------------------------------------------------------
# 1. R8 Optimization & Obfuscation Dictionaries
# ------------------------------------------------------------------------------
# Repackage all obfuscated classes into the root package to flatten package structure and reduce DEX string pool
-repackageclasses ''

# Allow R8 to loosen method/field access modifiers for more aggressive inlining
-allowaccessmodification

# Preserve debugging attributes for meaningful crash reports while obfuscating identifiers
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ------------------------------------------------------------------------------
# 2. Application Entry Points
# ------------------------------------------------------------------------------
# Preserve MainActivity instantiated by Android OS manifest
-keep public class com.finsim.app.MainActivity extends androidx.activity.ComponentActivity {
    public <init>();
}

# ------------------------------------------------------------------------------
# 3. Compose Multiplatform & Material 3
# ------------------------------------------------------------------------------
# Preserve Composable function signatures and compiler-generated metadata
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
    @androidx.compose.runtime.ReadOnlyComposable *;
}

# Preserve Compose Multiplatform generated resource accessors
-keep class finsim.composeapp.generated.resources.** { *; }

# ------------------------------------------------------------------------------
# 4. Kermit Logger Performance Optimization
# ------------------------------------------------------------------------------
# Strip verbose and debug logging calls in release builds to eliminate string concat and lambda allocations
-assumenosideeffects class co.touchlab.kermit.Logger {
    public void v(...);
    public void d(...);
}

# ------------------------------------------------------------------------------
# 5. Coroutines & Platform Compatibility
# ------------------------------------------------------------------------------
# Coroutines debug agent is not used in production Android
-dontwarn kotlinx.coroutines.debug.**

# ServiceLoader provider for MainDispatcher
-keepclassmembernames class kotlinx.coroutines.internal.MainDispatcherFactory {
    <methods>;
}

# Suppress warnings for JVM management beans not present on Android
-dontwarn java.lang.management.**

# LiteRT-LM 0.18.0 JNI uses literal class and member names from native code.
# Keep only those entry points stable while leaving the rest of the library optimized.
-keep class com.google.ai.edge.litertlm.SamplerConfig {
    public int getTopK();
    public double getTopP();
    public double getTemperature();
    public int getSeed();
}
-keep class com.google.ai.edge.litertlm.ThinkingConfig {
    public boolean getEnableThinking();
    public int getThinkingTokenBudget();
}
-keep class com.google.ai.edge.litertlm.RepetitionPenaltyConfig {
    public java.lang.Float getRepetitionPenalty();
    public java.lang.Float getPresencePenalty();
    public java.lang.Float getFrequencyPenalty();
    public java.lang.Integer getWindowSize();
}
-keep class com.google.ai.edge.litertlm.NoRepeatNgramConfig {
    public java.lang.Integer getNoRepeatNgramSize();
    public java.lang.Integer getWindowSize();
}
-keep class com.google.ai.edge.litertlm.InputData$Text {
    public java.lang.String getText();
}
-keep class com.google.ai.edge.litertlm.InputData$Audio {
    public byte[] getBytes();
}
-keep class com.google.ai.edge.litertlm.InputData$Image {
    public byte[] getBytes();
}
-keep class com.google.ai.edge.litertlm.LiteRtLmJniException {
    public <init>(java.lang.String);
}
-keep class com.google.ai.edge.litertlm.BenchmarkInfo {
    public <init>(double, double, int, int, double, double);
}
-keep interface com.google.ai.edge.litertlm.LiteRtLmJni$JniInferenceCallback {
    public void onNext(java.lang.String);
    public void onDone();
    public void onError(int, java.lang.String);
}
-keepclassmembers class * implements com.google.ai.edge.litertlm.LiteRtLmJni$JniInferenceCallback {
    public void onNext(java.lang.String);
    public void onDone();
    public void onError(int, java.lang.String);
}
-keep interface com.google.ai.edge.litertlm.LiteRtLmJni$JniMessageCallback {
    public void onMessage(java.lang.String);
    public void onDone();
    public void onError(int, java.lang.String);
}
-keepclassmembers class * implements com.google.ai.edge.litertlm.LiteRtLmJni$JniMessageCallback {
    public void onMessage(java.lang.String);
    public void onDone();
    public void onError(int, java.lang.String);
}
