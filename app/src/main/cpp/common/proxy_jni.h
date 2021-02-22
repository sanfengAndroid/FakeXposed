//
// Created by beich on 2020/12/20.
//

#pragma once

#include <jni.h>

extern "C" JNINativeInterface *original_functions;

class ProxyJNIEnv {
public:
// JNI Proxy
    jint GetVersion() { return functions->GetVersion(env_); }

    jclass DefineClass(const char *name, jobject loader, const jbyte *buf, jsize bufLen) {
        return functions->DefineClass != nullptr ? functions->DefineClass(env_, name, loader, buf, bufLen) : env_->DefineClass(name, loader, buf, bufLen);
    }

    jclass FindClass(const char *name) { return functions->FindClass != nullptr ? functions->FindClass(env_, name) : env_->FindClass(name); }

    jmethodID FromReflectedMethod(jobject method) {
        return functions->FromReflectedMethod != nullptr ? functions->FromReflectedMethod(env_, method) : env_->FromReflectedMethod(method);
    }

    jfieldID FromReflectedField(jobject field) { return functions->FromReflectedField != nullptr ? functions->FromReflectedField(env_, field) : env_->FromReflectedField(field); }

    jobject ToReflectedMethod(jclass cls, jmethodID methodID, jboolean isStatic) {
        return functions->ToReflectedMethod != nullptr ? functions->ToReflectedMethod(env_, cls, methodID, isStatic) : env_->ToReflectedMethod(cls, methodID, isStatic);
    }

    jclass GetSuperclass(jclass clazz) { return functions->GetSuperclass != nullptr ? functions->GetSuperclass(env_, clazz) : env_->GetSuperclass(clazz); }

    jboolean IsAssignableFrom(jclass clazz1, jclass clazz2) {
        return functions->IsAssignableFrom != nullptr ? functions->IsAssignableFrom(env_, clazz1, clazz2) : env_->IsAssignableFrom(clazz1, clazz2);
    }

    jobject ToReflectedField(jclass cls, jfieldID fieldID, jboolean isStatic) {
        return functions->ToReflectedField != nullptr ? functions->ToReflectedField(env_, cls, fieldID, isStatic) : env_->ToReflectedField(cls, fieldID, isStatic);
    }

    jint Throw(jthrowable obj) { return functions->Throw != nullptr ? functions->Throw(env_, obj) : env_->Throw(obj); }

    jint ThrowNew(jclass clazz, const char *message) { return functions->ThrowNew != nullptr ? functions->ThrowNew(env_, clazz, message) : env_->ThrowNew(clazz, message); }

    jthrowable ExceptionOccurred() { return functions->ExceptionOccurred != nullptr ? functions->ExceptionOccurred(env_) : env_->ExceptionOccurred(); }

    void ExceptionDescribe() {
        if (functions->ExceptionDescribe != nullptr)
            functions->ExceptionDescribe(env_);
        else
            env_->ExceptionDescribe();
    }

    void ExceptionClear() {
        if (functions->ExceptionClear != nullptr)
            functions->ExceptionClear(env_);
        else
            env_->ExceptionClear();
    }

    void FatalError(const char *msg) { functions->FatalError != nullptr ? functions->FatalError(env_, msg) : env_->FatalError(msg); }

    jint PushLocalFrame(jint capacity) { return functions->PushLocalFrame != nullptr ? functions->PushLocalFrame(env_, capacity) : env_->PushLocalFrame(capacity); }

    jobject PopLocalFrame(jobject result) { return functions->PopLocalFrame != nullptr ? functions->PopLocalFrame(env_, result) : env_->PopLocalFrame(result); }

    jobject NewGlobalRef(jobject obj) { return functions->NewGlobalRef != nullptr ? functions->NewGlobalRef(env_, obj) : env_->NewGlobalRef(obj); }

    void DeleteGlobalRef(jobject globalRef) { functions->DeleteGlobalRef != nullptr ? functions->DeleteGlobalRef(env_, globalRef) : env_->DeleteGlobalRef(globalRef); }

    void DeleteLocalRef(jobject localRef) { functions->DeleteLocalRef != nullptr ? functions->DeleteLocalRef(env_, localRef) : env_->DeleteLocalRef(localRef); }

    jboolean IsSameObject(jobject ref1, jobject ref2) { return functions->IsSameObject != nullptr ? functions->IsSameObject(env_, ref1, ref2) : env_->IsSameObject(ref1, ref2); }

    jobject NewLocalRef(jobject ref) { return functions->NewLocalRef != nullptr ? functions->NewLocalRef(env_, ref) : env_->NewLocalRef(ref); }

    jint EnsureLocalCapacity(jint capacity) {
        return functions->EnsureLocalCapacity != nullptr ? functions->EnsureLocalCapacity(env_, capacity) : env_->EnsureLocalCapacity(capacity);
    }

    jobject AllocObject(jclass clazz) { return functions->AllocObject != nullptr ? functions->AllocObject(env_, clazz) : env_->AllocObject(clazz); }

    jobject NewObject(jclass clazz, jmethodID methodID, ...) {
        va_list args;
        va_start(args, methodID);
        jobject result = functions->NewObjectV != nullptr ? functions->NewObjectV(env_, clazz, methodID, args) : env_->NewObjectV(clazz, methodID, args);
        va_end(args);
        return result;
    }

    jobject NewObjectV(jclass clazz, jmethodID methodID, va_list args) {
        return functions->NewObjectV != nullptr ? functions->NewObjectV(env_, clazz, methodID, args) : env_->NewObjectV(clazz, methodID, args);
    }

    jobject NewObjectA(jclass clazz, jmethodID methodID, const jvalue *args) {
        return functions->NewObjectA != nullptr ? functions->NewObjectA(env_, clazz, methodID, args) : env_->NewObjectA(clazz, methodID, args);
    }

    jclass GetObjectClass(jobject obj) { return functions->GetObjectClass != nullptr ? functions->GetObjectClass(env_, obj) : env_->GetObjectClass(obj); }

    jboolean IsInstanceOf(jobject obj, jclass clazz) { return functions->IsInstanceOf != nullptr ? functions->IsInstanceOf(env_, obj, clazz) : env_->IsInstanceOf(obj, clazz); }

    jmethodID GetMethodID(jclass clazz, const char *name, const char *sig) {
        return functions->GetMethodID != nullptr ? functions->GetMethodID(env_, clazz, name, sig) : env_->GetMethodID(clazz, name, sig);
    }

#define PROXY_CALL_TYPE_METHOD(_jtype, _jname)                                    \
    _jtype Call##_jname##Method(jobject obj, jmethodID methodID, ...)       \
    {                                                                       \
        _jtype result;                                                      \
        va_list args;                                                       \
        va_start(args, methodID);                                           \
        if(functions->Call##_jname##MethodV != nullptr)                        \
            result = functions->Call##_jname##MethodV(env_, obj, methodID,args);                                                  \
        else                                                                \
            result = env_->Call##_jname##MethodV(obj, methodID,args);        \
        va_end(args);                                                       \
        return result;                                                      \
    }
#define PROXY_CALL_TYPE_METHODV(_jtype, _jname)                                   \
    _jtype Call##_jname##MethodV(jobject obj, jmethodID methodID,           \
        va_list args)                                                       \
    { return functions->Call##_jname##MethodV != nullptr ? functions->Call##_jname##MethodV(env_, obj, methodID, args) : env_->Call##_jname##MethodV(obj, methodID, args); }
#define PROXY_CALL_TYPE_METHODA(_jtype, _jname)                                   \
    _jtype Call##_jname##MethodA(jobject obj, jmethodID methodID,  const jvalue* args)                        \
    { return functions->Call##_jname##MethodA != nullptr ? functions->Call##_jname##MethodA(env_, obj, methodID, args) : env_->Call##_jname##MethodA(obj, methodID, args); }

#define PROXY_CALL_TYPE(_jtype, _jname)                                           \
    PROXY_CALL_TYPE_METHOD(_jtype, _jname)                                        \
    PROXY_CALL_TYPE_METHODV(_jtype, _jname)                                       \
    PROXY_CALL_TYPE_METHODA(_jtype, _jname)

    PROXY_CALL_TYPE(jobject, Object)

    PROXY_CALL_TYPE(jboolean, Boolean)

    PROXY_CALL_TYPE(jbyte, Byte)

    PROXY_CALL_TYPE(jchar, Char)

    PROXY_CALL_TYPE(jshort, Short)

    PROXY_CALL_TYPE(jint, Int)

    PROXY_CALL_TYPE(jlong, Long)

    PROXY_CALL_TYPE(jfloat, Float)

    PROXY_CALL_TYPE(jdouble, Double)

    void CallVoidMethod(jobject obj, jmethodID methodID, ...) {
        va_list args;
        va_start(args, methodID);
        functions->CallVoidMethodV != nullptr ? functions->CallVoidMethodV(env_, obj, methodID, args) : env_->CallVoidMethodV(obj, methodID, args);
        va_end(args);
    }

    void CallVoidMethodV(jobject obj, jmethodID methodID, va_list args) {
        functions->CallVoidMethodV != nullptr ? functions->CallVoidMethodV(env_, obj, methodID, args) : env_->CallVoidMethodV(obj, methodID, args);
    }

    void CallVoidMethodA(jobject obj, jmethodID methodID, const jvalue *args) {
        functions->CallVoidMethodA != nullptr ? functions->CallVoidMethodA(env_, obj, methodID, args) : env_->CallVoidMethodA(obj, methodID, args);
    }

#define PROXY_CALL_NONVIRT_TYPE_METHOD(_jtype, _jname)                            \
    _jtype CallNonvirtual##_jname##Method(jobject obj, jclass clazz,        \
        jmethodID methodID, ...)                                            \
    {                                                                       \
        _jtype result;                                                      \
        va_list args;                                                       \
        va_start(args, methodID);                                           \
        if( functions->CallNonvirtual##_jname##MethodV != nullptr)            \
            result = functions->CallNonvirtual##_jname##MethodV(env_, obj, clazz, methodID, args);                                 \
        else                                                                \
            result = env_->CallNonvirtual##_jname##MethodV(obj, clazz, methodID, args);  \
        va_end(args);                                                       \
        return result;                                                      \
    }
#define PROXY_CALL_NONVIRT_TYPE_METHODV(_jtype, _jname)                           \
    _jtype CallNonvirtual##_jname##MethodV(jobject obj, jclass clazz,       \
        jmethodID methodID, va_list args)                                   \
    { return functions->CallNonvirtual##_jname##MethodV != nullptr ? functions->CallNonvirtual##_jname##MethodV(env_, obj, clazz, methodID, args) :    \
        env_->CallNonvirtual##_jname##MethodV(obj, clazz, methodID, args); }
#define PROXY_CALL_NONVIRT_TYPE_METHODA(_jtype, _jname)                           \
    _jtype CallNonvirtual##_jname##MethodA(jobject obj, jclass clazz,       \
        jmethodID methodID, const jvalue* args)                             \
    { return functions->CallNonvirtual##_jname##MethodA != nullptr ? functions->CallNonvirtual##_jname##MethodA(env_, obj, clazz, methodID, args) : \
        env_->CallNonvirtual##_jname##MethodA(obj, clazz, methodID, args); }

#define PROXY_CALL_NONVIRT_TYPE(_jtype, _jname)                                   \
    PROXY_CALL_NONVIRT_TYPE_METHOD(_jtype, _jname)                                \
    PROXY_CALL_NONVIRT_TYPE_METHODV(_jtype, _jname)                               \
    PROXY_CALL_NONVIRT_TYPE_METHODA(_jtype, _jname)

    PROXY_CALL_NONVIRT_TYPE(jobject, Object)

    PROXY_CALL_NONVIRT_TYPE(jboolean, Boolean)

    PROXY_CALL_NONVIRT_TYPE(jbyte, Byte)

    PROXY_CALL_NONVIRT_TYPE(jchar, Char)

    PROXY_CALL_NONVIRT_TYPE(jshort, Short)

    PROXY_CALL_NONVIRT_TYPE(jint, Int)

    PROXY_CALL_NONVIRT_TYPE(jlong, Long)

    PROXY_CALL_NONVIRT_TYPE(jfloat, Float)

    PROXY_CALL_NONVIRT_TYPE(jdouble, Double)

    void CallNonvirtualVoidMethod(jobject obj, jclass clazz, jmethodID methodID, ...) {
        va_list args;
        va_start(args, methodID);
        functions->CallNonvirtualVoidMethodV != nullptr ? functions->CallNonvirtualVoidMethodV(env_, obj, clazz, methodID, args) : env_->CallNonvirtualVoidMethodV(obj, clazz,
                                                                                                                                                                   methodID, args);
        va_end(args);
    }

    void CallNonvirtualVoidMethodV(jobject obj, jclass clazz, jmethodID methodID, va_list args) {
        functions->CallNonvirtualVoidMethodV != nullptr ? functions->CallNonvirtualVoidMethodV(env_, obj, clazz, methodID, args) : env_->CallNonvirtualVoidMethodV(obj, clazz,
                                                                                                                                                                   methodID, args);
    }

    void CallNonvirtualVoidMethodA(jobject obj, jclass clazz, jmethodID methodID, const jvalue *args) {
        functions->CallNonvirtualVoidMethodA != nullptr ? functions->CallNonvirtualVoidMethodA(env_, obj, clazz, methodID, args) : env_->CallNonvirtualVoidMethodA(obj, clazz,
                                                                                                                                                                   methodID, args);
    }

    jfieldID GetFieldID(jclass clazz, const char *name, const char *sig) {
        return functions->GetFieldID != nullptr ? functions->GetFieldID(env_, clazz, name, sig) : env_->GetFieldID(clazz, name, sig);
    }

    jobject GetObjectField(jobject obj, jfieldID fieldID) {
        return functions->GetObjectField != nullptr ? functions->GetObjectField(env_, obj, fieldID) : env_->GetObjectField(obj, fieldID);
    }

    jboolean GetBooleanField(jobject obj, jfieldID fieldID) {
        return functions->GetBooleanField != nullptr ? functions->GetBooleanField(env_, obj, fieldID) : env_->GetBooleanField(obj, fieldID);
    }

    jbyte GetByteField(jobject obj, jfieldID fieldID) {
        return functions->GetByteField != nullptr ? functions->GetByteField(env_, obj, fieldID) : env_->GetByteField(obj, fieldID);
    }

    jchar GetCharField(jobject obj, jfieldID fieldID) {
        return functions->GetCharField != nullptr ? functions->GetCharField(env_, obj, fieldID) : env_->GetCharField(obj, fieldID);
    }

    jshort GetShortField(jobject obj, jfieldID fieldID) {
        return functions->GetShortField != nullptr ? functions->GetShortField(env_, obj, fieldID) : env_->GetShortField(obj, fieldID);
    }

    jint GetIntField(jobject obj, jfieldID fieldID) { return functions->GetIntField != nullptr ? functions->GetIntField(env_, obj, fieldID) : env_->GetIntField(obj, fieldID); }

    jlong GetLongField(jobject obj, jfieldID fieldID) {
        return functions->GetLongField != nullptr ? functions->GetLongField(env_, obj, fieldID) : env_->GetLongField(obj, fieldID);
    }

    jfloat GetFloatField(jobject obj, jfieldID fieldID) {
        return functions->GetFloatField != nullptr ? functions->GetFloatField(env_, obj, fieldID) : env_->GetFloatField(obj, fieldID);
    }

    jdouble GetDoubleField(jobject obj, jfieldID fieldID) {
        return functions->GetDoubleField != nullptr ? functions->GetDoubleField(env_, obj, fieldID) : env_->GetDoubleField(obj, fieldID);
    }

    void SetObjectField(jobject obj, jfieldID fieldID, jobject value) {
        functions->SetObjectField != nullptr ? functions->SetObjectField(env_, obj, fieldID, value) : env_->SetObjectField(obj, fieldID, value);
    }

    void SetBooleanField(jobject obj, jfieldID fieldID, jboolean value) {
        functions->SetBooleanField != nullptr ? functions->SetBooleanField(env_, obj, fieldID, value) : env_->SetBooleanField(obj, fieldID, value);
    }

    void SetByteField(jobject obj, jfieldID fieldID, jbyte value) {
        functions->SetByteField != nullptr ? functions->SetByteField(env_, obj, fieldID, value) : env_->SetByteField(obj, fieldID, value);
    }

    void SetCharField(jobject obj, jfieldID fieldID, jchar value) {
        functions->SetCharField != nullptr ? functions->SetCharField(env_, obj, fieldID, value) : env_->SetCharField(obj, fieldID, value);
    }

    void SetShortField(jobject obj, jfieldID fieldID, jshort value) {
        functions->SetShortField != nullptr ? functions->SetShortField(env_, obj, fieldID, value) : env_->SetShortField(obj, fieldID, value);
    }

    void SetIntField(jobject obj, jfieldID fieldID, jint value) {
        functions->SetIntField != nullptr ? functions->SetIntField(env_, obj, fieldID, value) : env_->SetIntField(obj, fieldID, value);
    }

    void SetLongField(jobject obj, jfieldID fieldID, jlong value) {
        functions->SetLongField != nullptr ? functions->SetLongField(env_, obj, fieldID, value) : env_->SetLongField(obj, fieldID, value);
    }

    void SetFloatField(jobject obj, jfieldID fieldID, jfloat value) {
        functions->SetFloatField != nullptr ? functions->SetFloatField(env_, obj, fieldID, value) : env_->SetFloatField(obj, fieldID, value);
    }

    void SetDoubleField(jobject obj, jfieldID fieldID, jdouble value) {
        functions->SetDoubleField != nullptr ? functions->SetDoubleField(env_, obj, fieldID, value) : env_->SetDoubleField(obj, fieldID, value);
    }

    jmethodID GetStaticMethodID(jclass clazz, const char *name, const char *sig) {
        return functions->GetStaticMethodID != nullptr ? functions->GetStaticMethodID(env_, clazz, name, sig) : env_->GetStaticMethodID(clazz, name, sig);
    }

#define PROXY_CALL_STATIC_TYPE_METHOD(_jtype, _jname)                             \
    _jtype CallStatic##_jname##Method(jclass clazz, jmethodID methodID,  ...)                                                                \
    {                                                                       \
        _jtype result;                                                      \
        va_list args;                                                       \
        va_start(args, methodID);                                           \
        result = functions->CallStatic##_jname##MethodV != nullptr ? functions->CallStatic##_jname##MethodV(env_, clazz, methodID, args) : \
            env_->CallStatic##_jname##MethodV(clazz, methodID, args);        \
        va_end(args);                                                       \
        return result;                                                      \
    }
#define PROXY_CALL_STATIC_TYPE_METHODV(_jtype, _jname)                            \
    _jtype CallStatic##_jname##MethodV(jclass clazz, jmethodID methodID,    \
        va_list args)                                                       \
    { return functions->CallStatic##_jname##MethodV != nullptr ? functions->CallStatic##_jname##MethodV(env_, clazz, methodID, args) : \
        env_->CallStatic##_jname##MethodV(clazz, methodID, args); }
#define PROXY_CALL_STATIC_TYPE_METHODA(_jtype, _jname)                            \
    _jtype CallStatic##_jname##MethodA(jclass clazz, jmethodID methodID,    \
                                       const jvalue* args)                  \
    { return functions->CallStatic##_jname##MethodA != nullptr ? functions->CallStatic##_jname##MethodA(env_, clazz, methodID,args) :    \
    env_->CallStatic##_jname##MethodA(clazz, methodID,args); }                    \

#define PROXY_CALL_STATIC_TYPE(_jtype, _jname)                                    \
    PROXY_CALL_STATIC_TYPE_METHOD(_jtype, _jname)                                 \
    PROXY_CALL_STATIC_TYPE_METHODV(_jtype, _jname)                                \
    PROXY_CALL_STATIC_TYPE_METHODA(_jtype, _jname)

    PROXY_CALL_STATIC_TYPE(jobject, Object)

    PROXY_CALL_STATIC_TYPE(jboolean, Boolean)

    PROXY_CALL_STATIC_TYPE(jbyte, Byte)

    PROXY_CALL_STATIC_TYPE(jchar, Char)

    PROXY_CALL_STATIC_TYPE(jshort, Short)

    PROXY_CALL_STATIC_TYPE(jint, Int)

    PROXY_CALL_STATIC_TYPE(jlong, Long)

    PROXY_CALL_STATIC_TYPE(jfloat, Float)

    PROXY_CALL_STATIC_TYPE(jdouble, Double)

    void CallStaticVoidMethod(jclass clazz, jmethodID methodID, ...) {
        va_list args;
        va_start(args, methodID);
        functions->CallStaticVoidMethodV != nullptr ? functions->CallStaticVoidMethodV(env_, clazz, methodID, args) : env_->CallStaticVoidMethodV(clazz, methodID, args);
        va_end(args);
    }

    void CallStaticVoidMethodV(jclass clazz, jmethodID methodID, va_list args) {
        functions->CallStaticVoidMethodV != nullptr ? functions->CallStaticVoidMethodV(env_, clazz, methodID, args) : env_->CallStaticVoidMethodV(clazz, methodID, args);
    }

    void CallStaticVoidMethodA(jclass clazz, jmethodID methodID, const jvalue *args) {
        functions->CallStaticVoidMethodA != nullptr ? functions->CallStaticVoidMethodA(env_, clazz, methodID, args) : env_->CallStaticVoidMethodA(clazz, methodID, args);
    }

    jfieldID GetStaticFieldID(jclass clazz, const char *name, const char *sig) {
        return functions->GetStaticFieldID != nullptr ? functions->GetStaticFieldID(env_, clazz, name, sig) : env_->GetStaticFieldID(clazz, name, sig);
    }

    jobject GetStaticObjectField(jclass clazz, jfieldID fieldID) {
        return functions->GetStaticObjectField != nullptr ? functions->GetStaticObjectField(env_, clazz, fieldID) : env_->GetStaticObjectField(clazz, fieldID);
    }

    jboolean GetStaticBooleanField(jclass clazz, jfieldID fieldID) {
        return functions->GetStaticBooleanField != nullptr ? functions->GetStaticBooleanField(env_, clazz, fieldID) : env_->GetStaticBooleanField(clazz, fieldID);
    }

    jbyte GetStaticByteField(jclass clazz, jfieldID fieldID) {
        return functions->GetStaticByteField != nullptr ? functions->GetStaticByteField(env_, clazz, fieldID) : env_->GetStaticByteField(clazz, fieldID);
    }

    jchar GetStaticCharField(jclass clazz, jfieldID fieldID) {
        return functions->GetStaticCharField != nullptr ? functions->GetStaticCharField(env_, clazz, fieldID) : env_->GetStaticCharField(clazz, fieldID);
    }

    jshort GetStaticShortField(jclass clazz, jfieldID fieldID) {
        return functions->GetStaticShortField != nullptr ? functions->GetStaticShortField(env_, clazz, fieldID) : env_->GetStaticShortField(clazz, fieldID);
    }

    jint GetStaticIntField(jclass clazz, jfieldID fieldID) {
        return functions->GetStaticIntField != nullptr ? functions->GetStaticIntField(env_, clazz, fieldID) : env_->GetStaticIntField(clazz, fieldID);
    }

    jlong GetStaticLongField(jclass clazz, jfieldID fieldID) {
        return functions->GetStaticLongField != nullptr ? functions->GetStaticLongField(env_, clazz, fieldID) : env_->GetStaticLongField(clazz, fieldID);
    }

    jfloat GetStaticFloatField(jclass clazz, jfieldID fieldID) {
        return functions->GetStaticFloatField != nullptr ? functions->GetStaticFloatField(env_, clazz, fieldID) : env_->GetStaticFloatField(clazz, fieldID);
    }

    jdouble GetStaticDoubleField(jclass clazz, jfieldID fieldID) {
        return functions->GetStaticDoubleField != nullptr ? functions->GetStaticDoubleField(env_, clazz, fieldID) : env_->GetStaticDoubleField(clazz, fieldID);
    }

    void SetStaticObjectField(jclass clazz, jfieldID fieldID, jobject value) {
        functions->SetStaticObjectField != nullptr ? functions->SetStaticObjectField(env_, clazz, fieldID, value) : env_->SetStaticObjectField(clazz, fieldID, value);
    }

    void SetStaticBooleanField(jclass clazz, jfieldID fieldID, jboolean value) {
        functions->SetStaticBooleanField != nullptr ? functions->SetStaticBooleanField(env_, clazz, fieldID, value) : env_->SetStaticBooleanField(clazz, fieldID, value);
    }

    void SetStaticByteField(jclass clazz, jfieldID fieldID, jbyte value) {
        functions->SetStaticByteField != nullptr ? functions->SetStaticByteField(env_, clazz, fieldID, value) : env_->SetStaticByteField(clazz, fieldID, value);
    }

    void SetStaticCharField(jclass clazz, jfieldID fieldID, jchar value) {
        functions->SetStaticCharField != nullptr ? functions->SetStaticCharField(env_, clazz, fieldID, value) : env_->SetStaticCharField(clazz, fieldID, value);
    }

    void SetStaticShortField(jclass clazz, jfieldID fieldID, jshort value) {
        functions->SetStaticShortField != nullptr ? functions->SetStaticShortField(env_, clazz, fieldID, value) : env_->SetStaticShortField(clazz, fieldID, value);
    }

    void SetStaticIntField(jclass clazz, jfieldID fieldID, jint value) {
        functions->SetStaticIntField != nullptr ? functions->SetStaticIntField(env_, clazz, fieldID, value) : env_->SetStaticIntField(clazz, fieldID, value);
    }

    void SetStaticLongField(jclass clazz, jfieldID fieldID, jlong value) {
        functions->SetStaticLongField != nullptr ? functions->SetStaticLongField(env_, clazz, fieldID, value) : env_->SetStaticLongField(clazz, fieldID, value);
    }

    void SetStaticFloatField(jclass clazz, jfieldID fieldID, jfloat value) {
        functions->SetStaticFloatField != nullptr ? functions->SetStaticFloatField(env_, clazz, fieldID, value) : env_->SetStaticFloatField(clazz, fieldID, value);
    }

    void SetStaticDoubleField(jclass clazz, jfieldID fieldID, jdouble value) {
        functions->SetStaticDoubleField != nullptr ? functions->SetStaticDoubleField(env_, clazz, fieldID, value) : env_->SetStaticDoubleField(clazz, fieldID, value);
    }

    jstring NewString(const jchar *unicodeChars, jsize len) {
        return functions->NewString != nullptr ? functions->NewString(env_, unicodeChars, len) : env_->NewString(unicodeChars, len);
    }

    jsize GetStringLength(jstring string) { return functions->GetStringLength != nullptr ? functions->GetStringLength(env_, string) : env_->GetStringLength(string); }

    const jchar *GetStringChars(jstring string, jboolean *isCopy) {
        return functions->GetStringChars != nullptr ? functions->GetStringChars(env_, string, isCopy) : env_->GetStringChars(string, isCopy);
    }

    void ReleaseStringChars(jstring string, const jchar *chars) {
        functions->ReleaseStringChars != nullptr ? functions->ReleaseStringChars(env_, string, chars) : env_->ReleaseStringChars(string, chars);
    }

    jstring NewStringUTF(const char *bytes) { return functions->NewStringUTF != nullptr ? functions->NewStringUTF(env_, bytes) : env_->NewStringUTF(bytes); }

    jsize GetStringUTFLength(jstring string) { return functions->GetStringUTFLength != nullptr ? functions->GetStringUTFLength(env_, string) : env_->GetStringUTFLength(string); }

    const char *GetStringUTFChars(jstring string, jboolean *isCopy) {
        return functions->GetStringUTFChars != nullptr ? functions->GetStringUTFChars(env_, string, isCopy) : env_->GetStringUTFChars(string, isCopy);
    }

    void ReleaseStringUTFChars(jstring string, const char *utf) {
        functions->ReleaseStringUTFChars != nullptr ? functions->ReleaseStringUTFChars(env_, string, utf) : env_->ReleaseStringUTFChars(string, utf);
    }

    jsize GetArrayLength(jarray array) { return functions->GetArrayLength != nullptr ? functions->GetArrayLength(env_, array) : env_->GetArrayLength(array); }

    jobjectArray NewObjectArray(jsize length, jclass elementClass,jobject initialElement) {
        return functions->NewObjectArray != nullptr ? functions->NewObjectArray(env_, length, elementClass, initialElement) :env_->NewObjectArray(length, elementClass, initialElement);
    }

    jobject GetObjectArrayElement(jobjectArray array, jsize index) {
        return functions->GetObjectArrayElement != nullptr ? functions->GetObjectArrayElement(env_, array, index) : env_->GetObjectArrayElement(array, index);
    }

    void SetObjectArrayElement(jobjectArray array, jsize index, jobject value) {
        functions->SetObjectArrayElement != nullptr ? functions->SetObjectArrayElement(env_, array, index, value) : env_->SetObjectArrayElement(array, index, value);
    }

    jbooleanArray NewBooleanArray(jsize length) { return functions->NewBooleanArray != nullptr ? functions->NewBooleanArray(env_, length) : env_->NewBooleanArray(length); }

    jbyteArray NewByteArray(jsize length) { return functions->NewByteArray != nullptr ? functions->NewByteArray(env_, length) : env_->NewByteArray(length); }

    jcharArray NewCharArray(jsize length) { return functions->NewCharArray != nullptr ? functions->NewCharArray(env_, length) : env_->NewCharArray(length); }

    jshortArray NewShortArray(jsize length) { return functions->NewShortArray != nullptr ? functions->NewShortArray(env_, length) : env_->NewShortArray(length); }

    jintArray NewIntArray(jsize length) { return functions->NewIntArray != nullptr ? functions->NewIntArray(env_, length) : env_->NewIntArray(length); }

    jlongArray NewLongArray(jsize length) { return functions->NewLongArray != nullptr ? functions->NewLongArray(env_, length) : env_->NewLongArray(length); }

    jfloatArray NewFloatArray(jsize length) { return functions->NewFloatArray != nullptr ? functions->NewFloatArray(env_, length) : env_->NewFloatArray(length); }

    jdoubleArray NewDoubleArray(jsize length) { return functions->NewDoubleArray != nullptr ? functions->NewDoubleArray(env_, length) : env_->NewDoubleArray(length); }

    jboolean *GetBooleanArrayElements(jbooleanArray array, jboolean *isCopy) {
        return functions->GetBooleanArrayElements != nullptr ? functions->GetBooleanArrayElements(env_, array, isCopy) : env_->GetBooleanArrayElements(array, isCopy);
    }

    jbyte *GetByteArrayElements(jbyteArray array, jboolean *isCopy) {
        return functions->GetByteArrayElements != nullptr ? functions->GetByteArrayElements(env_, array, isCopy) : env_->GetByteArrayElements(array, isCopy);
    }

    jchar *GetCharArrayElements(jcharArray array, jboolean *isCopy) {
        return functions->GetCharArrayElements != nullptr ? functions->GetCharArrayElements(env_, array, isCopy) : env_->GetCharArrayElements(array, isCopy);
    }

    jshort *GetShortArrayElements(jshortArray array, jboolean *isCopy) {
        return functions->GetShortArrayElements != nullptr ? functions->GetShortArrayElements(env_, array, isCopy) : env_->GetShortArrayElements(array, isCopy);
    }

    jint *GetIntArrayElements(jintArray array, jboolean *isCopy) {
        return functions->GetIntArrayElements != nullptr ? functions->GetIntArrayElements(env_, array, isCopy) : env_->GetIntArrayElements(array, isCopy);
    }

    jlong *GetLongArrayElements(jlongArray array, jboolean *isCopy) {
        return functions->GetLongArrayElements != nullptr ? functions->GetLongArrayElements(env_, array, isCopy) : env_->GetLongArrayElements(array, isCopy);
    }

    jfloat *GetFloatArrayElements(jfloatArray array, jboolean *isCopy) {
        return functions->GetFloatArrayElements != nullptr ? functions->GetFloatArrayElements(env_, array, isCopy) : env_->GetFloatArrayElements(array, isCopy);
    }

    jdouble *GetDoubleArrayElements(jdoubleArray array, jboolean *isCopy) {
        return functions->GetDoubleArrayElements != nullptr ? functions->GetDoubleArrayElements(env_, array, isCopy) : env_->GetDoubleArrayElements(array, isCopy);
    }

    void ReleaseBooleanArrayElements(jbooleanArray array, jboolean *elems,
                                     jint mode) {
        functions->ReleaseBooleanArrayElements != nullptr ? functions->ReleaseBooleanArrayElements(env_, array, elems, mode) : env_->ReleaseBooleanArrayElements(array, elems,
                                                                                                                                                                 mode);
    }

    void ReleaseByteArrayElements(jbyteArray array, jbyte *elems,
                                  jint mode) {
        functions->ReleaseByteArrayElements != nullptr ? functions->ReleaseByteArrayElements(env_, array, elems, mode) : env_->ReleaseByteArrayElements(array, elems, mode);
    }

    void ReleaseCharArrayElements(jcharArray array, jchar *elems,
                                  jint mode) {
        functions->ReleaseCharArrayElements != nullptr ? functions->ReleaseCharArrayElements(env_, array, elems, mode) : env_->ReleaseCharArrayElements(array, elems, mode);
    }

    void ReleaseShortArrayElements(jshortArray array, jshort *elems,
                                   jint mode) {
        functions->ReleaseShortArrayElements != nullptr ? functions->ReleaseShortArrayElements(env_, array, elems, mode) : env_->ReleaseShortArrayElements(array, elems, mode);
    }

    void ReleaseIntArrayElements(jintArray array, jint *elems,
                                 jint mode) {
        functions->ReleaseIntArrayElements != nullptr ? functions->ReleaseIntArrayElements(env_, array, elems, mode) : env_->ReleaseIntArrayElements(array, elems, mode);
    }

    void ReleaseLongArrayElements(jlongArray array, jlong *elems,
                                  jint mode) {
        functions->ReleaseLongArrayElements != nullptr ? functions->ReleaseLongArrayElements(env_, array, elems, mode) : env_->ReleaseLongArrayElements(array, elems, mode);
    }

    void ReleaseFloatArrayElements(jfloatArray array, jfloat *elems,
                                   jint mode) {
        functions->ReleaseFloatArrayElements != nullptr ? functions->ReleaseFloatArrayElements(env_, array, elems, mode) : env_->ReleaseFloatArrayElements(array, elems, mode);
    }

    void ReleaseDoubleArrayElements(jdoubleArray array, jdouble *elems,
                                    jint mode) {
        functions->ReleaseDoubleArrayElements != nullptr ? functions->ReleaseDoubleArrayElements(env_, array, elems, mode) : env_->ReleaseDoubleArrayElements(array, elems, mode);
    }

    void GetBooleanArrayRegion(jbooleanArray array, jsize start, jsize len,
                               jboolean *buf) {
        functions->GetBooleanArrayRegion != nullptr ? functions->GetBooleanArrayRegion(env_, array, start, len, buf) : env_->GetBooleanArrayRegion(array, start, len, buf);
    }

    void GetByteArrayRegion(jbyteArray array, jsize start, jsize len,
                            jbyte *buf) {
        functions->GetByteArrayRegion != nullptr ? functions->GetByteArrayRegion(env_, array, start, len, buf) : env_->GetByteArrayRegion(array, start, len, buf);
    }

    void GetCharArrayRegion(jcharArray array, jsize start, jsize len,
                            jchar *buf) {
        functions->GetCharArrayRegion != nullptr ? functions->GetCharArrayRegion(env_, array, start, len, buf) : env_->GetCharArrayRegion(array, start, len, buf);
    }

    void GetShortArrayRegion(jshortArray array, jsize start, jsize len,
                             jshort *buf) {
        functions->GetShortArrayRegion != nullptr ? functions->GetShortArrayRegion(env_, array, start, len, buf) : env_->GetShortArrayRegion(array, start, len, buf);
    }

    void GetIntArrayRegion(jintArray array, jsize start, jsize len,
                           jint *buf) {
        functions->GetIntArrayRegion != nullptr ? functions->GetIntArrayRegion(env_, array, start, len, buf) : env_->GetIntArrayRegion(array, start, len, buf);
    }

    void GetLongArrayRegion(jlongArray array, jsize start, jsize len,
                            jlong *buf) {
        functions->GetLongArrayRegion != nullptr ? functions->GetLongArrayRegion(env_, array, start, len, buf) : env_->GetLongArrayRegion(array, start, len, buf);
    }

    void GetFloatArrayRegion(jfloatArray array, jsize start, jsize len,
                             jfloat *buf) {
        functions->GetFloatArrayRegion != nullptr ? functions->GetFloatArrayRegion(env_, array, start, len, buf) : env_->GetFloatArrayRegion(array, start, len, buf);
    }

    void GetDoubleArrayRegion(jdoubleArray array, jsize start, jsize len,
                              jdouble *buf) {
        functions->GetDoubleArrayRegion != nullptr ? functions->GetDoubleArrayRegion(env_, array, start, len, buf) : env_->GetDoubleArrayRegion(array, start, len, buf);
    }

    void SetBooleanArrayRegion(jbooleanArray array, jsize start, jsize len,
                               const jboolean *buf) {
        functions->SetBooleanArrayRegion != nullptr ? functions->SetBooleanArrayRegion(env_, array, start, len, buf) : env_->SetBooleanArrayRegion(array, start, len, buf);
    }

    void SetByteArrayRegion(jbyteArray array, jsize start, jsize len,
                            const jbyte *buf) {
        functions->SetByteArrayRegion != nullptr ? functions->SetByteArrayRegion(env_, array, start, len, buf) : env_->SetByteArrayRegion(array, start, len, buf);
    }

    void SetCharArrayRegion(jcharArray array, jsize start, jsize len,
                            const jchar *buf) {
        functions->SetCharArrayRegion != nullptr ? functions->SetCharArrayRegion(env_, array, start, len, buf) : env_->SetCharArrayRegion(array, start, len, buf);
    }

    void SetShortArrayRegion(jshortArray array, jsize start, jsize len,
                             const jshort *buf) {
        functions->SetShortArrayRegion != nullptr ? functions->SetShortArrayRegion(env_, array, start, len, buf) : env_->SetShortArrayRegion(array, start, len, buf);
    }

    void SetIntArrayRegion(jintArray array, jsize start, jsize len,
                           const jint *buf) {
        functions->SetIntArrayRegion != nullptr ? functions->SetIntArrayRegion(env_, array, start, len, buf) : env_->SetIntArrayRegion(array, start, len, buf);
    }

    void SetLongArrayRegion(jlongArray array, jsize start, jsize len,
                            const jlong *buf) {
        functions->SetLongArrayRegion != nullptr ? functions->SetLongArrayRegion(env_, array, start, len, buf) : env_->SetLongArrayRegion(array, start, len, buf);
    }

    void SetFloatArrayRegion(jfloatArray array, jsize start, jsize len,
                             const jfloat *buf) {
        functions->SetFloatArrayRegion != nullptr ? functions->SetFloatArrayRegion(env_, array, start, len, buf) : env_->SetFloatArrayRegion(array, start, len, buf);
    }

    void SetDoubleArrayRegion(jdoubleArray array, jsize start, jsize len,
                              const jdouble *buf) {
        functions->SetDoubleArrayRegion != nullptr ? functions->SetDoubleArrayRegion(env_, array, start, len, buf) : env_->SetDoubleArrayRegion(array, start, len, buf);
    }

    jint RegisterNatives(jclass clazz, const JNINativeMethod *methods,
                         jint nMethods) {
        return functions->RegisterNatives != nullptr ? functions->RegisterNatives(env_, clazz, methods, nMethods) : env_->RegisterNatives(clazz, methods, nMethods);
    }

    jint UnregisterNatives(jclass clazz) { return functions->UnregisterNatives != nullptr ? functions->UnregisterNatives(env_, clazz) : env_->UnregisterNatives(clazz); }

    jint MonitorEnter(jobject obj) { return functions->MonitorEnter != nullptr ? functions->MonitorEnter(env_, obj) : env_->MonitorEnter(obj); }

    jint MonitorExit(jobject obj) { return functions->MonitorExit != nullptr ? functions->MonitorExit(env_, obj) : env_->MonitorExit(obj); }

    jint GetJavaVM(JavaVM **vm) { return functions->GetJavaVM != nullptr ? functions->GetJavaVM(env_, vm) : env_->GetJavaVM(vm); }

    void GetStringRegion(jstring str, jsize start, jsize len, jchar *buf) {
        functions->GetStringRegion != nullptr ? functions->GetStringRegion(env_, str, start, len, buf) : env_->GetStringRegion(str, start, len, buf);
    }

    void GetStringUTFRegion(jstring str, jsize start, jsize len, char *buf) {
        return functions->GetStringUTFRegion != nullptr ? functions->GetStringUTFRegion(env_, str, start, len, buf) : env_->GetStringUTFRegion(str, start, len, buf);
    }

    void *GetPrimitiveArrayCritical(jarray array, jboolean *isCopy) {
        return functions->GetPrimitiveArrayCritical != nullptr ? functions->GetPrimitiveArrayCritical(env_, array, isCopy) : env_->GetPrimitiveArrayCritical(array, isCopy);
    }

    void ReleasePrimitiveArrayCritical(jarray array, void *carray, jint mode) {
        functions->ReleasePrimitiveArrayCritical != nullptr ? functions->ReleasePrimitiveArrayCritical(env_, array, carray, mode) : env_->ReleasePrimitiveArrayCritical(array,
                                                                                                                                                                        carray,
                                                                                                                                                                        mode);
    }

    const jchar *GetStringCritical(jstring string, jboolean *isCopy) {
        return functions->GetStringCritical != nullptr ? functions->GetStringCritical(env_, string, isCopy) : env_->GetStringCritical(string, isCopy);
    }

    void ReleaseStringCritical(jstring string, const jchar *carray) {
        functions->ReleaseStringCritical != nullptr ? functions->ReleaseStringCritical(env_, string, carray) : env_->ReleaseStringCritical(string, carray);
    }

    jweak NewWeakGlobalRef(jobject obj) { return functions->NewWeakGlobalRef != nullptr ? functions->NewWeakGlobalRef(env_, obj) : env_->NewWeakGlobalRef(obj); }

    void DeleteWeakGlobalRef(jweak obj) { functions->DeleteWeakGlobalRef != nullptr ? functions->DeleteWeakGlobalRef(env_, obj) : env_->DeleteWeakGlobalRef(obj); }

    jboolean ExceptionCheck() { return functions->ExceptionCheck != nullptr ? functions->ExceptionCheck(env_) : env_->ExceptionCheck(); }

    jobject NewDirectByteBuffer(void *address, jlong capacity) {
        return functions->NewDirectByteBuffer != nullptr ? functions->NewDirectByteBuffer(env_, address, capacity) : env_->NewDirectByteBuffer(address, capacity);
    }

    void *GetDirectBufferAddress(jobject buf) {
        return functions->GetDirectBufferAddress != nullptr ? functions->GetDirectBufferAddress(env_, buf) : env_->GetDirectBufferAddress(buf);
    }

    jlong GetDirectBufferCapacity(jobject buf) {
        return functions->GetDirectBufferCapacity != nullptr ? functions->GetDirectBufferCapacity(env_, buf) : env_->GetDirectBufferCapacity(buf);
    }

    /* added in JNI 1.6 */
    jobjectRefType GetObjectRefType(jobject obj) { return functions->GetObjectRefType != nullptr ? functions->GetObjectRefType(env_, obj) : env_->GetObjectRefType(obj); }

public:
    explicit ProxyJNIEnv(JNIEnv *env) : env_(env) {
        functions = original_functions;
    }

    explicit ProxyJNIEnv(JNIEnv *env, JNINativeInterface *interface) : env_(env), functions(interface) {}

    void Env(JNIEnv *env) {
        env_ = env;
    }

private:
    JNIEnv *env_;
    JNINativeInterface *functions;
};

