//
// Created by beich on 2020/12/18.
//

#pragma once

#include <string>
#include <jni.h>
#include <vector>

#include "scoped_local_ref.h"

class JNIHelper {
public:
    static std::string GetClassName(JNIEnv *env, jclass clazz);

    static std::string GetObjectClassName(JNIEnv *env, jobject obj);

    static std::string GetMethodName(JNIEnv *env, jmethodID mid);

    static std::string GetFieldName(JNIEnv *env, jfieldID fieldId);

    static bool IsClassObject(JNIEnv *env, jobject obj);

    static std::string ToString(JNIEnv *env, jboolean z);

    static std::string ToString(JNIEnv *env, jbyte b);

    static std::string ToString(JNIEnv *env, jchar c);

    static std::string ToString(JNIEnv *env, jshort s);

    static std::string ToString(JNIEnv *env, jint i);

    static std::string ToString(JNIEnv *env, jlong j);

    static std::string ToString(JNIEnv *env, jfloat f);

    static std::string ToString(JNIEnv *env, jdouble d);

    static std::string ToString(JNIEnv *env, jobject l);

    static std::string ToString(JNIEnv *env, jmethodID methodID);

    static std::string ToString(JNIEnv *env, jfieldID fieldID);

    static void PrintAndClearException(JNIEnv *env);

    static void ClearException(JNIEnv *env);

    static jclass CacheClass(JNIEnv *env, const char *jni_class_name);

    static void ReleaseBytes(JNIEnv *env, jbyteArray arr, const char *parr);

    static void ReleaseInts(JNIEnv *env, jintArray arr, jint *parr);

public:
    static void Init(JNIEnv *env);

    static void Clear();

public:
    static jclass java_lang_Object;
    static jclass java_lang_Class;
    static jclass java_lang_reflect_Method;
    static jclass java_lang_reflect_Field;
    static jclass java_lang_String;

    static jmethodID java_lang_Object_toString;
    static jmethodID java_lang_Object_getClass;
    static jmethodID java_lang_reflect_Field_getName;
    static jmethodID java_lang_Class_getName;
    static jmethodID java_lang_reflect_Method_getName;

    static const char *java_lang_String_sign;

private:
    static bool init_;
};


template<typename StringVisitor>
jobjectArray toStringArray(JNIEnv *env, size_t count, StringVisitor &&visitor) {
    jclass stringClass = env->FindClass("java/lang/String");
    ScopedLocalRef<jobjectArray> result(env, env->NewObjectArray(count, stringClass, NULL));
    env->DeleteLocalRef(stringClass);
    if (result == nullptr) {
        return nullptr;
    }
    for (size_t i = 0; i < count; ++i) {
        ScopedLocalRef<jstring> s(env, env->NewStringUTF(visitor(i)));
        if (env->ExceptionCheck()) {
            return nullptr;
        }
        env->SetObjectArrayElement(result.get(), i, s.get());
        if (env->ExceptionCheck()) {
            return nullptr;
        }
    }
    return result.release();
}

inline jobjectArray toStringArray(JNIEnv *env, const std::vector<std::string> &strings) {
    return toStringArray(env, strings.size(), [&strings](size_t i) { return strings[i].c_str(); });
}

inline jobjectArray toStringArray(JNIEnv *env, const char *const *strings) {
    size_t count = 0;
    for (; strings[count] != nullptr; ++count) {}
    return toStringArray(env, count, [&strings](size_t i) { return strings[i]; });
}

template<typename Counter, typename Getter>
jobjectArray toStringArray(JNIEnv *env, Counter *counter, Getter *getter) {
    return toStringArray(env, counter(), [getter](size_t i) { return getter(i); });
}