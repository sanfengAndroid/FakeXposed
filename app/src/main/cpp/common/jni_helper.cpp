//
// Created by beich on 2020/12/18.
//

#include "jni_helper.h"
#include "scoped_utf_chars.h"
#include "proxy_jni.h"

jclass JNIHelper::java_lang_Object;
jclass JNIHelper::java_lang_Class;
jclass JNIHelper::java_lang_reflect_Method;
jclass JNIHelper::java_lang_reflect_Field;
jclass JNIHelper::java_lang_String;

jmethodID JNIHelper::java_lang_Object_toString;
jmethodID JNIHelper::java_lang_Object_getClass;
jmethodID JNIHelper::java_lang_Class_getName;
jmethodID JNIHelper::java_lang_reflect_Method_getName;
jmethodID JNIHelper::java_lang_reflect_Field_getName;

const char *JNIHelper::java_lang_String_sign = "Ljava/lang/String;";
bool JNIHelper::init_;

std::string JNIHelper::GetClassName(JNIEnv *env, jclass clazz) {
    ProxyJNIEnv proxy(env);

    if (__predict_false(clazz == nullptr)) {
        return "";
    }
    if (__predict_false(proxy.ExceptionCheck())) {
        return "There is an unhandled exception, this operation will be ignored";
    }
    auto name = reinterpret_cast<jstring>(proxy.CallObjectMethod(clazz, java_lang_Class_getName));
    if (__predict_false(proxy.ExceptionCheck())) {
        proxy.ExceptionClear();
        return "call Class.getName() exception occurs";
    }
    ScopedUtfChars sof(env, name);
    return sof.c_str();
}

std::string JNIHelper::GetObjectClassName(JNIEnv *env, jobject obj) {
    ProxyJNIEnv proxy(env);

    if (__predict_false(obj == nullptr)) {
        return "";
    }
    if (__predict_false(proxy.ExceptionCheck())) {
        return "There is an unhandled exception, this operation will be ignored";
    }
    ScopedLocalRef<jclass> clazz(&proxy, (jclass) proxy.CallObjectMethod(obj, java_lang_Object_getClass));
    if (__predict_false(proxy.ExceptionCheck())) {
        proxy.ExceptionClear();
        return "call getClass exception occurs";
    }
    return GetClassName(env, clazz.get());
}


std::string JNIHelper::ToString(JNIEnv *env, jobject l) {
    ProxyJNIEnv proxy(env);

    if (__predict_false(l == nullptr)) {
        return "";
    }
    if (__predict_false(proxy.ExceptionCheck())) {
        return "There is an unhandled exception, this operation will be ignored";
    }
    auto name = reinterpret_cast<jstring>(proxy.CallObjectMethod(l, java_lang_Object_toString));
    if (__predict_false(proxy.ExceptionCheck())) {
        proxy.ExceptionClear();
        return "ToString exception occurs";
    }
    ScopedUtfChars sof(env, name);
    return sof.c_str();
}


std::string JNIHelper::ToString(JNIEnv *env, jboolean z) {
    return z ? "true" : "false";
}

std::string JNIHelper::ToString(JNIEnv *env, jbyte b) {
    return std::to_string(b);
}

std::string JNIHelper::ToString(JNIEnv *env, jchar c) {
    return std::to_string(c);
}

std::string JNIHelper::ToString(JNIEnv *env, jshort s) {
    return std::to_string(s);
}

std::string JNIHelper::ToString(JNIEnv *env, jint i) {
    return std::to_string(i);
}

std::string JNIHelper::ToString(JNIEnv *env, jlong l) {
    return std::to_string(l);
}

std::string JNIHelper::ToString(JNIEnv *env, jfloat f) {
    return std::to_string(f);
}

std::string JNIHelper::ToString(JNIEnv *env, jdouble d) {
    return std::to_string(d);
}

std::string JNIHelper::ToString(JNIEnv *env, jmethodID methodID) {
    ProxyJNIEnv proxy(env);

    if (methodID == nullptr) {
        return "null jmethodID";
    }
    if (__predict_false(proxy.ExceptionCheck())) {
        return "There is an unhandled exception, this operation will be ignored";
    }
    ScopedLocalRef<jobject> method(&proxy, proxy.ToReflectedMethod(java_lang_Object, methodID, false));
    if (__predict_false(proxy.ExceptionCheck())) {
        proxy.ExceptionClear();
        return "Method.toString exception occurs";
    }
    return ToString(env, method.get());
}

std::string JNIHelper::ToString(JNIEnv *env, jfieldID fieldID) {
    ProxyJNIEnv proxy(env);
    if (__predict_false(proxy.ExceptionCheck())) {
        return "There is an unhandled exception, this operation will be ignored";
    }
    ScopedLocalRef<jobject> filed(&proxy, proxy.ToReflectedField(java_lang_Object, fieldID, false));
    if (__predict_false(proxy.ExceptionCheck())) {
        proxy.ExceptionClear();
        return "Field.toString exception occurs";
    }
    return ToString(env, filed.get());
}

void JNIHelper::PrintAndClearException(JNIEnv *env) {
    if (env->ExceptionCheck()) {
        env->ExceptionDescribe();
        env->ExceptionClear();
    }
}

std::string JNIHelper::GetMethodName(JNIEnv *env, jmethodID mid) {
    ProxyJNIEnv proxy(env);
    if (__predict_false(proxy.ExceptionCheck())) {
        return "There is an unhandled exception, this operation will be ignored";
    }
    jobject obj = proxy.ToReflectedMethod(JNIHelper::java_lang_reflect_Method, mid, false);
    ScopedLocalRef<jobject> method_obj(&proxy, obj);
    auto name = reinterpret_cast<jstring>(proxy.CallObjectMethod(method_obj.get(), java_lang_reflect_Method_getName));
    if (__predict_false(proxy.ExceptionCheck())) {
        proxy.ExceptionClear();
        return "Method.getName exception occurs";
    }
    ScopedUtfChars sof(env, name);
    return sof.c_str();
}

std::string JNIHelper::GetFieldName(JNIEnv *env, jfieldID fieldId) {
    ProxyJNIEnv proxy(env);
    if (__predict_false(proxy.ExceptionCheck())) {
        return "There is an unhandled exception, this operation will be ignored";
    }
    ScopedLocalRef<jobject> field_obj(env, proxy.ToReflectedField(JNIHelper::java_lang_reflect_Field, fieldId, false));
    auto name = reinterpret_cast<jstring>(proxy.CallObjectMethod(field_obj.get(), java_lang_reflect_Field_getName));
    if (__predict_false(proxy.ExceptionCheck())) {
        proxy.ExceptionClear();
        return "Field.getName exception occurs";
    }
    ScopedUtfChars sof(env, name);
    return sof.c_str();
}

bool JNIHelper::IsClassObject(JNIEnv *env, jobject obj) {
    ProxyJNIEnv proxy(env);
    if (__predict_false(proxy.ExceptionCheck())) {
        proxy.ExceptionClear();
        return "Method.getName exception occurs";
    }
    return proxy.IsInstanceOf(obj, JNIHelper::java_lang_Class);
}

jclass JNIHelper::CacheClass(JNIEnv *env, const char *jni_class_name) {
    ScopedLocalRef<jclass> c(env, env->FindClass(jni_class_name));
    if (c.get() == nullptr) {
        LOGE("Couldn't find class: %s", jni_class_name);
        return nullptr;
    }
    return reinterpret_cast<jclass>(env->NewGlobalRef(c.get()));
}

void JNIHelper::ReleaseBytes(JNIEnv *env, jbyteArray arr, const char *parr) {
    if (parr != nullptr) {
        env->ReleaseByteArrayElements(arr, (jbyte *) parr, JNI_ABORT);
    }
}

void JNIHelper::ReleaseInts(JNIEnv *env, jintArray arr, int *parr) {
    if (parr != nullptr) {
        env->ReleaseIntArrayElements(arr, parr, 0);
    }
}


void JNIHelper::ClearException(JNIEnv *env) {
    if (env->ExceptionCheck()) {
        env->ExceptionClear();
    }
}

static jmethodID CacheMethod(JNIEnv *env, jclass c, bool is_static, const char *name, const char *signature) {
    jmethodID mid;
    mid = is_static ? env->GetStaticMethodID(c, name, signature) : env->GetMethodID(c, name, signature);
    return mid;
}

void JNIHelper::Init(JNIEnv *env) {
    if (init_) {
        return;
    }
    CHECK(java_lang_Object = CacheClass(env, "java/lang/Object"));
    CHECK(java_lang_Class = CacheClass(env, "java/lang/Class"));
    CHECK(java_lang_reflect_Method = CacheClass(env, "java/lang/reflect/Method"));
    CHECK(java_lang_reflect_Field = CacheClass(env, "java/lang/reflect/Field"));
    CHECK(java_lang_String = CacheClass(env, "java/lang/String"));

    CHECK(java_lang_Object_toString = CacheMethod(env, java_lang_Object, false, "toString", "()Ljava/lang/String;"));
    CHECK(java_lang_Object_getClass = CacheMethod(env, java_lang_Object, false, "getClass", "()Ljava/lang/Class;"));
    CHECK(java_lang_Class_getName = CacheMethod(env, java_lang_Class, false, "getName", "()Ljava/lang/String;"));
    CHECK(java_lang_reflect_Method_getName = CacheMethod(env, java_lang_reflect_Method, false, "getName", "()Ljava/lang/String;"));
    CHECK(java_lang_reflect_Field_getName = CacheMethod(env, java_lang_reflect_Field, false, "getName", "()Ljava/lang/String;"));

    init_ = true;
}

void JNIHelper::Clear() {
    java_lang_Object = nullptr;
    java_lang_Class = nullptr;
    java_lang_reflect_Method = nullptr;
    java_lang_reflect_Field = nullptr;

    java_lang_Object_toString = nullptr;
    java_lang_Object_getClass = nullptr;
    java_lang_Class_getName = nullptr;
    java_lang_reflect_Method_getName = nullptr;
    java_lang_reflect_Field_getName = nullptr;
    init_ = false;
}