//
// Created by beich on 2020/12/18.
//

#include <map>
#include <mutex>
#include <inttypes.h>
#include "hook_jni_native_interface.h"
#include "../common/jni_helper.h"

#ifndef NELEM
#define NELEM(x) ((int) (sizeof(x) / sizeof((x)[0])))
#endif
#define LOG(format, ...) LOGD("[JNICall %s(%p)] "#format, __FUNCTION__, __builtin_return_address(0), __VA_ARGS__)

static JNINativeInterface original_jni;

JNINativeInterface *original_functions = &original_jni;
JNIInterfaceMonitor *JNIInterfaceMonitor::singleton = nullptr;

#define HOOK_NATIVE_DEF(ret, func, ...) \
    static ret HookJni##func(__VA_ARGS__) \

#define HOOK_METHOD_ITEM(name) \
    {offsetof(JNINativeInterface,name), reinterpret_cast<void *>(HookJni##name), reinterpret_cast<void **>(&original_jni.name)}

static bool IsMonitor(uintptr_t addr) {
    return JNIInterfaceMonitor::Get()->InMonitoring(addr);
}

#define HOOK_NATIVE_CALL_TYPE_METHOD_DEF(_jtype, _jname)                                                                        \
    static _jtype HookJniCall##_jname##Method(JNIEnv *env, jobject obj, jmethodID methodID, ...)                                \
    {                                                                                                                           \
        _jtype result;                                                                                                          \
        va_list args;                                                                                                           \
        va_start(args, methodID);                                                                                               \
        result = original_functions->Call##_jname##MethodV(env, obj, methodID, args);                                           \
        va_end(args);                                                                                                           \
        if (IsMonitor((uintptr_t) __builtin_return_address(0))) {                                                               \
            if(env->ExceptionCheck()){                                                                                          \
                return result;                                                                                                  \
            }                                                                                                                   \
            LOG("class: %s, methodId: %s, result: %s", JNIHelper::GetObjectClassName(env, obj).c_str(),                         \
            JNIHelper::ToString(env, methodID).c_str(), JNIHelper::ToString(env, result).c_str());                              \
        }                                                                                                                       \
        return result;                                                                                                          \
    }                                                                                                                           \
    static _jtype HookJniCallStatic##_jname##Method(JNIEnv *env, jclass clazz, jmethodID methodID, ...)                         \
    {                                                                                                                           \
        _jtype result;                                                                                                          \
        va_list args;                                                                                                           \
        va_start(args, methodID);                                                                                               \
        result = original_functions->CallStatic##_jname##MethodV(env, clazz, methodID, args);                                   \
        va_end(args);                                                                                                           \
        if (IsMonitor((uintptr_t) __builtin_return_address(0))) {                                                               \
            if(env->ExceptionCheck()){                                                                                          \
                return result;                                                                                                  \
            }                                                                                                                   \
            LOG("class: %s, methodId: %s, result: %s", JNIHelper::GetObjectClassName(env, clazz).c_str(),                       \
            JNIHelper::ToString(env, methodID).c_str(), JNIHelper::ToString(env, result).c_str());                              \
        }                                                                                                                       \
        return result;                                                                                                          \
    }

#define HOOK_NATIVE_CALL_TYPE_METHODV_DEF(_jtype, _jname)                                                                       \
    static _jtype HookJniCall##_jname##MethodV(JNIEnv *env, jobject obj, jmethodID methodID, va_list args)                      \
    {                                                                                                                           \
        _jtype result = original_functions->Call##_jname##MethodV(env, obj, methodID, args);                                    \
        if (IsMonitor((uintptr_t) __builtin_return_address(0))) {                                                               \
            if(env->ExceptionCheck()){                                                                                          \
                return result;                                                                                                  \
            }                                                                                                                   \
            LOG("class: %s, methodId: %s, result: %s", JNIHelper::GetObjectClassName(env, obj).c_str(),                         \
            JNIHelper::ToString(env, methodID).c_str(), JNIHelper::ToString(env, result).c_str());                              \
        }                                                                                                                       \
        return result;                                                                                                          \
    }                                                                                                                           \
    static _jtype HookJniCallStatic##_jname##MethodV(JNIEnv *env, jclass clazz, jmethodID methodID, va_list args)               \
    {                                                                                                                           \
        _jtype result = original_functions->CallStatic##_jname##MethodV(env, clazz, methodID, args);                            \
        if (IsMonitor((uintptr_t) __builtin_return_address(0))) {                                                               \
            if(env->ExceptionCheck()){                                                                                          \
                return result;                                                                                                  \
            }                                                                                                                   \
            LOG("class: %s, methodId: %s, result: %s", JNIHelper::GetObjectClassName(env, clazz).c_str(),                       \
            JNIHelper::ToString(env, methodID).c_str(), JNIHelper::ToString(env, result).c_str());                              \
        }                                                                                                                       \
        return result;                                                                                                          \
    }

#define HOOK_NATIVE_CALL_TYPE_METHODA_DEF(_jtype, _jname)                                                                       \
    static _jtype HookJniCall##_jname##MethodA(JNIEnv *env, jobject obj, jmethodID methodID, const jvalue* args)                \
    {                                                                                                                           \
        _jtype result = original_functions->Call##_jname##MethodA(env, obj, methodID, args);                                    \
        if (IsMonitor((uintptr_t) __builtin_return_address(0))) {                                                               \
            if(env->ExceptionCheck()){                                                                                          \
                return result;                                                                                                  \
            }                                                                                                                   \
            LOG("class: %s, methodId: %s, result: %s", JNIHelper::GetObjectClassName(env, obj).c_str(),                         \
            JNIHelper::ToString(env, methodID).c_str(), JNIHelper::ToString(env, result).c_str());                              \
        }                                                                                                                       \
        return result;                                                                                                          \
    }                                                                                                                           \
    static _jtype HookJniCallStatic##_jname##MethodA(JNIEnv *env, jclass clazz, jmethodID methodID, const jvalue* args)         \
    {                                                                                                                           \
        _jtype result = original_functions->CallStatic##_jname##MethodA(env, clazz, methodID, args);                            \
        if (IsMonitor((uintptr_t) __builtin_return_address(0))) {                                                               \
            if(env->ExceptionCheck()){                                                                                          \
                return result;                                                                                                  \
            }                                                                                                                   \
            LOG("class: %s, methodId: %s, result: %s", JNIHelper::GetObjectClassName(env, clazz).c_str(),                       \
            JNIHelper::ToString(env, methodID).c_str(), JNIHelper::ToString(env, result).c_str());                              \
        }                                                                                                                       \
        return result;                                                                                                          \
    }

#define HOOK_NATIVE_CALL_TYPE_DEF(_jtype, _jname)                               \
    HOOK_NATIVE_CALL_TYPE_METHOD_DEF(_jtype, _jname)                            \
    HOOK_NATIVE_CALL_TYPE_METHODV_DEF(_jtype, _jname)                           \
    HOOK_NATIVE_CALL_TYPE_METHODA_DEF(_jtype, _jname)

#define HOOK_CALL_METHOD_ITEM(_jname)                                           \
    HOOK_METHOD_ITEM(Call##_jname##Method),                                     \
    HOOK_METHOD_ITEM(CallStatic##_jname##Method),                               \
    HOOK_METHOD_ITEM(Call##_jname##MethodV),                                    \
    HOOK_METHOD_ITEM(CallStatic##_jname##MethodV),                              \
    HOOK_METHOD_ITEM(Call##_jname##MethodA),                                    \
    HOOK_METHOD_ITEM(CallStatic##_jname##MethodA)

HOOK_NATIVE_CALL_TYPE_DEF(jobject, Object);

HOOK_NATIVE_CALL_TYPE_DEF(jboolean, Boolean);

HOOK_NATIVE_CALL_TYPE_DEF(jbyte, Byte);

HOOK_NATIVE_CALL_TYPE_DEF(jchar, Char);

HOOK_NATIVE_CALL_TYPE_DEF(jshort, Short);

HOOK_NATIVE_CALL_TYPE_DEF(jint, Int);

HOOK_NATIVE_CALL_TYPE_DEF(jlong, Long);

HOOK_NATIVE_CALL_TYPE_DEF(jfloat, Float);

HOOK_NATIVE_CALL_TYPE_DEF(jdouble, Double);

// CallNonvirtual系列使用较少,暂不监听

#define HOOK_NATIVE_FIELD_TYPE_DEF(_jtype, _jname)                                                                          \
    static _jtype HookJniGet##_jname##Field(JNIEnv *env, jobject obj, jfieldID fieldID)                                     \
    {                                                                                                                       \
        _jtype result = original_functions->Get##_jname##Field(env, obj, fieldID);                                          \
        if (IsMonitor((uintptr_t) __builtin_return_address(0))) {                                                           \
            if(env->ExceptionCheck()){                                                                                      \
                return result;                                                                                              \
            }                                                                                                               \
            LOG("class: %s, field: %s, result: %s", JNIHelper::GetObjectClassName(env, obj).c_str(),                        \
            JNIHelper::ToString(env, fieldID).c_str(), JNIHelper::ToString(env, result).c_str());                           \
        }                                                                                                                   \
        return result;                                                                                                      \
    }                                                                                                                       \
    static _jtype HookJniGetStatic##_jname##Field(JNIEnv *env, jclass clazz, jfieldID fieldID)                              \
    {                                                                                                                       \
        _jtype result = original_functions->GetStatic##_jname##Field(env, clazz, fieldID);                                  \
        if (IsMonitor((uintptr_t) __builtin_return_address(0))) {                                                           \
            if(env->ExceptionCheck()){                                                                                      \
                return result;                                                                                              \
            }                                                                                                               \
            LOG("class: %s, field: %s, result: %s",JNIHelper::GetClassName(env, clazz).c_str(),                             \
            JNIHelper::ToString(env, fieldID).c_str(), JNIHelper::ToString(env, result).c_str());                           \
        }                                                                                                                   \
        return result;                                                                                                      \
    }                                                                                                                       \
    static void HookJniSet##_jname##Field(JNIEnv *env, jobject obj, jfieldID fieldID, _jtype value)                         \
    {                                                                                                                       \
        if (IsMonitor((uintptr_t) __builtin_return_address(0))) {                                                           \
            LOG("class: %s, field: %s, value: %s", JNIHelper::GetObjectClassName(env, obj).c_str(),                         \
            JNIHelper::ToString(env, fieldID).c_str(), JNIHelper::ToString(env, value).c_str());                            \
        }                                                                                                                   \
        original_functions->Set##_jname##Field(env, obj, fieldID, value);                                                   \
    }                                                                                                                       \
    static void HookJniSetStatic##_jname##Field(JNIEnv *env, jclass clazz, jfieldID fieldID, _jtype value)                  \
    {                                                                                                                       \
        if (IsMonitor((uintptr_t) __builtin_return_address(0))) {                                                           \
            LOG("class: %s, field: %s, value: %s", JNIHelper::GetClassName(env, clazz).c_str(),                             \
            JNIHelper::ToString(env, fieldID).c_str(),  JNIHelper::ToString(env, value).c_str());                           \
        }                                                                                                                   \
        original_functions->SetStatic##_jname##Field(env, clazz, fieldID, value);                                           \
    }

#define HOOK_CALL_FIELD_ITEM(_jname)                       \
    HOOK_METHOD_ITEM(Get##_jname##Field),                  \
    HOOK_METHOD_ITEM(GetStatic##_jname##Field),            \
    HOOK_METHOD_ITEM(Set##_jname##Field),                  \
    HOOK_METHOD_ITEM(SetStatic##_jname##Field)

HOOK_NATIVE_FIELD_TYPE_DEF(jobject, Object);

HOOK_NATIVE_FIELD_TYPE_DEF(jboolean, Boolean);

HOOK_NATIVE_FIELD_TYPE_DEF(jbyte, Byte);

HOOK_NATIVE_FIELD_TYPE_DEF(jchar, Char);

HOOK_NATIVE_FIELD_TYPE_DEF(jshort, Short);

HOOK_NATIVE_FIELD_TYPE_DEF(jint, Int);

HOOK_NATIVE_FIELD_TYPE_DEF(jlong, Long);

HOOK_NATIVE_FIELD_TYPE_DEF(jfloat, Float);

HOOK_NATIVE_FIELD_TYPE_DEF(jdouble, Double);

HOOK_NATIVE_DEF(jint, RegisterNatives, JNIEnv *env, jclass c, const JNINativeMethod *methods, jint nMethods) {
    LOG("start register native function %p", __builtin_return_address(0));
    jint ret = original_functions->RegisterNatives(env, c, methods, nMethods);
    if (ret != JNI_ERR && !original_functions->ExceptionCheck(env)) {
        std::string cls = JNIHelper::GetClassName(env, c);
        for (int i = 0; i < nMethods; ++i) {
            LOG("native register class: %s, method name: %s, function signature: %s, register address: %p",
                cls.c_str(), methods[i].name, methods[i].signature, methods[i].fnPtr);
        }
    }
    return ret;
}

HOOK_NATIVE_DEF(void, CallVoidMethod, JNIEnv *env, jobject obj, jmethodID methodID, ...) {
    va_list args;
    va_start(args, methodID);
    original_functions->CallVoidMethodV(env, obj, methodID, args);
    va_end(args);
    if (IsMonitor((uintptr_t) __builtin_return_address(0))) {
        if (original_functions->ExceptionCheck(env)) {
            return;
        }
        LOG("class: %s, methodId: %s", JNIHelper::GetObjectClassName(env, obj).c_str(), JNIHelper::ToString(env, methodID).c_str());
    }
}

HOOK_NATIVE_DEF(void, CallVoidMethodV, JNIEnv *env, jobject obj, jmethodID methodID, va_list args) {
    original_functions->CallVoidMethodV(env, obj, methodID, args);
    if (IsMonitor((uintptr_t) __builtin_return_address(0))) {
        if (original_functions->ExceptionCheck(env)) {
            return;
        }
        LOG("class: %s, methodId: %s", JNIHelper::GetObjectClassName(env, obj).c_str(), JNIHelper::ToString(env, methodID).c_str());
    }
}

HOOK_NATIVE_DEF(void, CallVoidMethodA, JNIEnv *env, jobject obj, jmethodID methodID, const jvalue *args) {
    original_functions->CallVoidMethodA(env, obj, methodID, args);
    if (IsMonitor((uintptr_t) __builtin_return_address(0))) {
        if (original_functions->ExceptionCheck(env)) {
            return;
        }
        LOG("class: %s, methodId: %s", JNIHelper::GetObjectClassName(env, obj).c_str(), JNIHelper::ToString(env, methodID).c_str());
    }
}

HOOK_NATIVE_DEF(void, CallStaticVoidMethod, JNIEnv *env, jclass clazz, jmethodID methodID, ...) {
    va_list args;
    va_start(args, methodID);
    original_functions->CallStaticVoidMethodV(env, clazz, methodID, args);
    va_end(args);
    if (IsMonitor((uintptr_t) __builtin_return_address(0))) {
        if (original_functions->ExceptionCheck(env)) {
            return;
        }
        LOG("class: %s, methodId: %s", JNIHelper::GetClassName(env, clazz).c_str(), JNIHelper::ToString(env, methodID).c_str());
    }
}

HOOK_NATIVE_DEF(void, CallStaticVoidMethodV, JNIEnv *env, jclass clazz, jmethodID methodID, va_list args) {
    original_functions->CallStaticVoidMethodV(env, clazz, methodID, args);
    if (IsMonitor((uintptr_t) __builtin_return_address(0))) {
        if (original_functions->ExceptionCheck(env)) {
            return;
        }
        LOG("class: %s, methodId: %s", JNIHelper::GetClassName(env, clazz).c_str(), JNIHelper::ToString(env, methodID).c_str());
    }
}

HOOK_NATIVE_DEF(void, CallStaticVoidMethodA, JNIEnv *env, jclass clazz, jmethodID methodID, const jvalue *args) {
    original_functions->CallStaticVoidMethodA(env, clazz, methodID, args);
    if (IsMonitor((uintptr_t) __builtin_return_address(0))) {
        if (original_functions->ExceptionCheck(env)) {
            return;
        }
        LOG("class: %s, methodId: %s", JNIHelper::GetClassName(env, clazz).c_str(), JNIHelper::ToString(env, methodID).c_str());
    }
}

HOOK_NATIVE_DEF(jclass, FindClass, JNIEnv *env, const char *name) {
    jclass result = original_functions->FindClass(env, name);
    if (IsMonitor((uintptr_t) __builtin_return_address(0))) {
        if (original_functions->ExceptionCheck(env)) {
            return result;
        }
        LOG("class name: %s, result: %p", name, result);
    }
    return result;
}


HOOK_NATIVE_DEF(jfieldID, GetFieldID, JNIEnv *env, jclass clazz, const char *name, const char *sig) {
    jfieldID result = original_functions->GetFieldID(env, clazz, name, sig);
    if (IsMonitor((uintptr_t) __builtin_return_address(0))) {
        if (original_functions->ExceptionCheck(env)) {
            return result;
        }
        LOG("class id: %s, field name: %s, sig: %s, result: %p", JNIHelper::GetClassName(env, clazz).c_str(), name, sig, result);
    }
    return result;
}

HOOK_NATIVE_DEF(jfieldID, GetStaticFieldID, JNIEnv *env, jclass clazz, const char *name, const char *sig) {
    jfieldID result = original_functions->GetStaticFieldID(env, clazz, name, sig);
    if (IsMonitor((uintptr_t) __builtin_return_address(0))) {
        if (original_functions->ExceptionCheck(env)) {
            return result;
        }
        LOG("class id: %s, field name: %s, sig: %s, result: %p", JNIHelper::GetClassName(env, clazz).c_str(), name, sig, result);
    }
    return result;
}

HOOK_NATIVE_DEF(jmethodID, GetMethodID, JNIEnv *env, jclass clazz, const char *name, const char *sig) {
    jmethodID result = original_functions->GetMethodID(env, clazz, name, sig);
    if (IsMonitor((uintptr_t) __builtin_return_address(0))) {
        if (original_functions->ExceptionCheck(env)) {
            return result;
        }
        LOG("class id: %s, method name: %s, sig: %s, result: %p", JNIHelper::GetClassName(env, clazz).c_str(), name, sig, result);
    }
    return result;
}

HOOK_NATIVE_DEF(jmethodID, GetStaticMethodID, JNIEnv *env, jclass clazz, const char *name, const char *sig) {
    jmethodID result = original_functions->GetStaticMethodID(env, clazz, name, sig);
    if (IsMonitor((uintptr_t) __builtin_return_address(0))) {
        if (original_functions->ExceptionCheck(env)) {
            return result;
        }
        LOG("class id: %s, method name: %s, sig: %s, result: %p", JNIHelper::GetClassName(env, clazz).c_str(), name, sig, result);
    }
    return result;
}

HOOK_NATIVE_DEF(jobject, NewGlobalRef, JNIEnv *env, jobject obj) {
    jobject result = original_functions->NewGlobalRef(env, obj);
    return result;
}

static HookJniUnit hooks[] = {
        HOOK_METHOD_ITEM(RegisterNatives),
        HOOK_METHOD_ITEM(FindClass),
        HOOK_METHOD_ITEM(GetFieldID),
        HOOK_METHOD_ITEM(GetStaticFieldID),
        HOOK_METHOD_ITEM(GetMethodID),
        HOOK_METHOD_ITEM(GetStaticMethodID),

        HOOK_CALL_METHOD_ITEM(Object),
        HOOK_CALL_METHOD_ITEM(Boolean),
        HOOK_CALL_METHOD_ITEM(Byte),
        HOOK_CALL_METHOD_ITEM(Char),
        HOOK_CALL_METHOD_ITEM(Short),
        HOOK_CALL_METHOD_ITEM(Int),
        HOOK_CALL_METHOD_ITEM(Long),
        HOOK_CALL_METHOD_ITEM(Float),
        HOOK_CALL_METHOD_ITEM(Double),
        HOOK_CALL_METHOD_ITEM(Void),

        HOOK_CALL_FIELD_ITEM(Object),
        HOOK_CALL_FIELD_ITEM(Boolean),
        HOOK_CALL_FIELD_ITEM(Byte),
        HOOK_CALL_FIELD_ITEM(Char),
        HOOK_CALL_FIELD_ITEM(Short),
        HOOK_CALL_FIELD_ITEM(Int),
        HOOK_CALL_FIELD_ITEM(Long),
        HOOK_CALL_FIELD_ITEM(Float),
        HOOK_CALL_FIELD_ITEM(Double),
};

void JNIInterfaceMonitor::AddLibraryMonitor(const char *name, bool contain) {
    int error_code;
    auto attr = static_cast<SoinfoAttribute *>(remote_->CallSoinfoFunction(kSFInquireAttr, kSPName, name, kSPNull, nullptr, &error_code));
    if (error_code != kErrorNo) {
        LOGE("not found special library name: %s", name);
        return;
    }
    LOGD("Add Jni monitor item so name: %s, address: 0x%" PRIuPTR " - 0x%" PRIuPTR ", contain: %d",
         attr->so_name, (uintptr_t) attr->base, (uintptr_t) (attr->base + attr->size), contain);
    AddAddressMonitor(attr->base, attr->base + attr->size, contain);
    delete attr;
}

void JNIInterfaceMonitor::RemoveLibraryMonitor(const char *name) {
    int error_code;
    auto attr = static_cast<SoinfoAttribute *>(remote_->CallSoinfoFunction(kSFInquireAttr, kSPName, name, kSPNull, nullptr, &error_code));
    if (error_code != kErrorNo) {
        LOGE("not found special library name: %s", name);
        return;
    }
    size_t end = attr->base + attr->size;
    RemoveAddressMonitor(attr->base, attr->base + attr->size);
    delete attr;
}

void JNIInterfaceMonitor::AddAddressMonitor(uintptr_t start, size_t end, bool contain) {
    if (end < start) {
        return;
    }
    if (contain != contain_) {
        contain_ = contain;
        monitors_.clear();
    }
    monitors_[start] = end;
}

void JNIInterfaceMonitor::RemoveAddressMonitor(uintptr_t start, size_t end){
    for (auto iter = monitors_.begin(); iter != monitors_.end(); iter++) {
        if (start == iter->first && end == iter->second) {
            monitors_.erase(iter);
            return;
        }
    }
}


bool JNIInterfaceMonitor::InMonitoring(uintptr_t addr) {
    bool result = !contain_;
    for (auto &monitor : monitors_) {
        if (addr >= monitor.first && addr <= monitor.second) {
            return !result;
        }
    }
    return result;
}

JNIInterfaceMonitor::JNIInterfaceMonitor(const RemoteInvokeInterface *remote, JNIEnv *env) : remote_(remote) {
    memcpy(&original_jni, env->functions, sizeof(JNINativeInterface));
    original_interface = &original_jni;
}

int JNIInterfaceMonitor::InitHookJNIInterfaces() {
    return remote_->HookJniNatives(hooks, NELEM(hooks));
}

JNIInterfaceMonitor *JNIInterfaceMonitor::Get() {
    CHECK(singleton);
    return singleton;
}

void JNIInterfaceMonitor::Init(const RemoteInvokeInterface *remote, JNIEnv *env) {
    if (singleton != nullptr) {
        return;
    }
    if (remote == nullptr) {
        LOGE("Remote jni native hook implement is null");
        return;
    }
    if (env == nullptr) {
        LOGE("JNIEnv is null");
        return;
    }
    singleton = new JNIInterfaceMonitor(remote, env);
    JNIHelper::Init(env);
}