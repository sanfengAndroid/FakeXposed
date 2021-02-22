//
// Created by beich on 2020/12/28.
//

#include "hook_io.h"

#include <fcntl.h>
#include <syscall.h>
#include <sys/stat.h>
#include <sys/statfs.h>
#include <sys/statvfs.h>
#include <cerrno>
#include <dirent.h>

#include "io_redirect.h"

#include "hook_common.h"
#include "hook_syscall.h"
#include "hook_unistd.h"

static inline bool needs_mode(int flags) {
    return ((flags & O_CREAT) == O_CREAT) || ((flags & O_TMPFILE) == O_TMPFILE);
}

USE_SYSCALL HOOK_DEF(int, openat, int fd, const char *pathname, int flags, ...) {
    mode_t mode = 0;
    if (needs_mode(flags)) {
        va_list args;
        va_start(args, flags);
        mode = static_cast<mode_t>(va_arg(args, int));
        va_end(args);
    }
    return syscall(__NR_openat, fd, pathname, flags, mode);
}
__strong_alias(openat64, openat);

USE_SYSCALL HOOK_DEF(int, __openat, int fd, const char *path, int flags, int mode) {
    return syscall(__NR_openat, fd, path, flags, mode);
}

USE_SYSCALL HOOK_DEF(int, __openat_2, int fd, const char *pathname, int flags) {
    CHECK_OUTPUT(!needs_mode(flags), "open: called with O_CREAT/O_TMPFILE but no mode");
    return syscall(__NR_openat, fd, pathname, flags, 0);
}

/*
 * __open已废弃
 * */
USE_SYSCALL HOOK_DEF(int, open, const char *path, int flags, ...) {
    LOGMV("path %s flags: %d", path, flags);
    mode_t mode = 0;
    if (needs_mode(flags)) {
        va_list args;
        va_start(args, flags);
        mode = static_cast<mode_t>(va_arg(args, int));
        va_end(args);
    }
    return syscall(__NR_openat, AT_FDCWD, path, flags, mode);
}
__strong_alias(open64, open);

USE_SYSCALL HOOK_DEF(int, __open_2, const char *pathname, int flags) {
    LOGMV("path %s, flags: %d", pathname, flags);
    CHECK_OUTPUT(!needs_mode(flags), "open: called with O_CREAT/O_TMPFILE but no mode");
    return syscall(__NR_openat, AT_FDCWD, pathname, flags, 0);
}

FUN_INTERCEPT HOOK_DEF(FILE *, fopen, const char *path, const char *mode) {
    LOGMV("path %s, mode %s", path, mode);
    IS_BLACKLIST_FILE_RETURN(path, nullptr);

    const char *fake_path = IoRedirect::RedirectMaps(path);
    if (fake_path != nullptr) {
        FILE *fd = get_orig_fopen()(fake_path, mode);
        if (fd != nullptr) {
            LOGW("Fake: The maps file has been redirected, fd: %s", fake_path);
            get_orig_unlink()(fake_path);
            return fd;
        }
    }
    return get_orig_fopen()(IoRedirect::GetRedirectFile(path), mode);
}

__strong_alias(fopen64, fopen);

FUN_INTERCEPT HOOK_DEF(struct dirent *, readdir, DIR *dirp) {
    struct dirent *ret = get_orig_readdir()(dirp);
    if (ret == nullptr) {
        return ret;
    }
    int found;
    do {
        if (FileNameIsBlacklisted(ret->d_name)) {
            LOGW("Fake: Found blacklist file: %s, reading next...", ret->d_name);
            ret = orig_readdir(dirp);
            found = 1;
        } else {
            found = 0;
        }
    } while (found == 1 && ret != nullptr);
    return ret;
}

FUN_INTERCEPT HOOK_DEF(int, renameat, int old_dir_fd, const char *old_path, int new_dir_fd, const char *new_path) {
    IS_BLACKLIST_FILE(old_path);
    return get_orig_renameat()(old_dir_fd, IoRedirect::GetRedirect(old_path), new_dir_fd, IoRedirect::GetRedirect(new_path));
}

STUB_SYMBOL HOOK_DEF(int, rename, const char *old_path, const char *new_path) {
    return renameat(AT_FDCWD, old_path, AT_FDCWD, new_path);
}




