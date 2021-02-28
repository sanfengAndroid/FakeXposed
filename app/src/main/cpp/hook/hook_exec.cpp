//
// Created by beich on 2020/12/28.
//

#include "hook_exec.h"

#include <cerrno>
#include <cstring>
#include "io_redirect.h"

enum ExecVariant {
    kIsExecL, kIsExecLE, kIsExecLP
};

FUN_INTERCEPT HOOK_DEF(int, execve, const char *filename, char *const argv[], char *const envp[]) {
    LOGMV("cmd %s ", filename);
    int i = 0;
    while (argv[i] != nullptr) {
        LOGMV("arg %d: %s ", i, filename);
        i++;
    }
    IS_BLACKLIST_FILE(filename);
    return get_orig_execve()(IoRedirect::GetRedirectFile(filename), argv, envp);
}

STUB_SYMBOL int __execl(const char *name, const char *argv0, ExecVariant variant, va_list ap) {
    // Count the arguments.
    IS_BLACKLIST_FILE(name);
    va_list count_ap;
    va_copy(count_ap, ap);
    size_t n = 1;
    while (va_arg(count_ap, char*) != nullptr) {
        ++n;
    }
    va_end(count_ap);

    // Construct the new argv.
    char *argv[n + 1];
    argv[0] = const_cast<char *>(argv0);
    n = 1;
    while ((argv[n] = va_arg(ap, char*)) != nullptr) {
        ++n;
    }

    // Collect the argp too.
    char **argp = (variant == kIsExecLE) ? va_arg(ap, char**) : FXHandler::GetEnviron();

    va_end(ap);
    const char *path = IoRedirect::GetRedirectFile(name);
    return (variant == kIsExecLP) ? execvp(path, argv) : execve(path, argv, argp);
}

STUB_SYMBOL C_API API_PUBLIC int execl(const char *name, const char *arg, ...) {
    IS_BLACKLIST_FILE(name);
    va_list ap;
    va_start(ap, arg);
    int result = __execl(IoRedirect::GetRedirectFile(name), arg, kIsExecL, ap);
    va_end(ap);
    return result;
}

STUB_SYMBOL C_API API_PUBLIC int execle(const char *name, const char *arg, ...) {
    IS_BLACKLIST_FILE(name);
    va_list ap;
    va_start(ap, arg);
    int result = __execl(IoRedirect::GetRedirectFile(name), arg, kIsExecLE, ap);
    va_end(ap);
    return result;
}

STUB_SYMBOL C_API API_PUBLIC int execlp(const char *name, const char *arg, ...) {
    IS_BLACKLIST_FILE(name);
    va_list ap;
    va_start(ap, arg);
    int result = __execl(IoRedirect::GetRedirectFile(name), arg, kIsExecLP, ap);
    va_end(ap);
    return result;
}

// 不能拦截execvp,否者子进程执行一直不结束
// java层调用执行会有问题,C层没有问题
FUN_INTERCEPT HOOK_DEF(int, execvp, const char *name, char *const *argv) {
    LOGMV("cmd %s ", name);
    int i = 0;
    while (argv[i] != nullptr) {
        LOGMV("arg %d: %s ", i, name);
        i++;
    }
    void *caller = __builtin_return_address(0);
    int error_code = 0;
    char *so_name = static_cast<char *>(remote->CallSoinfoFunction(kSFGetName, kSPAddress, caller, kSPNull, nullptr, &error_code));
    if (strcmp(so_name, "libopenjdk.so") == 0) {
        // 屏蔽java层调用
        return -1;
    }
    IS_BLACKLIST_FILE(name);
    return get_orig_execvp()(IoRedirect::GetRedirectFile(name), argv);
}

FUN_INTERCEPT HOOK_DEF(int, execvpe, const char *name, char *const *argv, char *const *envp) {
    IS_BLACKLIST_FILE(name);
    return get_orig_execvpe()(IoRedirect::GetRedirectFile(name), argv, envp);
}

FUN_INTERCEPT HOOK_DEF(int, fexecve, int __fd, char *const *__argv, char *const *__envp) __INTRODUCED_IN(28) {
    return get_orig_fexecve()(__fd, __argv, __envp);
}
