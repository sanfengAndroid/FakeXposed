//
// Created by beich on 2020/12/28.
//

#include <alog.h>
#include "hook_dlfcn.h"
#include "hook_jni_native_interface.h"
#include "io_redirect.h"

__BEGIN_DECLS

FUN_INTERCEPT API_PUBLIC

void *dlopen(const char *filename, int flag) {
    void *result;
    IS_BLACKLIST_FILE_RETURN(filename, nullptr);
    const char *path = IoRedirect::GetRedirectFile(filename);
    LOGMV("filename: %s, flag: 0x%x", filename, flag);
#if __ANDROID_API__ >= __ANDROID_API_N__
    result = remote->CallDlopenImpl(path, flag, nullptr, __builtin_return_address(0));
#else
    result = remote->CallDlopenImpl(path, flag, nullptr);
#endif
    return result;
}

FUN_INTERCEPT API_PUBLIC void *dlsym(void *handle, const char *symbol) {
    void *result = nullptr;
    LOGMV("caller: %p, handle: %p, symbol: %s", __builtin_return_address(0), handle, symbol);
    if (symbol == nullptr) {
        return result;
    }
    if (FXHandler::SymbolIsBlacklisted(symbol)) {
        LOGMW("get hidden symbol: %s", symbol);
        return result;
    }

    // libc handle 有可能就是自身soinfo
    if (handle == FXHandler::Get()->libc_handle || handle == FXHandler::Get()->libc_soinfo) {
        // 优先在本模块查找
        LOGD("find libc dlsym, Use myself first");
        int error_code;
        result = remote->CallSoinfoFunction(kSFCallDlsym, kSPOriginal, FXHandler::Get()->self_soinfo, kSPSymbol, symbol, &error_code);
        if (error_code == kErrorNo && result != nullptr) {
            return result;
        }
    }
#if __ANDROID_API__ >= __ANDROID_API_N__
    remote->CallDlsymImpl(handle, symbol, nullptr, __builtin_return_address(0), &result);
#else
    result = remote->CallDlsymImpl(handle, symbol);
#endif
    return result;
}

typedef struct {
    /** A bitmask of `ANDROID_DLEXT_` enum values. */
    uint64_t flags;

    /** Used by `ANDROID_DLEXT_RESERVED_ADDRESS` and `ANDROID_DLEXT_RESERVED_ADDRESS_HINT`. */
    void *reserved_addr;
    /** Used by `ANDROID_DLEXT_RESERVED_ADDRESS` and `ANDROID_DLEXT_RESERVED_ADDRESS_HINT`. */
    size_t reserved_size;

    /** Used by `ANDROID_DLEXT_WRITE_RELRO` and `ANDROID_DLEXT_USE_RELRO`. */
    int relro_fd;

    /** Used by `ANDROID_DLEXT_USE_LIBRARY_FD`. */
    int library_fd;
    /** Used by `ANDROID_DLEXT_USE_LIBRARY_FD_OFFSET` */
#if __ANDROID_API__ > __ANDROID_API_L__
    off64_t library_fd_offset;
#endif

    /** Used by `ANDROID_DLEXT_USE_NAMESPACE`. */
#if __ANDROID_API__ >= __ANDROID_API_N__
    struct android_namespace_t *library_namespace;
#endif
} android_dlextinfo;

///*
// * 拦截Android层调用,这里有可能漏掉一些命名空间,因为Hook模块加载时机太晚,一些已经存在的命名空间无法找到,
// * 而当extinfo不为空时实际是使用extinfo中的命名空间
// * */
FUN_INTERCEPT API_PUBLIC void *android_dlopen_ext(const char *filename, int flag, const android_dlextinfo *extinfo) {
    IS_BLACKLIST_FILE_RETURN(filename, nullptr);
    const char *path = IoRedirect::GetRedirectFile(filename);
    LOGMV("filename: %s, flag: 0x%x, extinfo: %p", filename, flag, extinfo);
    void *result;
#if __ANDROID_API__ >= __ANDROID_API_N__
    int error_code = 0;
    if (extinfo != nullptr && extinfo->library_namespace != nullptr) {
        remote->CallNamespaceFunction(kNFAddGlobalSoinfoToNamespace, kNPOriginal, extinfo->library_namespace, kNPAddress, nullptr, &error_code);
    }
    result = remote->CallDlopenImpl(path, flag, extinfo, __builtin_return_address(0));
#else
    result = remote->CallDlopenImpl(path, flag, extinfo);
#endif
    return result;
}

#if __ANDROID_API__ >= __ANDROID_API_N__
/*
 * 新创建命名空间内已经有全局库了
 * */
/*struct android_namespace_t**/
FUN_INTERCEPT API_PUBLIC void *android_create_namespace(const char *name, const char *ld_library_path, const char *default_library_path, uint64_t type,
                                                        const char *permitted_when_isolated_path, /*struct android_namespace_t**/void *parent) {
    LOGMV("name: %s, ld library path: %s, default library path: %s, permitted path: %s", name, ld_library_path, default_library_path,
          permitted_when_isolated_path);
    return remote->CallCreateNamespaceImpl(name, IoRedirect::GetRedirectDirectory(ld_library_path), IoRedirect::GetRedirectDirectory(default_library_path), type,
                                           IoRedirect::GetRedirectDirectory(permitted_when_isolated_path), parent, __builtin_return_address(0));
}

#endif

__END_DECLS