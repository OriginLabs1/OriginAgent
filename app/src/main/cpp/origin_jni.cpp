#include <jni.h>
#include <string>
#include <vector>

#include "origin_harness.h"

extern "C" JNIEXPORT jstring JNICALL
Java_com_oneorigin_originagent_util_NativeBridge_nativeVersion(JNIEnv* env, jclass /*clazz*/) {
    return env->NewStringUTF(origin_harness_version());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_oneorigin_originagent_util_NativeBridge_nativeNormalize(JNIEnv* env, jclass /*clazz*/, jstring input) {
    if (input == nullptr) return env->NewStringUTF("");
    const char* chars = env->GetStringUTFChars(input, nullptr);
    std::vector<char> buffer(static_cast<size_t>(env->GetStringUTFLength(input)) + 1);
    origin_normalize_intent(chars, buffer.data(), buffer.size());
    env->ReleaseStringUTFChars(input, chars);
    return env->NewStringUTF(buffer.data());
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_oneorigin_originagent_util_NativeBridge_nativeFnv1a64(JNIEnv* env, jclass /*clazz*/, jstring input) {
    if (input == nullptr) return static_cast<jlong>(origin_fnv1a64(nullptr));
    const char* chars = env->GetStringUTFChars(input, nullptr);
    const uint64_t hash = origin_fnv1a64(chars);
    env->ReleaseStringUTFChars(input, chars);
    return static_cast<jlong>(hash);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_oneorigin_originagent_util_NativeBridge_nativeVerifyChain(JNIEnv* env, jclass /*clazz*/, jstring chain) {
    if (chain == nullptr) return 1;
    const char* chars = env->GetStringUTFChars(chain, nullptr);
    const int result = origin_verify_chain(chars);
    env->ReleaseStringUTFChars(chain, chars);
    return static_cast<jint>(result);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_oneorigin_originagent_util_NativeBridge_nativeEngineSupported(JNIEnv* env, jclass /*clazz*/, jstring name) {
    if (name == nullptr) return 0;
    const char* chars = env->GetStringUTFChars(name, nullptr);
    const int result = origin_engine_supported(chars);
    env->ReleaseStringUTFChars(name, chars);
    return static_cast<jint>(result);
}
