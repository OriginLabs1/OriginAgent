# Origin Agent keep rules
# JNI entry points must keep their names if minification is enabled later.
-keep class com.oneorigin.originagent.util.NativeBridge { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}
