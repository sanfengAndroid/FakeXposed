
#include "hook_main.h"

#include <libgen.h>
#include <sys/system_properties.h>
#include <unistd.h>
#include <map>
#include <elf.h>

#include <gtype.h>
#include <maps_util.h>
#include <variable_length_object.h>
#include <scoped_utf_chars.h>
#include <jni_helper.h>
#include <module_config.h>

#include "io_redirect.h"
#include "hook_jni_native_interface.h"
#include "hook_syscall.h"


#ifndef NELEM
#define NELEM(x) ((int) (sizeof(x) / sizeof((x)[0])))
#endif

#define NO_FOUND_INDEX -10000

static void *fake_linker_soinfo = nullptr;


static int api, pid;
int g_log_level = HOOK_LOG_LEVEL;
void *libc_handle;
void *self_soinfo;
void *libc_soinfo;
static bool hook_init = false;
const char *cache_dir = "/data/data/com.sanfengandroid.fakexposed/cache";
static const char *config_dir = "/data/system/sanfengandroid/fakexposed";

static const char *process_name;
static std::map<std::string, int> hook_option;
static std::map<std::string, std::string> string_option;

std::map<std::string, int> maps_rules;
static std::map<std::string, int> file_blacklist;
static std::map<std::string, int> file_path_blacklist;
static std::map<std::string, int> symbol_blacklist;
static std::map<std::string, std::string> properties;
const RemoteInvokeInterface *remote;
static struct {
    jclass nativeCall;
    jmethodID replaceEnv;
} java;

static JavaVM *jvm;

/*
 * 文件配置格式为 key=value, key必须为字符串,value可以是int,可以是字符串
 * */
enum HookConfig {
    // 是否开启拦截java层调用的execvp(Runtime.exec),只能屏蔽掉它,否者子进程永远不返回
    kHFJavaExecvp,
    // 在此之前是int类型值,在此之后是字符串类型值,内部使用
    kHFStringSeparation,
    // 全局屏蔽重定位符号名称
    kHFRelinkSymbolFilter,
    kHFEnd
};
const char *hook_config_name[] = {"java_execvp", "hook_string_separation", "relink_symbol_filter"};
/*
 * 在Java层已经验证了参数的合法性,但不能完全排除,C层应再次检测
 * */
enum ErrorJniReturn {
    // 没有错误
    kEJNo = 0,
    // 未找到某项配置
    kEJNotFound = 1,
    // 参数为空错误
    kEJParameterNull = 1 << 1,
    // 参数类型错误,如需要int而传入的是string
    kEJParameterType = 1 << 2,
    // 内部执行错误
    kEJExecute = 1 << 3
};

enum Option {
    // 关闭该功能
    kODisable,
    // 开启该功能
    kOOpen
};

// 全局库使用线性查找,可以把本库添加到solist头部,但是这样查找非常耗时,因此直接指定库名称查找最好
// Android 6.0 全局库拥有 RTLD_GLOBAL 标志无法使用 RTLD_NEXT 标志查找符号
// Android 5.1及以下使用 RTLD_NEXT 查找线性查找不成功直接返回null
// 可以使用库名称指定查找
void *FindLibcSymbolRealAddress(const char *name) {
    int error_code;
    void *result = remote->CallSoinfoFunction(kSFCallDlsym, kSPOriginal, libc_soinfo, kSPSymbol, name, &error_code);
    if (error_code == kErrorNo) {
        return result;
    } else {
        LOGE("ERROR: find symbol failed, symbol name: %s, error code: 0x%x", name, error_code);
    }
    return nullptr;
}

char **GetEnviron() {
    static char ***env_ptr = nullptr;
    if (env_ptr == nullptr) {
        env_ptr = static_cast<char ***>(FindLibcSymbolRealAddress("environ"));
        CHECK(env_ptr);
    }
    return *env_ptr;
}

static void InitPreFunction() {
    int error_code;
    libc_handle = remote->CallSoinfoFunction(kSFGetHandle, kSPName, "libc.so", kSPNull, nullptr, &error_code);
    CHECK(libc_handle);
    libc_soinfo = remote->CallSoinfoFunction(kSFInquire, kSPName, "libc.so", kSPNull, nullptr, &error_code);
    CHECK(libc_soinfo);
    self_soinfo = remote->CallSoinfoFunction(kSFInquire, kSPAddress, nullptr, kSPNull, nullptr, &error_code);
    CHECK(self_soinfo);

    LOGD("libc handle: %p, libc soinfo: %p, self soinfo: %p", libc_handle, libc_soinfo, self_soinfo);

}

static int GetHookConfigIndex(const char *name) {
    int index = NO_FOUND_INDEX;
    if (name == nullptr) {
        return index;
    }
    for (int i = 0; i < kHFEnd; ++i) {
        if (!strcmp(name, hook_config_name[i])) {
            index = i;
            break;
        }
    }
    if (index == kHFStringSeparation || index == kHFEnd) {
        index = NO_FOUND_INDEX;
    }
    return index;
}

static inline int FindMapIndex(std::map<std::string, int> &map, const char *name) {
    auto iter = map.find(name);
    if (iter != map.end()) {
        return iter->second;
    }
    return NO_FOUND_INDEX;
}

static inline const char *FindStringOption(const char *name) {
    auto iter = string_option.find(name);
    if (iter != string_option.end()) {
        return iter->second.c_str();
    }
    return nullptr;
}

static inline void ModifyIntMap(std::map<std::string, int> *map, const char *name, int value, bool add) {
    if (add) {
        if (map == &file_blacklist && name[0] == '/') {
            file_path_blacklist[name] = value;
        } else {
            (*map)[name] = value;
        }
        return;
    }
    if (map == &file_blacklist) {
        auto found = file_path_blacklist.find(name);
        if (found != file_path_blacklist.end()) {
            file_path_blacklist.erase(found);
            return;
        }
    }
    auto found = map->find(name);
    if (found != map->end()) {
        map->erase(found);
    }

}

static inline void ModifyStringMap(std::map<std::string, std::string> *map, const char *name, const char *value, bool add) {
    if (add) {
        (*map)[name] = value;
        return;
    }
    auto found = map->find(name);
    if (found != map->end()) {
        map->erase(found);
    }
}

bool FileNameIsBlacklisted(const char *path) {
    if (path == nullptr) {
        return false;
    }
    if (FindMapIndex(file_path_blacklist, path) == kOOpen) {
        return true;
    }
    char *name = basename(path);
    if (name == nullptr) {
        return false;
    }
    return FindMapIndex(file_blacklist, name) == kOOpen;
}

static std::map<std::string, char *> replace_env;

char *FindEnvReplace(const char *name, char *value) {
    auto iter = replace_env.find(name);
    if (iter != replace_env.end()) {
        return iter->second;
    }
    JNIEnv *env = nullptr;
    jvm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_4);
    if (env != nullptr) {
        ScopedLocalRef<jstring> _name(env, env->NewStringUTF(name));
        ScopedLocalRef<jstring> _value(env, env->NewStringUTF(value));
        ScopedUtfChars result(env, reinterpret_cast<jstring>(env->CallStaticObjectMethod(java.nativeCall, java.replaceEnv, _name.get(), _value.get())), false);
        // 这里由于jstring马上要被清理，因此必须复制字符串,这里缓存起来方便下次查找，避免造成内存泄露
        // 同时也会导致环境变量无法更新，但通常情况下环境变量是不会变的
        if (result.c_str() != nullptr) {
            char *copy = strdup(result.c_str());
            LOGD("cache replace environment name: %s orig value: %s, replace value: %s", name, value, copy);
            replace_env[name] = copy;
            return copy;
        }
    }
    return nullptr;
}


bool SymbolIsBlacklisted(const char *symbol) {
    if (symbol_blacklist.empty()) {
        return false;
    }
    return FindMapIndex(symbol_blacklist, symbol) == kOOpen;
}

bool PropertiesIsBlacklisted(const char *name) {
    auto found = properties.find(name);
    if (found != properties.end() && found->second.empty()) {
        LOGD("found blacklist property name: %s", name);
        return true;
    }
    return false;
}

const char *PropertiesReplace(const char *name, const char *value) {
    auto found = properties.find(name);
    if (found != properties.end() && !found->second.empty()) {
        LOGD("found filter property name: %s, old value: %s, new value: %s", name, value, found->second.c_str());
        return found->second.c_str();
    }
    return nullptr;
}

static int InitHook() {
    int error_code;

    remote->CallCommonFunction(kCFAddSoinfoToGlobal, kSPAddress, nullptr, kSPNull, nullptr, &error_code);
    if (error_code != kErrorNo) {
        LOGE("init global soinfo error, error code: %x", error_code);
        return error_code;
    }

    auto find_value = [&](int index) {
        auto iter = hook_option.find(hook_config_name[index]);
        if (iter != hook_option.end()) {
            return iter->second;
        }
        return NO_FOUND_INDEX;
    };

    if (find_value(kHFJavaExecvp) == kOOpen) {
        VarLengthObject<const char *> *libs = VaArgsToVarLengthObject<const char *>(8, "libjavacore.so", "libnativehelper.so", "libnativeloader.so",
                                                                                    "libart.so", "libopenjdk.so", "libopenjdkjvm.so",
                                                                                    "libandroid_runtime.so", "libcutils.so");
        remote->CallCommonFunction(kCFCallManualRelinks, kSPAddress, nullptr, kSPNames, libs, &error_code);
    } else {
        VarLengthObject<const char *> *libs = VaArgsToVarLengthObject<const char *>(7, "libjavacore.so", "libnativehelper.so", "libnativeloader.so",
                                                                                    "libart.so", "libopenjdkjvm.so", "libandroid_runtime.so",
                                                                                    "libcutils.so");
        remote->CallCommonFunction(kCFCallManualRelinks, kSPAddress, nullptr, kSPNames, libs, &error_code);

        remote->CallCommonFunction(kCFAddRelinkFilterSymbol, kSPSymbol, "execvp", kSPNull, nullptr, &error_code);
        remote->CallCommonFunction(kCFCallManualRelink, kSPAddress, nullptr, kSPName, "libopenjdk.so", &error_code);
        remote->CallCommonFunction(kCFRemoveRelinkFilterSymbol, kSPSymbol, "execvp", kSPNull, nullptr, &error_code);
    }
    LOGD("call manual relink library");
    char **env = GetEnviron();
    while (*env != nullptr) {
        LOGD("environments: %s", *env);
        env++;
    }
    return error_code;
}

static void InitDefaultHookConf() {
    // 默认关闭Java层execvp拦截
    hook_option[hook_config_name[kHFJavaExecvp]] = kODisable;
}

static int ModifyHookOption(const char *name, int int_value, const char *string_value) {
    int error_code = kEJNo;

    int index = GetHookConfigIndex(name);
    if (index == NO_FOUND_INDEX) {
        LOGE("Invalid configuration item, does not contain the name: %s", name);
        return kEJNotFound;
    }
    if (index == kHFStringSeparation || index == kHFEnd) {
        return kEJParameterType;
    }
    if (index < kHFStringSeparation) {
        hook_option[name] = int_value;
        return kEJNo;
    }
    if (index == kHFRelinkSymbolFilter) {
        remote->CallCommonFunction(kCFAddRelinkFilterSymbol, kSPSymbol, string_value, kSPNull, nullptr, &error_code);
        if (error_code != kErrorNo) {
            LOGE("add global relink filter symbol failed, symbol name: %s, error code: %d", string_value, error_code);
        } else {
            LOGD("add global relink filter symbol success, symbol name: %s", string_value);
        }
        return error_code;
    } else {
        string_option[name] = string_value;
    }
    return kErrorNo;
}

static void InitHookConfig() {
    if (hook_init) {
        return;
    }
    InitPreFunction();
    api = android_get_device_api_level();
    pid = getpid();
    IoRedirect::SetPid(pid);
    int error_code;
    const char *fake_name = static_cast<const char *>(remote->CallSoinfoFunction(kSFGetName, kSPOriginal, fake_linker_soinfo, kSPNull, nullptr, &error_code));
    if (error_code == kErrorNo) {
        maps_rules[fake_name] = kMapsRemove;
    }
    fake_name = static_cast<const char *>(remote->CallSoinfoFunction(kSFGetName, kSPAddress, nullptr, kSPNull, nullptr, &error_code));
    if (error_code == kErrorNo) {
        maps_rules[fake_name] = kMapsRemove;
    }
    InitDefaultHookConf();
    // syscall调用频繁,首先初始化
    get_orig_syscall();
    hook_init = true;
}

static jint NativeHook_AddIntBlackList(JNIEnv *env, jclass clazz, jint type, jstring name, jint value, jboolean add) {
    if (name == nullptr) {
        return kEJParameterNull;
    }
    ScopedUtfChars _name(env, name);
    std::map<std::string, int> *_map;

    switch (type) {
        case 0:
            _map = &file_blacklist;
            break;
        case 1:
            _map = &symbol_blacklist;
            break;
        case 2:
            _map = &maps_rules;
            break;
        default:
            return kEJParameterType;
    }
    ModifyIntMap(_map, _name.c_str(), value, add);
    return kEJNo;
}

static jint NativeHook_AddIntBlackLists(JNIEnv *env, jclass clazz, jint type, jobjectArray _names, jintArray _values, jboolean add) {
    jsize len = env->GetArrayLength(_names);
    jint *values = add ? env->GetIntArrayElements(_values, nullptr) : nullptr;
    if (add && values == nullptr) {
        LOGE("Cannot access value array");
        return kEJParameterNull;
    }
    for (int i = 0; i < len; ++i) {
        auto name = reinterpret_cast<jstring>(env->GetObjectArrayElement(_names, i));
        ScopedUtfChars _name(env, name);
        std::map<std::string, int> *_map;
        switch (type) {
            case 0:
                _map = &file_blacklist;
                break;
            case 1:
                _map = &symbol_blacklist;
                break;
            case 2:
                _map = &maps_rules;
                break;
            default:
                return kEJParameterType;
        }
        ModifyIntMap(_map, _name.c_str(), add ? values[i] : 0, add);
    }
    if (add) {
        env->ReleaseIntArrayElements(_values, values, 0);
    }
    return kEJNo;
}

static jint NativeHook_AddStringBlackList(JNIEnv *env, jclass clazz, jint type, jstring name, jstring value, jboolean add) {
    ScopedUtfChars _name(env, name);
    ScopedUtfChars _value(env, value, false);
    std::map<std::string, std::string> *_map;
    switch (type) {
        case 0:
            _map = &properties;
            break;
        default:
            return kEJParameterType;
    }
    LOGD("add string blacklist name: %s, value: %s", _name.c_str(), _value.c_str());
    ModifyStringMap(_map, _name.c_str(), _value.c_str() == nullptr ? "" : _value.c_str(), add);
    return kEJNo;
}

static jint NativeHook_SetHookOptionInt(JNIEnv *env, jclass clazz, jstring name, jint value) {
    if (name == nullptr) {
        return kEJParameterNull;
    }
    ScopedUtfChars name_(env, name);
    return ModifyHookOption(name_.c_str(), value, nullptr);
}

static jint NativeHook_SetHookOptionString(JNIEnv *env, jclass clazz, jstring name, jstring value) {
    if (name == nullptr || value == nullptr) {
        return kEJParameterNull;
    }
    ScopedUtfChars name_(env, name);
    ScopedUtfChars value_(env, value);

    return ModifyHookOption(name_.c_str(), 0, value_.c_str());
}

static void NativeHook_ClearBlackList(JNIEnv *env, jclass clazz, jint type) {
    std::map<std::string, int> *_map;

    switch (type) {
        case 0:
            _map = &file_blacklist;
            break;
        case 1:
            _map = &symbol_blacklist;
            break;
        case 2:
            _map = &maps_rules;
            break;
        default:
            return;
    }
    _map->clear();
}

static jint NativeHook_StartHook(JNIEnv *env, jclass clazz) {
    if (!hook_init) {
        return false;
    }
    int error_code = InitHook();
    return error_code;
}

static void NativeHook_ClearAll(JNIEnv *env, jclass clazz) {
    file_blacklist.clear();
    file_path_blacklist.clear();
    symbol_blacklist.clear();
    properties.clear();
    // maps 规则不清除，包含内置的屏蔽
//    maps_rules.clear();
}

static jint NativeHook_OpenJniMonitor(JNIEnv *env, jclass clazz, jboolean open) {
    if (open) {
        return JNIInterfaceMonitor::Get()->InitHookJNIInterfaces() != 0 ? kErrorNo : kErrorExec;
    }
    return kErrorNo;
}

static jint NativeHook_SetJniMonitorLib(JNIEnv *env, jclass clazz, jstring lib, jboolean contain, jboolean add) {
    ScopedUtfChars lib_(env, lib);
    if (add) {
        JNIInterfaceMonitor::Get()->AddLibraryMonitor(lib_.c_str(), contain);
    } else {
        JNIInterfaceMonitor::Get()->RemoveLibraryMonitor(lib_.c_str());
    }
    return kErrorNo;
}

static jint NativeHook_SetJniMonitorAddress(JNIEnv *env, jclass clazz, jlong start, jlong end, jboolean contain, jboolean add) {
    if (add) {
        JNIInterfaceMonitor::Get()->AddAddressMonitor(start, end, contain);
    } else {
        JNIInterfaceMonitor::Get()->RemoveAddressMonitor(start, end);
    }
    return kErrorNo;
}

static jboolean NativeHook_SetRedirectFile(JNIEnv *env, jclass clazz, jstring src_, jstring redirect_, jboolean dir) {
    ScopedUtfChars src(env, src_);
    ScopedUtfChars redirect(env, redirect_);
    bool result;
    result = IoRedirect::AddRedirect(src.c_str(), redirect.c_str(), dir);
    return result ? JNI_TRUE : JNI_FALSE;
}

static void NativeHook_RemoveRedirectFile(JNIEnv *env, jclass clazz, jstring src_, jboolean dir) {
    ScopedUtfChars src(env, src_);
    if (dir) {
        IoRedirect::DeleteRedirectDirectory(src.c_str());
    } else {
        IoRedirect::DeleteRedirectFile(src.c_str());
    }
}

static jboolean NativeHook_SetFileAccess(JNIEnv *env, jclass clazz, jstring path_, jint uid_, jint gid_, jint access) {
    ScopedUtfChars path(env, path_);
    return IoRedirect::AddFileAccess(path.c_str(), uid_, gid_, access);
}

static void NativeHook_RemoveFileAccess(JNIEnv *env, jclass clazz, jstring path_) {
    ScopedUtfChars path(env, path_);
    return IoRedirect::RemoveFileAccess(path.c_str());
}

/*
 * 默认导出无法查找到,因为在Java层调用System.load方法会将打开的库保存在 JavaVMExt std::unique_ptr<Libraries> libraries_ 中
 * 在C层调用不会添加到该队列中,因此根本不会查找到当前的库
 * */
static jint NativeHook_RelinkSpecialLibrary(JNIEnv *env, jclass clazz, jstring library_name) {
    ScopedUtfChars lib(env, library_name);
    int error_code;

    LOGD("Java invoke relink library: %s", lib.c_str());
    remote->CallCommonFunction(kCFCallManualRelink, kSPAddress, nullptr, kSPName, lib.c_str(), &error_code);
    if (error_code != kErrorNo) {
        LOGE("Relink library failed, library name: %s, error code: %d", lib.c_str(), error_code);
    }
    return error_code;
}

#define NATIVE_METHOD(className, functionName, signature) \
{ "native"#functionName, signature, (void*)(className ## _ ## functionName) }

static JNINativeMethod gMethods[] = {
        NATIVE_METHOD(NativeHook, AddIntBlackList, "(ILjava/lang/String;IZ)I"),
        NATIVE_METHOD(NativeHook, AddIntBlackLists, "(I[Ljava/lang/String;[IZ)I"),
        NATIVE_METHOD(NativeHook, AddStringBlackList, "(ILjava/lang/String;Ljava/lang/String;Z)I"),
        NATIVE_METHOD(NativeHook, SetHookOptionInt, "(Ljava/lang/String;I)I"),
        NATIVE_METHOD(NativeHook, SetHookOptionString, "(Ljava/lang/String;Ljava/lang/String;)I"),
        NATIVE_METHOD(NativeHook, ClearBlackList, "(I)V"),
        NATIVE_METHOD(NativeHook, StartHook, "()I"),
        NATIVE_METHOD(NativeHook, SetRedirectFile, "(Ljava/lang/String;Ljava/lang/String;Z)Z"),
        NATIVE_METHOD(NativeHook, RemoveRedirectFile, "(Ljava/lang/String;Z)V"),
        NATIVE_METHOD(NativeHook, SetFileAccess, "(Ljava/lang/String;III)Z"),
        NATIVE_METHOD(NativeHook, RemoveFileAccess, "(Ljava/lang/String;)V"),
        NATIVE_METHOD(NativeHook, RelinkSpecialLibrary, "(Ljava/lang/String;)I"),
        NATIVE_METHOD(NativeHook, OpenJniMonitor, "(Z)I"),
        NATIVE_METHOD(NativeHook, SetJniMonitorLib, "(Ljava/lang/String;ZZ)I"),
        NATIVE_METHOD(NativeHook, SetJniMonitorAddress, "(JJZZ)I"),
        NATIVE_METHOD(NativeHook, ClearAll, "()V")

};

int JniRegisterNativeMethods(JNIEnv *env, const char *className, const JNINativeMethod *methods, int numMethod) {
    LOGD("Registering %s's %d native methods...", className, numMethod);
    jclass clazz = env->FindClass(className);
    if (clazz == nullptr) {
        LOGE("Native registration unable to find class '%s'", className);
        return -1;
    }
    int result = env->RegisterNatives(clazz, methods, numMethod);
    env->DeleteLocalRef(clazz);
    if (result == 0) {
        return 0;
    }
    jthrowable thrown = env->ExceptionOccurred();
    if (thrown != nullptr) {
        env->DeleteLocalRef(thrown);
    }
    return result;
}


void JniRegisterJavaMethods(JNIEnv *env) {
    env->GetJavaVM(&jvm);
    jclass clazz = env->FindClass("com/sanfengandroid/fakeinterface/NativeCall");
    java.nativeCall = reinterpret_cast<jclass>(env->NewGlobalRef(clazz));
    java.replaceEnv = env->GetStaticMethodID(clazz, "nativeReplaceEnv", "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;");
}

/*
 * Hook 入口函数
 * */
C_API API_PUBLIC
void
fake_load_library_init(JNIEnv *env, void *fake_soinfo, const RemoteInvokeInterface *interface, const char *cache_path, const char *config_path, const char *_process_name) {
    LOGD("Init Hook module");
    fake_linker_soinfo = fake_soinfo;
    remote = interface;

    CHECK(env);
    CHECK(fake_linker_soinfo);
    CHECK(remote);
    CHECK(cache_path);
    cache_dir = strdup(cache_path);
    process_name = strdup(_process_name);
    if (config_path != nullptr) {
        config_dir = strdup(config_path);
    }

    CHECK(remote->CallDlopenImpl);
    CHECK(remote->CallDlsymImpl);
    CHECK(remote->CallSoinfoFunction);
    CHECK(remote->CallCommonFunction);
#if __ANDROID_API__ >= __ANDROID_API_N__
    CHECK(remote->CallNamespaceFunction);
#endif

    CHECK(JniRegisterNativeMethods(env, "com/sanfengandroid/fakeinterface/NativeHook", gMethods, NELEM(gMethods)) == 0);
    JNIInterfaceMonitor::Init(remote, env);
    JniRegisterJavaMethods(env);
    InitHookConfig();
}

