
#include "hook_main.h"

#include <libgen.h>
#include <sys/system_properties.h>
#include <unistd.h>
#include <elf.h>

#include <gtype.h>
#include <maps_util.h>
#include <variable_length_object.h>
#include <scoped_utf_chars.h>
#include <jni_helper.h>
#include <module_config.h>
#include <fcntl.h>

#include "io_redirect.h"
#include "hook_jni_native_interface.h"
#include "hook_syscall.h"
#include "hook_java_native.h"
#include "hook_common.h"


#ifndef NELEM
#define NELEM(x) ((int) (sizeof(x) / sizeof((x)[0])))
#endif

#define NO_FOUND_INDEX -10000

FXHandler *FXHandler::instance_ = new FXHandler;

int g_log_level = HOOK_LOG_LEVEL;
static std::map<std::string, int> hook_option;
static std::map<std::string, std::string> string_option;


static bool hook_init = false;
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
void *FXHandler::FindLibcSymbolRealAddress(const char *name) {
    int error_code;
    void *result = remote->CallSoinfoFunction(kSFCallDlsym, kSPOriginal, FXHandler::Get()->libc_soinfo, kSPSymbol, name, &error_code);
    if (error_code == kErrorNo) {
        return result;
    } else {
        LOGE("ERROR: find symbol failed, symbol name: %s, error code: 0x%x", name, error_code);
    }
    return nullptr;
}

char **FXHandler::GetEnviron() {
    static char ***env_ptr = nullptr;
    if (env_ptr == nullptr) {
        env_ptr = static_cast<char ***>(FindLibcSymbolRealAddress("environ"));
        CHECK(env_ptr);
    }
    return *env_ptr;
}

static void InitPreFunction() {
    int error_code;
    FXHandler::Get()->libc_handle = remote->CallSoinfoFunction(kSFGetHandle, kSPName, "libc.so", kSPNull, nullptr, &error_code);
    CHECK(FXHandler::Get()->libc_handle);
    FXHandler::Get()->libc_soinfo = remote->CallSoinfoFunction(kSFInquire, kSPName, "libc.so", kSPNull, nullptr, &error_code);
    CHECK(FXHandler::Get()->libc_soinfo);
    FXHandler::Get()->self_soinfo = remote->CallSoinfoFunction(kSFInquire, kSPAddress, nullptr, kSPNull, nullptr, &error_code);
    CHECK(FXHandler::Get()->self_soinfo);

    LOGD("libc handle: %p, libc soinfo: %p, self soinfo: %p", FXHandler::Get()->libc_handle, FXHandler::Get()->libc_soinfo, FXHandler::Get()->self_soinfo);

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

static inline bool HasMapKey(std::map<std::string, std::string> &map, const char *name) {
    auto iter = map.find(name);
    return iter != map.end();
}

static inline bool HasMapKeyword(std::map<std::string, std::string> &map, const char *name) {
    return std::any_of(map.begin(), map.end(), [&](const auto &key) {
        return strstr(key.first.c_str(), name) != nullptr;
    });
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
        if (map == &FXHandler::Get()->file_blacklist && name[0] == '/') {
            FXHandler::Get()->file_path_blacklist[name] = value;
        } else {
            (*map)[name] = value;
        }
        return;
    }
    if (map == &FXHandler::Get()->file_blacklist) {
        auto found = FXHandler::Get()->file_path_blacklist.find(name);
        if (found != FXHandler::Get()->file_path_blacklist.end()) {
            FXHandler::Get()->file_path_blacklist.erase(found);
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

bool FXHandler::FileNameIsBlacklisted(const char *path) {
    if (path == nullptr) {
        return false;
    }
    if (FindMapIndex(Get()->file_path_blacklist, path) == kOOpen) {
        return true;
    }
    char *name = basename(path);
    if (name == nullptr) {
        return false;
    }
    return FindMapIndex(Get()->file_blacklist, name) == kOOpen;
}

bool FXHandler::ClassNameBlacklisted(const char *class_name) {
    return HasMapKeyword(instance_->load_class_blacklist, class_name);
}

bool FXHandler::StackClassNameBlacklisted(const char *class_name) {
    return HasMapKeyword(instance_->static_class_blacklist, class_name);
}

static std::map<std::string, char *> replace_env;

char *FXHandler::EnvironmentReplace(const char *name, char *value) {
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


bool FXHandler::SymbolIsBlacklisted(const char *symbol) {
    if (Get()->symbol_blacklist.empty()) {
        return false;
    }
    return FindMapIndex(Get()->symbol_blacklist, symbol) == kOOpen;
}

bool FXHandler::PropertyIsBlacklisted(const char *name) {
    auto found = Get()->properties.find(name);
    if (found != Get()->properties.end() && found->second.empty()) {
        LOGD("found blacklist property name: %s", name);
        return true;
    }
    return false;
}

const char *FXHandler::PropertyReplace(const char *name, const char *value) {
    auto found = Get()->properties.find(name);
    if (found != Get()->properties.end() && !found->second.empty()) {
        LOGD("found filter property name: %s, old value: %s, new value: %s", name, value, found->second.c_str());
        return found->second.c_str();
    }
    return nullptr;
}

FXHandler *FXHandler::Get() {
    return instance_;
}

bool FXHandler::RuntimeReplaceCommandArgv(RuntimeBean *bean, const char **new_command, const char **new_argv, jsize *block_size, jsize *new_argc) {
    bool result = false;
    if (bean->replace_command) {
        *new_command = bean->new_command.c_str();
        result = true;
    }
    if (bean->replace_parameter) {
        *new_argc = bean->new_parameter_size;
        if (bean->new_parameter_size > 0) {
            *new_argv = bean->parameter_blocks;
            *block_size = bean->blocks_size;
        }
        result = true;
    }
    return result;
}

#define IO_FAILURE_RETRY(return_type, syscall_name, _fd, ...) ({        \
    return_type _rc = -1;                                               \
    int _syscallErrno;                                                  \
    do {                                                                \
        _rc = syscall_name(_fd, __VA_ARGS__);                           \
        _syscallErrno = errno;                                          \
        if (_rc == -1 && _syscallErrno != EINTR) {                      \
            break;                                                      \
        }                                                               \
    } while (_rc == -1); /* && _syscallErrno == EINTR && !_wasSignaled */ \
    if (_rc == -1) { \
        /* If the syscall failed, re-set errno: throwing an exception might have modified it. */ \
        errno = _syscallErrno; \
    } \
    _rc; })

bool FXHandler::RuntimeReplaceStream(RuntimeBean *bean, int fds[3]) {
    // fds[0] stdin
    bool result = false;
    if (bean->replace_input && fds[0] != -1) {
        const char *input = bean->input_stream.c_str();
        if (IO_FAILURE_RETRY(ssize_t, write, fds[0], input, strlen(input)) != -1) {
            close(fds[0]);
            fds[0] = __open_real("/dev/null", O_WRONLY);
            result = true;
            LOGD("fix input stream: %s ,fd: %d", input, fds[0]);
        } else {
            LOGE("write fixed input stream string %s failed.", input);
        }
    }
    if (bean->replace_output && fds[1] != -1) {
        int fd = IoRedirect::CreateTempFile();
        if (fd == -1) {
            LOGE("Failed to create temporary file, unable to replace output stream");
        } else {
            const char *input = bean->output_stream.c_str();
            if (IO_FAILURE_RETRY(ssize_t, write, fd, input, strlen(input)) != -1) {
                close(fds[1]);
                fds[1] = fd;
                result = true;
                lseek(fd, 0, SEEK_SET);
                LOGD("fix output stream: %s, fd: %d", input, fd);
            } else {
                LOGE("write fixed output stream string %s failed.", input);
            }
        }
    }
    if (bean->replace_error && fds[2] != -1) {
        int fd = IoRedirect::CreateTempFile();
        if (fd == -1) {
            LOGE("Failed to create temporary file, unable to replace error stream");
        } else {
            const char *input = bean->error_stream.c_str();
            if (IO_FAILURE_RETRY(ssize_t, write, fd, input, strlen(input)) != -1) {
                close(fds[2]);
                fds[2] = fd;
                lseek(fd, 0, SEEK_SET);
                result = true;
                LOGD("fix error stream: %s, fd: %d", input, fd);
            } else {
                LOGE("write fixed error stream string %s failed.", input);
            }
        }
    }
    return result;
}


RuntimeBean *FXHandler::FindRuntimeBean(const char *cmd, const char **argv, int argc) {
    auto iter = instance_->runtime_blacklist.find(cmd);
    if (iter == instance_->runtime_blacklist.end()) {
        return nullptr;
    }
    for (RuntimeBean &bean : iter->second) {
        if (!bean.match_parameter) {
            return &bean;
        }
        if (bean.parameters.size() != argc) {
            continue;
        }
        bool found = true;
        for (int i = 0; i < argc; ++i) {
            if (bean.parameters[i] != argv[i]) {
                found = false;
                break;
            }
        }
        if (found) {
            return &bean;
        }
    }
    return nullptr;
}

std::vector<RuntimeBean> &FXHandler::GetRuntimeCmd(const char *cmd) {
    return instance_->runtime_blacklist[cmd];
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
        VarLengthObject<const char *> *libs = VaArgsToVarLengthObject<const char *>(9, "libjavacore.so", "libnativehelper.so", "libnativeloader.so",
                                                                                    "libart.so", "libopenjdk.so", "libopenjdkjvm.so",
                                                                                    "libandroid_runtime.so", "libcutils.so", "libbase.so");
        remote->CallCommonFunction(kCFCallManualRelinks, kSPAddress, nullptr, kSPNames, libs, &error_code);
    } else {
        VarLengthObject<const char *> *libs = VaArgsToVarLengthObject<const char *>(8, "libjavacore.so", "libnativehelper.so", "libnativeloader.so",
                                                                                    "libart.so", "libopenjdkjvm.so", "libandroid_runtime.so",
                                                                                    "libcutils.so", "libbase.so");
        remote->CallCommonFunction(kCFCallManualRelinks, kSPAddress, nullptr, kSPNames, libs, &error_code);

        remote->CallCommonFunction(kCFAddRelinkFilterSymbol, kSPSymbol, "execvp", kSPNull, nullptr, &error_code);
        remote->CallCommonFunction(kCFCallManualRelink, kSPAddress, nullptr, kSPName, "libopenjdk.so", &error_code);
        remote->CallCommonFunction(kCFRemoveRelinkFilterSymbol, kSPSymbol, "execvp", kSPNull, nullptr, &error_code);
    }
    LOGD("call manual relink library");
    char **env = FXHandler::GetEnviron();
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

static int get_api_level() {
    char value[92] = { 0 };
    if (__system_property_get("ro.build.version.sdk", value) < 1) return -1;
    int api_level = atoi(value);
    return (api_level > 0) ? api_level : -1;
}

static void InitHookConfig() {
    if (hook_init) {
        return;
    }
    InitPreFunction();
    FXHandler::Get()->pid = getpid();
    FXHandler::Get()->api = get_api_level();
    IoRedirect::SetPid(FXHandler::Get()->pid);
    int error_code;
    const char *fake_name = static_cast<const char *>(remote->CallSoinfoFunction(kSFGetName, kSPOriginal, FXHandler::Get()->fake_linker_soinfo, kSPNull, nullptr, &error_code));
    if (error_code == kErrorNo) {
        FXHandler::Get()->maps_rules[fake_name] = kMapsRemove;
    }
    fake_name = static_cast<const char *>(remote->CallSoinfoFunction(kSFGetName, kSPAddress, nullptr, kSPNull, nullptr, &error_code));
    if (error_code == kErrorNo) {
        FXHandler::Get()->maps_rules[fake_name] = kMapsRemove;
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
            _map = &FXHandler::Get()->file_blacklist;
            break;
        case 1:
            _map = &FXHandler::Get()->symbol_blacklist;
            break;
        case 2:
            _map = &FXHandler::Get()->maps_rules;
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
                _map = &FXHandler::Get()->file_blacklist;
                break;
            case 1:
                _map = &FXHandler::Get()->symbol_blacklist;
                break;
            case 2:
                _map = &FXHandler::Get()->maps_rules;
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
    // 与Java枚举类 NativeStringOption 相对应
    switch (type) {
        case 0:
            _map = &FXHandler::Get()->properties;
            break;
        case 1:
            _map = &FXHandler::Get()->load_class_blacklist;
            break;
        case 2:
            _map = &FXHandler::Get()->static_class_blacklist;
            break;
        default:
            return kEJParameterType;
    }
    LOGD("add string blacklist type: %d, name: %s, value: %s", type, _name.c_str(), _value.c_str());
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
            _map = &FXHandler::Get()->file_blacklist;
            break;
        case 1:
            _map = &FXHandler::Get()->symbol_blacklist;
            break;
        case 2:
            _map = &FXHandler::Get()->maps_rules;
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
    JNHook::InitJavaNativeHook(env);
    return error_code;
}

static void NativeHook_ClearAll(JNIEnv *env, jclass clazz) {
    FXHandler::Get()->file_blacklist.clear();
    FXHandler::Get()->file_path_blacklist.clear();
    FXHandler::Get()->symbol_blacklist.clear();
    FXHandler::Get()->properties.clear();
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

static char *VectorToBlock(const std::vector<std::string> &vector, jsize size) {
    char *block = new char[size];
    for (auto &str : vector) {
        memcpy(block, str.c_str(), strlen(str.c_str()) + 1);
    }
    return block;
}

static jint NativeHook_SetRuntimeBlacklist(JNIEnv *env, jclass clazz, jstring old_cmd, jstring new_cmd, jobjectArray old_argv,
                                           jboolean match_argv, jobjectArray new_argv, jboolean replace_argv, jstring input,
                                           jstring output, jstring error) {
    RuntimeBean bean;
    ScopedUtfChars _old_cmd(env, old_cmd);
    bean.command = _old_cmd.c_str();
    ScopedUtfChars _new_cmd(env, new_cmd, false);
    bean.replace_command = _new_cmd.c_str() != nullptr;
    if (bean.replace_command) {
        bean.new_command = _new_cmd.c_str();
    }
    bean.match_parameter = match_argv;
    if (bean.match_parameter && old_argv != nullptr) {
        jsize len = env->GetArrayLength(old_argv);
        for (jsize i = 0; i < len; i++) {
            ScopedUtfChars arg(env, (jstring) env->GetObjectArrayElement(old_argv, i));
            bean.parameters.emplace_back(arg.c_str());
        }
    }
    bean.replace_parameter = replace_argv;
    bean.blocks_size = 0;
    if (bean.replace_parameter && new_argv != nullptr) {
        jsize len = env->GetArrayLength(new_argv);
        std::vector<std::string> argv;
        jsize block_size = 0;
        for (jsize i = 0; i < len; i++) {
            ScopedUtfChars arg(env, (jstring) env->GetObjectArrayElement(new_argv, i));
            block_size += strlen(arg.c_str());
            argv.emplace_back(arg.c_str());
        }
        bean.blocks_size = block_size + len;
        bean.parameter_blocks = VectorToBlock(argv, bean.blocks_size);
        bean.new_parameter_size = len;
    }
    bean.replace_input = input != nullptr;
    if (bean.replace_input) {
        ScopedUtfChars _input(env, input);
        bean.input_stream = _input.c_str();
    }
    bean.replace_output = output != nullptr;
    if (bean.replace_output) {
        ScopedUtfChars _output(env, output);
        bean.output_stream = _output.c_str();
    }
    bean.replace_error = error != nullptr;
    if (error != nullptr) {
        ScopedUtfChars _error(env, input);
        bean.error_stream = _error.c_str();
    }
    std::vector<RuntimeBean> &runtimes = FXHandler::GetRuntimeCmd(_old_cmd.c_str());
    runtimes.push_back(bean);
    return kErrorNo;
}

static void NativeHook_Test1(JNIEnv *env, jclass clazz) {

}

static void NativeHook_Test(JNIEnv *env, jclass clazz) {
    putenv((char *) "test_key=sanfengandroid");
}

static void NativeHook_Test2(JNIEnv *env, jclass clazz) {

}

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
        NATIVE_METHOD(NativeHook, ClearAll, "()V"),
        NATIVE_METHOD(NativeHook, SetRuntimeBlacklist,
                      "(Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;Z[Ljava/lang/String;ZLjava/lang/String;Ljava/lang/String;Ljava/lang/String;)I"),
        NATIVE_METHOD(NativeHook, Test, "()V"),
        NATIVE_METHOD(NativeHook, Test1, "()V"),

};

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
    LOGD("Init Hook module, cache path: %s", cache_path);
    FXHandler::Get()->fake_linker_soinfo = fake_soinfo;
    remote = interface;
    CHECK(env);
    CHECK(fake_soinfo);
    CHECK(remote);
    CHECK(cache_path);
    FXHandler::Get()->cache_dir = strdup(cache_path);
    FXHandler::Get()->process_name = strdup(_process_name);
    if (config_path != nullptr) {
        FXHandler::Get()->config_dir = strdup(config_path);
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

