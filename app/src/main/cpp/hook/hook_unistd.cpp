//
// Created by beich on 2020/12/28.
//

#include "hook_unistd.h"
#include <cerrno>
#include <fcntl.h>

#include "io_redirect.h"

FUN_INTERCEPT HOOK_DEF(int, faccessat, int dirfd, const char *pathname, int mode, int flags) {
    LOGMV("dirfd: %d path: %s, mode: %d\n", dirfd, pathname, mode);
    IS_BLACKLIST_FILE(pathname);
    const char *redirect = IoRedirect::GetRedirect(pathname);
    IoMask mask = IoRedirect::GetFileMask(redirect, mode);
    if (mask == kIMNotFound) {
        return get_orig_faccessat()(dirfd, pathname, mode, flags);
    }
    return mask;
}

STUB_SYMBOL HOOK_DEF(int, access, const char *pathname, int mode) {
    return faccessat(AT_FDCWD, pathname, mode, 0);
}

FUN_INTERCEPT HOOK_DEF(int, linkat, int old_dir_fd, const char *old_path, int new_dir_fd, const char *new_path, int flags) {
    IS_BLACKLIST_FILE(old_path);
    return get_orig_linkat()(old_dir_fd, IoRedirect::GetRedirect(old_path), new_dir_fd, IoRedirect::GetRedirect(new_path), flags);
}

STUB_SYMBOL HOOK_DEF(int, link, const char *old_path, const char *new_path) {
    return linkat(AT_FDCWD, old_path, AT_FDCWD, new_path, 0);
}

FUN_INTERCEPT HOOK_DEF(int, unlinkat, int dirfd, const char *path, int flags) {
    IS_BLACKLIST_FILE(path);
    return get_orig_unlinkat()(dirfd, IoRedirect::GetRedirect(path), flags);
}

STUB_SYMBOL HOOK_DEF(int, unlink, const char *path) {
    return unlinkat(AT_FDCWD, path, 0);
}

STUB_SYMBOL HOOK_DEF(int, rmdir, const char *path) {
    return unlinkat(AT_FDCWD, path, AT_REMOVEDIR);
}

FUN_INTERCEPT HOOK_DEF(int, chdir, const char *path) {
    IS_BLACKLIST_FILE(path);
    return get_orig_chdir()(IoRedirect::GetRedirect(path));
}

FUN_INTERCEPT HOOK_DEF(int, symlinkat, const char *old_path, int new_dir_fd, const char *new_path) {
    IS_BLACKLIST_FILE(old_path);
    return get_orig_symlinkat()(IoRedirect::GetRedirect(old_path), new_dir_fd, IoRedirect::GetRedirect(new_path));
}

STUB_SYMBOL HOOK_DEF(int, symlink, const char *old_path, const char *new_path) {
    return symlinkat(old_path, AT_FDCWD, new_path);
}

FUN_INTERCEPT HOOK_DEF(ssize_t, readlinkat, int dir_fd, const char *path, char *buf, size_t buf_size) {
    IS_BLACKLIST_FILE(path);
    const char *redirect = IoRedirect::GetRedirect(path);
    ssize_t result = get_orig_readlinkat()(dir_fd, redirect, buf, buf_size);
    if (result >= 0 && buf[0] == '/') {
        buf[buf_size] = '\0';
        const char *src = IoRedirect::RedirectToSource(buf);
        if (src != buf) {
            // 将结尾0拷贝,以防原始路径过短字符串错误
            result = strlen(src);
            if (result > buf_size) {
                result = buf_size;
            }
            memcpy(buf, src, result < buf_size ? result + 1 : buf_size);
        }
    }
    return result;
}

STUB_SYMBOL HOOK_DEF(ssize_t, readlink, const char *path, char *buf, size_t size) {
    return readlinkat(AT_FDCWD, path, buf, size);
}

FUN_INTERCEPT HOOK_DEF(int, fchownat, int dir_fd, const char *path, uid_t owner, gid_t group, int flags) {
    IS_BLACKLIST_FILE(path);
    return get_orig_fchownat()(dir_fd, IoRedirect::GetRedirect(path), owner, group, flags);
}

STUB_SYMBOL HOOK_DEF(int, chown, const char *path, uid_t owner, gid_t group) {
    return fchownat(AT_FDCWD, path, owner, group, 0);
}

STUB_SYMBOL HOOK_DEF(int, lchown, const char *path, uid_t uid, gid_t gid) {
    return fchownat(AT_FDCWD, path, uid, gid, AT_SYMLINK_NOFOLLOW);
}

FUN_INTERCEPT HOOK_DEF(char *, getcwd, char *buf, size_t size) {
    bool alloc = buf == nullptr;
    char *result = get_orig_getcwd()(buf, size);
    if (result == nullptr) {
        return result;
    }
    const char *real = IoRedirect::RedirectToSourceDirectory(buf);
    if (real == buf) {
        return result;
    }
    size_t len = strlen(real);
    if (!alloc) {
        if (len + 1 > size) {
            result = nullptr;
            errno = ERANGE;
        } else {
            strlcpy(buf, real, size);
            result = buf;
        }
    } else {
        if (len > strlen(result)) {
            free(result);
            buf = static_cast<char *>(malloc(len + 10));
            strlcpy(buf, real, len + 10);
        } else {
            strlcpy(buf, real, size);
        }
        result = buf;
    }
    return result;
}

FUN_INTERCEPT HOOK_DEF(int, __getcwd, char *buf, size_t size) {
    int result = get_orig___getcwd()(buf, size);
    if (result != 0) {
        return result;
    }
    const char *real = IoRedirect::RedirectToSourceDirectory(buf);
    if (real == buf) {
        return result;
    }
    size_t len = strlen(real);
    if (len + 1 > size) {
        result = -1;
        errno = ERANGE;
    } else {
        strlcpy(buf, real, size);
    }
    return result;
}

FUN_INTERCEPT HOOK_DEF(int, truncate, const char *path, off_t length) {
    IS_BLACKLIST_FILE(path);
    return get_orig_truncate()(IoRedirect::GetRedirectFile(path), length);
}

FUN_INTERCEPT HOOK_DEF(int, truncate64, const char *path, off64_t length) {
    IS_BLACKLIST_FILE(path);
    return get_orig_truncate64()(IoRedirect::GetRedirectFile(path), length);
}