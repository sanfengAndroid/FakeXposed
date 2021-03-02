//
// Created by beich on 2020/12/28.
//

#pragma once

#include <alog.h>
#include <macros.h>
#include <linker_export.h>
#include <errno.h>
#include <map>
#include <string>
#include <vector>

#define USE_SYSCALL                 // 标记哪些函数使用syscall,然后在syscall实现中实施拦截
#define STUB_SYMBOL                 // 标记跳板函数,不实施拦截,在具体目标函数拦截
#define FUN_INTERCEPT               // 方法需要拦截,处理对应的黑名单


struct RuntimeBean {
    std::string command;
    std::string new_command;
    std::vector<std::string> parameters;
    const char *parameter_blocks;
    jsize blocks_size;
    jsize new_parameter_size;
    // 输入需要以 \n 结束
    std::string input_stream;
    std::string output_stream;
    std::string error_stream;
    bool match_parameter;
    bool replace_command;
    bool replace_parameter;
    bool replace_input;
    bool replace_output;
    bool replace_error;
};

class FXHandler {
public:
    static bool SymbolIsBlacklisted(const char *symbol);

    static bool FileNameIsBlacklisted(const char *path);

    static bool ClassNameBlacklisted(const char *class_name);

    static bool StackClassNameBlacklisted(const char *class_name);

    static char *EnvironmentReplace(const char *name, char *value);

    static char **GetEnviron();

    static bool PropertyIsBlacklisted(const char *name);

    static const char *PropertyReplace(const char *name, const char *value);

    static void *FindLibcSymbolRealAddress(const char *name);

    static bool RuntimeReplaceCommandArgv(RuntimeBean *bean, const char **new_command, const char **new_argv, jsize *block_size, jsize *new_argc);

    static bool RuntimeReplaceStream(RuntimeBean *bean, int fds[3]);

    static RuntimeBean *FindRuntimeBean(const char *cmd, const char **argv, int argc);

    static std::vector<RuntimeBean> &GetRuntimeCmd(const char *cmd);

    static FXHandler *Get();

private:
    FXHandler() {};

    ~FXHandler() {};

public:
    // Android 7以上是soinfo handle, 否者是soinfo自身
    void *libc_handle;
    void *libc_soinfo;
    void *self_soinfo;
    const char *cache_dir;
    const char *config_dir;
    const char *process_name;
    void *fake_linker_soinfo;

public:
    std::map<std::string, int> maps_rules;
    std::map<std::string, int> file_blacklist;
    std::map<std::string, int> file_path_blacklist;
    std::map<std::string, int> symbol_blacklist;
    std::map<std::string, std::string> properties;
    std::map<std::string, std::pair<std::string, std::string>> environments;
    std::map<std::string, std::string> load_class_blacklist;
    std::map<std::string, std::string> static_class_blacklist;
    int pid;
    int api;
private:
    std::map<std::string, std::vector<RuntimeBean>> runtime_blacklist;
    static FXHandler *instance_;
    DISALLOW_COPY_AND_ASSIGN(FXHandler);
};

#define HOOK_DEF(ret, func, ...)                                                                                    \
    static ret (*orig_##func)(__VA_ARGS__);                                                                         \
    C_API ret (*get_orig_##func(void))(__VA_ARGS__){                                                                \
        if(!orig_##func){                                                                                           \
            orig_##func = reinterpret_cast<ret (*)(__VA_ARGS__)>(FXHandler::FindLibcSymbolRealAddress(#func));      \
            CHECK(orig_##func);                                                                                     \
        }                                                                                                           \
    return orig_##func;                                                                                             \
    }                                                                                                               \
    C_API API_PUBLIC ret func(__VA_ARGS__)

#define HOOK_DEF_CPP(ret, func, ...)                                                                                \
    static ret (*orig_##func)(__VA_ARGS__);                                                                         \
    extern "C++" ret (*get_orig_##func(void))(__VA_ARGS__){                                                         \
        if(!orig_##func){                                                                                           \
            orig_##func = reinterpret_cast<ret (*)(__VA_ARGS__)>(FXHandler::FindLibcSymbolRealAddress(#func));      \
            CHECK(orig_##func);                                                                                     \
        }                                                                                                           \
        return orig_##func;                                                                                         \
    }                                                                                                               \
    extern "C++" API_PUBLIC ret func(__VA_ARGS__)

#define HOOK_DECLARE(ret, func, ...)                                                                                \
    C_API ret (*get_orig_##func(void))(__VA_ARGS__);                                                                \
    C_API API_PUBLIC ret func(__VA_ARGS__)

C_API API_PUBLIC void fake_load_library_init(JNIEnv *env, void *fake_soinfo, const RemoteInvokeInterface *interface, const char *cache_path, const char *config_path,
                                             const char *process_name);

extern const RemoteInvokeInterface *remote;

int force_O_LARGEFILE(int flags);

#define IS_BLACKLIST_FILE(name)                                                         \
    if (FXHandler::FileNameIsBlacklisted(name)) {                                       \
        LOGW("Fake '%s': access blacklist file %s",__FUNCTION__,  name);                \
        errno = ENOENT;                                                                 \
        return -1;                                                                      \
    }

#define IS_BLACKLIST_FILE_RETURN(name, ret) \
    if (FXHandler::FileNameIsBlacklisted(name)) {                                       \
        LOGW("Fake '%s': access blacklist file %s",__FUNCTION__,  name);                \
        errno = ENOENT;                                                                 \
        return ret;                                                                      \
    }

#define LOGMV(format, ...) LOGV("[Monitor %s] "#format, __FUNCTION__, __VA_ARGS__)
#define LOGMD(format, ...) LOGD("[Monitor %s] "#format, __FUNCTION__, __VA_ARGS__)
#define LOGMI(format, ...) LOGI("[Monitor %s] "#format, __FUNCTION__, __VA_ARGS__)
#define LOGMW(format, ...) LOGW("[Monitor %s] "#format, __FUNCTION__, __VA_ARGS__)
#define LOGME(format, ...) LOGE("[Monitor %s] "#format, __FUNCTION__, __VA_ARGS__)
