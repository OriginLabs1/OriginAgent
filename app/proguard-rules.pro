# Origin Agent keep rules
# JNI entry points must keep their names if minification is enabled later.
-keep class com.oneorigin.originagent.util.NativeBridge { *; }
-keep class dev.ffmpegkit.llama.** { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}
