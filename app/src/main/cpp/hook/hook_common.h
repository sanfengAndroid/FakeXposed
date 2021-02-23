//
// Created by beich on 2020/12/28.
//

#pragma once

#include <alog.h>
#include <macros.h>
#include <linker_export.h>
#include <errno.h>

#define USE_SYSCALL                 // 标记哪些函数使用syscall,然后在syscall实现中实施拦截
#define STUB_SYMBOL                 // 标记跳板函数,不实施拦截,在具体目标函数拦截
#define FUN_INTERCEPT               // 方法需要拦截,处理对应的黑名单


#define HOOK_DEF(ret, func, ...)                                                                        \
    static ret (*orig_##func)(__VA_ARGS__);                                                             \
    C_API ret (*get_orig_##func(void))(__VA_ARGS__){                                                   \
        if(!orig_##func){                                                                               \
            orig_##func = reinterpret_cast<ret (*)(__VA_ARGS__)>(FindLibcSymbolRealAddress(#func));     \
            CHECK(orig_##func);                                                                         \
        }                                                                                               \
        return orig_##func;                                                                             \
    }                                                                                                   \
    C_API API_PUBLIC ret func(__VA_ARGS__)

#define HOOK_DEF_CPP(ret, func, ...)                                                                    \
    static ret (*orig_##func)(__VA_ARGS__);                                                             \
    extern "C++" ret (*get_orig_##func(void))(__VA_ARGS__){                                                   \
        if(!orig_##func){                                                                               \
        orig_##func = reinterpret_cast<ret (*)(__VA_ARGS__)>(FindLibcSymbolRealAddress(#func));             \
            CHECK(orig_##func);                                                                         \
        }                                                                                               \
        return orig_##func;                                                                             \
    }                                                                                                   \
    extern "C++" API_PUBLIC ret func(__VA_ARGS__)

#define HOOK_DECLARE(ret, func, ...)                                                                     \
    C_API ret (*get_orig_##func(void))(__VA_ARGS__);                                                   \
    C_API API_PUBLIC ret func(__VA_ARGS__)

void *FindLibcSymbolRealAddress(const char *name);

C_API API_PUBLIC void fake_load_library_init(JNIEnv *env, void *fake_soinfo, const RemoteInvokeInterface *interface, const char *cache_path, const char *config_path,
                                             const char *process_name);

extern const RemoteInvokeInterface *remote;
extern const char *cache_dir;
// Android 7以上是soinfo handle, 否者是soinfo自身
extern void* libc_handle;
extern void* libc_soinfo;
// 本模块自身的soinfo
extern void* self_soinfo;
bool SymbolIsBlacklisted(const char *symbol);

bool FileNameIsBlacklisted(const char *path);

char* FindEnvReplace(const char* name, char *value);

char **GetEnviron();

bool PropertiesIsBlacklisted(const char* name);

const char* PropertiesReplace(const char* name, const char* value);

int force_O_LARGEFILE(int flags);

#define IS_BLACKLIST_FILE(name)                                                         \
    if (FileNameIsBlacklisted(name)) {                                                  \
        LOGW("Fake '%s': access blacklist file %s",__FUNCTION__,  name);                \
        errno = ENOENT;                                                                 \
        return -1;                                                                      \
    }

#define IS_BLACKLIST_FILE_RETURN(name, ret) \
    if (FileNameIsBlacklisted(name)) {                                                  \
        LOGW("Fake '%s': access blacklist file %s",__FUNCTION__,  name);                \
        errno = ENOENT;                                                                 \
        return ret;                                                                      \
    }

#define LOGMV(format, ...) LOGV("[Monitor %s] "#format, __FUNCTION__, __VA_ARGS__)
#define LOGMD(format, ...) LOGD("[Monitor %s] "#format, __FUNCTION__, __VA_ARGS__)
#define LOGMI(format, ...) LOGI("[Monitor %s] "#format, __FUNCTION__, __VA_ARGS__)
#define LOGMW(format, ...) LOGW("[Monitor %s] "#format, __FUNCTION__, __VA_ARGS__)
#define LOGME(format, ...) LOGE("[Monitor %s] "#format, __FUNCTION__, __VA_ARGS__)
