//
// Created by beich on 2020/12/28.
//

#include <fcntl.h>
#include <sys/stat.h>
#include "hook_stat.h"
#include "io_redirect.h"

FUN_INTERCEPT HOOK_DEF(int, fchmodat, int dirfd, const char *pathname, mode_t mode, int flags) {
    IS_BLACKLIST_FILE(pathname);
    return get_orig_fchmodat()(dirfd, IoRedirect::GetRedirect(pathname), mode, flags);
}

STUB_SYMBOL HOOK_DEF(int, chmod, const char *path, mode_t mode) {
    return fchmodat(AT_FDCWD, path, mode, 0);
}

FUN_INTERCEPT HOOK_DEF(int, mkdirat, int dir_fd, const char *path, mode_t mode) {
    return get_orig_mkdirat()(dir_fd, IoRedirect::GetRedirectDirectory(path), mode);
}

STUB_SYMBOL HOOK_DEF(int, mkdir, const char *path, mode_t mode) {
    return mkdirat(AT_FDCWD, path, mode);
}

FUN_INTERCEPT HOOK_DEF(int, fstatat, int dir_fd, const char *path, struct stat *buf, int flags) {
    IS_BLACKLIST_FILE(path);
    const char *redirect = IoRedirect::GetRedirect(path);
    int result = get_orig_fstatat()(dir_fd, redirect, buf, flags);
    if (result != 0) {
        return result;
    }
    const FileAccess *file_access = IoRedirect::GetFileAccess(redirect);
    if (file_access == nullptr) {
        return result;
    }
    LOGD("redirect file stat: %s, orig access: %d, modify access: %d", path, buf->st_mode & ~07777, file_access->access);
    buf->st_mode &= ~07777;

    buf->st_mode |= file_access->access;
    buf->st_uid = file_access->uid;
    buf->st_gid = file_access->gid;
    return result;
}
__strong_alias(fstatat64, fstatat);

STUB_SYMBOL HOOK_DEF(int, stat, const char *path, struct stat *buf) {
    return fstatat(AT_FDCWD, path, buf, 0);
}
__strong_alias(stat64, stat);

// fstat传入fd已经被更改
FUN_INTERCEPT HOOK_DEF(int, fstat, int fd, struct stat *buf) {
    return get_orig_fstat()(fd, buf);
}
__strong_alias(fstat64, fstat);

STUB_SYMBOL HOOK_DEF(int, lstat, const char *path, struct stat *buf) {
    return fstatat(AT_FDCWD, path, buf, AT_SYMLINK_NOFOLLOW);
}
__strong_alias(lstat64, lstat);

FUN_INTERCEPT HOOK_DEF(int, mknodat, int dir_fd, const char *path, mode_t mode, dev_t dev) {
    IS_BLACKLIST_FILE(path);
    return get_orig_mknodat()(dir_fd, IoRedirect::GetRedirect(path), mode, dev);
}

STUB_SYMBOL HOOK_DEF(int, mknod, const char *path, mode_t mode, dev_t dev) {
    return mknodat(AT_FDCWD, path, mode, dev);
}

// mkfifo 多数用于多进程通讯,更改本进程的路径可能会影响到其它进程使用,因此不拦截

FUN_INTERCEPT HOOK_DEF(int, utimensat, int dirfd, const char *pathname, const struct timespec times[2], int flags) {
    IS_BLACKLIST_FILE(pathname);
    return get_orig_utimensat()(dirfd, IoRedirect::GetRedirect(pathname), times, flags);
}