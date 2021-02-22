//
// Created by beich on 2020/12/28.
//

#include "hook_syscall.h"
#include <syscall.h>
#include <fcntl.h>
#include <cerrno>
#include <unistd.h>
#include <sys/stat.h>

#include <gtype.h>

#include "io_redirect.h"
#include "hook_unistd.h"

#define INTERCEPT_SYSCALL(name, ...) \
long intercept_##name(long number, __VA_ARGS__)

#define CALL_INTERCEPT(name, ...) \
    intercept_##name(number, __VA_ARGS__)

static long (*orig_syscall)(long number, ...);

#define LOG(format, ...) LOGV("[syscall %s] "#format, __FUNCTION__, __VA_ARGS__)

C_API long (*get_orig_syscall(void))(long number, ...) {
    if (!orig_syscall) {
        orig_syscall = reinterpret_cast<long (*)(long number, ...)>(FindLibcSymbolRealAddress("syscall"));
        CHECK(orig_syscall);
    }
    return orig_syscall;
}

INTERCEPT_SYSCALL(openat, int fd, const char *pathname, int flags, int mode) {
    LOG("fd: %d, path: %s, flags: %d, mode: %d", fd, pathname, flags, mode);
    IS_BLACKLIST_FILE(pathname);
    const char *fake_path = IoRedirect::RedirectMaps(pathname);
    if (fake_path != nullptr) {
        int fd_ = orig_syscall(__NR_openat, AT_FDCWD, fake_path, force_O_LARGEFILE(flags), mode);
        if (fd_ != -1) {
            LOGW("Fake 'maps': the maps file has been redirected, fd: %d,path: %s", fd_, fake_path);
            get_orig_unlink()(fake_path);
            return fd_;
        }
    }
    return orig_syscall(__NR_openat, fd, IoRedirect::GetRedirect(pathname), force_O_LARGEFILE(flags), mode);
}


#ifdef __LP64__
INTERCEPT_SYSCALL(statfs, const char* path, struct statfs* result){
    LOG("path: %s", path);
    IS_BLACKLIST_FILE(path);
    return orig_syscall(number, IoRedirect::GetRedirect(path), result);
}
#else

INTERCEPT_SYSCALL(statfs, const char *path, size_t size, struct statfs *result) {
    LOG("path: %s", path);
    IS_BLACKLIST_FILE(path);
    return orig_syscall(number, IoRedirect::GetRedirect(path), size, result);
}

#endif

INTERCEPT_SYSCALL(fstatat, int dir_fd, const char *path, struct stat *buf, int flags) {
    LOG("fd: %d, path: %s, flags: %d", dir_fd, path, flags);

    IS_BLACKLIST_FILE(path);
    const char *redirect = IoRedirect::GetRedirect(path);
    int result = orig_syscall(number, dir_fd, redirect, buf, flags);
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

INTERCEPT_SYSCALL(faccessat, int fd, const char *path, int mode) {
    LOG("fd: %d, path: %s, mode: %d", fd, path, mode);
    IS_BLACKLIST_FILE(path);

    const char *redirect = IoRedirect::GetRedirect(path);
    IoMask mask = IoRedirect::GetFileMask(redirect, mode);
    if (mask == kIMNotFound) {
        return orig_syscall(number, fd, redirect, mode);
    }
    return mask;
}

INTERCEPT_SYSCALL(fchmodat, int fd, const char *path, mode_t mode) {
    LOG("fd: %d, path: %s, mode: %d", fd, path, mode);
    IS_BLACKLIST_FILE(path);
    return orig_syscall(number, fd, IoRedirect::GetRedirect(path), mode);
}

INTERCEPT_SYSCALL(fchownat, int dir_fd, const char *path, uid_t owner, gid_t group, int flags) {
    LOG("fd: %d, path: %s, owner: %d, group: %d, flags: %d", dir_fd, path, owner, group, flags);
    IS_BLACKLIST_FILE(path);
    return orig_syscall(number, dir_fd, IoRedirect::GetRedirect(path), owner, group, flags);
}

INTERCEPT_SYSCALL(linkat, int old_dir_fd, const char *old_path, int new_dir_fd, const char *new_path, int flags) {
    LOG("old fd: %d, old path: %s, new fd: %d, new path: %s, flags: %d", old_dir_fd, old_path, new_dir_fd, new_path, flags);
    IS_BLACKLIST_FILE(old_path);
    return orig_syscall(number, old_dir_fd, IoRedirect::GetRedirect(old_path), new_dir_fd, IoRedirect::GetRedirect(new_path), flags);
}

INTERCEPT_SYSCALL(mkdirat, int dir_fd, const char *path, mode_t mode) {
    LOG("fd: %d, path: %s, mode: %d", dir_fd, path, mode);
    IS_BLACKLIST_FILE(path);
    return orig_syscall(number, dir_fd, IoRedirect::GetRedirectDirectory(path), mode);
}

INTERCEPT_SYSCALL(mknodat, int dir_fd, const char *path, mode_t mode, dev_t dev) {
    LOG("fd: %d, path: %s, mode: %d", dir_fd, path, mode);
    IS_BLACKLIST_FILE(path);
    return orig_syscall(number, dir_fd, IoRedirect::GetRedirect(path), mode, dev);
}

INTERCEPT_SYSCALL(readlinkat, int dir_fd, const char *path, char *buf, size_t buf_size) {
    LOG("fd: %d, path: %s", dir_fd, path);
    IS_BLACKLIST_FILE(path);
    const char *redirect = IoRedirect::GetRedirect(path);
    ssize_t result = orig_syscall(number, dir_fd, redirect, buf, buf_size);
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

INTERCEPT_SYSCALL(renameat, int old_dir_fd, const char *old_path, int new_dir_fd, const char *new_path, unsigned flags) {
    LOG("old fd: %d, old path: %s, new fd: %d, new path: %s flags: %d", old_dir_fd, old_path, new_dir_fd, new_path, flags);
    IS_BLACKLIST_FILE(old_path);
    return orig_syscall(number, old_dir_fd, IoRedirect::GetRedirect(old_path), new_dir_fd, IoRedirect::GetRedirect(new_path), flags);
}

INTERCEPT_SYSCALL(symlinkat, const char *old_path, int new_dir_fd, const char *new_path) {
    LOG("old path: %s, new fd: %d, new path: %s", old_path, new_dir_fd, new_path);
    IS_BLACKLIST_FILE(old_path);
    return orig_syscall(number, IoRedirect::GetRedirect(old_path), new_dir_fd, IoRedirect::GetRedirect(new_path));
}

INTERCEPT_SYSCALL(unlinkat, int dirfd, const char *path, int flags) {
    LOG("fd: %d, path: %s, flags: %d", dirfd, path, flags);
    IS_BLACKLIST_FILE(path);
    return orig_syscall(number, dirfd, IoRedirect::GetRedirect(path), flags);
}

INTERCEPT_SYSCALL(utimensat, int dir_fd, const char *path, const struct timespec times[2], int flags) {
    LOG("fd: %d, path: %s, flags: %d", dir_fd, path, flags);
    IS_BLACKLIST_FILE(path);
    return orig_syscall(number, dir_fd, IoRedirect::GetRedirect(path), times, flags);
}

INTERCEPT_SYSCALL(truncate, const char *path, off_t length) {
    LOG("path: %s, length: %ld", path, length);
    IS_BLACKLIST_FILE(path);
    return orig_syscall(number, IoRedirect::GetRedirectFile(path), length);
}
/*
 * 这里涉及到32位传递64位值的情况,系统寄存器对齐等,因此多设置几个参数
 * truncate64, const char *path, off64_t length
 * */
INTERCEPT_SYSCALL(truncate64, const char *path, int length0, int length1, int length2) {
    LOG("path: %s, length0: %d, length1: %d, length2: %d", path, length0, length1, length2);
    IS_BLACKLIST_FILE(path);
    return orig_syscall(number, IoRedirect::GetRedirectFile(path), length0, length1, length2);
}

INTERCEPT_SYSCALL(chdir, const char *dir) {
    LOG("dir: %s", dir);
    IS_BLACKLIST_FILE(dir);
    return orig_syscall(number, IoRedirect::GetRedirectFile(dir));
}

INTERCEPT_SYSCALL(getcwd, char *buf, size_t size) {
    LOG("getcwd size: %zd", size);
    int result = orig_syscall(number, buf, size);
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

C_API long syscall(long number, ...) {
    void *arg[7];
    va_list list;
    long ret;

    va_start(list, number);
    for (int i = 0; i < 7; ++i) {
        arg[i] = va_arg(list, void *);
    }
    va_end(list);
    if (number == __NR_futex) {
        // 同步系统call会调用很频繁,因此单独过滤
        return get_orig_syscall()(number, arg[0], arg[1], arg[2], arg[3], arg[4], arg[5], arg[6]);
    }
    LOGV("Monitor: syscall invoke number: %ld", number);
    switch (number) {
        case __NR_openat:
            ret = CALL_INTERCEPT(openat, GPOINTER_TO_SIZE(arg[0]), reinterpret_cast<const char *>(arg[1]), GPOINTER_TO_SIZE(arg[2]), GPOINTER_TO_SIZE(arg[3]));
            break;
        case __NR_faccessat:
            ret = CALL_INTERCEPT(faccessat, GPOINTER_TO_SIZE(arg[0]), reinterpret_cast<const char *>(arg[1]), GPOINTER_TO_SIZE(arg[2]));
            break;
        case __NR_fchmodat:
            ret = CALL_INTERCEPT(fchmodat, GPOINTER_TO_SIZE(arg[0]), reinterpret_cast<const char *>(arg[1]), GPOINTER_TO_SIZE(arg[2]));
            break;
        case __NR_fchownat:
            ret = CALL_INTERCEPT(fchownat, GPOINTER_TO_SIZE(arg[0]), reinterpret_cast<const char *>(arg[1]), GPOINTER_TO_SIZE(arg[2]), GPOINTER_TO_SIZE(arg[3]),
                                 GPOINTER_TO_SIZE(arg[4]));
            break;
#if defined(__LP64__)
            //            case __NR_fstatat:
                        case __NR_newfstatat:
#else
        case __NR_fstatat64:
#endif
            ret = CALL_INTERCEPT(fstatat, GPOINTER_TO_SIZE(arg[0]), reinterpret_cast<const char *>(arg[1]), reinterpret_cast<struct stat *>(arg[2]), GPOINTER_TO_SIZE(arg[3]));
            break;
        case __NR_linkat:
            ret = CALL_INTERCEPT(linkat, GPOINTER_TO_SIZE(arg[0]), reinterpret_cast<const char *>(arg[1]), GPOINTER_TO_SIZE(arg[2]), reinterpret_cast<const char *>(arg[3]),
                                 GPOINTER_TO_SIZE(arg[4]));
            break;
        case __NR_mkdirat:
            ret = CALL_INTERCEPT(mkdirat, GPOINTER_TO_SIZE(arg[0]), reinterpret_cast<const char *>(arg[1]), GPOINTER_TO_SIZE(arg[2]));
            break;
        case __NR_mknodat:
            ret = CALL_INTERCEPT(mknodat, GPOINTER_TO_SIZE(arg[0]), reinterpret_cast<const char *>(arg[1]), GPOINTER_TO_SIZE(arg[2]), GPOINTER_TO_SIZE(arg[3]));
            break;
        case __NR_readlinkat:
            ret = CALL_INTERCEPT(readlinkat, GPOINTER_TO_SIZE(arg[0]), reinterpret_cast<const char *>(arg[1]), reinterpret_cast<char *>(arg[2]), GPOINTER_TO_SIZE(arg[3]));
            break;
        case __NR_renameat:
        case __NR_renameat2:
            ret = CALL_INTERCEPT(renameat, GPOINTER_TO_SIZE(arg[0]), reinterpret_cast<const char *>(arg[1]), GPOINTER_TO_SIZE(arg[2]), reinterpret_cast<const char *>(arg[3]),
                                 GPOINTER_TO_SIZE(arg[4]));
            break;
        case __NR_symlinkat:
            ret = CALL_INTERCEPT(symlinkat, reinterpret_cast<const char *>(arg[0]), GPOINTER_TO_SIZE(arg[1]), reinterpret_cast<const char *>(arg[2]));
            break;
        case __NR_unlinkat:
            ret = CALL_INTERCEPT(unlinkat, GPOINTER_TO_SIZE(arg[0]), reinterpret_cast<const char *>(arg[1]), GPOINTER_TO_SIZE(arg[2]));
            break;
        case __NR_utimensat:
            ret = CALL_INTERCEPT(utimensat, GPOINTER_TO_SIZE(arg[0]), reinterpret_cast<const char *>(arg[1]), reinterpret_cast<const timespec *>(arg[2]), GPOINTER_TO_SIZE(arg[3]));
            break;
#if defined(__LP64__)
            case __NR_truncate:
                ret = CALL_INTERCEPT(truncate, reinterpret_cast<const char *>(arg[0]), GPOINTER_TO_SIZE(arg[1]));
                 break;
#else
        case __NR_truncate:
            ret = CALL_INTERCEPT(truncate, reinterpret_cast<const char *>(arg[0]), GPOINTER_TO_SIZE(arg[1]));
            break;
        case __NR_truncate64:
            ret = CALL_INTERCEPT(truncate64, reinterpret_cast<const char *>(arg[0]), GPOINTER_TO_SIZE(arg[1]), GPOINTER_TO_SIZE(arg[2]), GPOINTER_TO_SIZE(arg[3]));
            break;
#endif

#if defined(__LP64__)
            case __NR_statfs:
                 ret = CALL_INTERCEPT(statfs, reinterpret_cast<const char *>(arg[0]), reinterpret_cast<struct statfs *>(arg[1]));
                break;
#else
        case __NR_statfs64:
            ret = CALL_INTERCEPT(statfs, reinterpret_cast<const char *>(arg[0]), GPOINTER_TO_SIZE(arg[1]), reinterpret_cast<struct statfs *>(arg[2]));
            break;
#endif
        case __NR_chdir:
            ret = CALL_INTERCEPT(chdir, reinterpret_cast<const char *>(arg[0]));
            break;
        case __NR_getcwd:
            ret = CALL_INTERCEPT(getcwd, reinterpret_cast<char *>(arg[0]), GPOINTER_TO_SIZE(arg[1]));
            break;
        default:
            ret = orig_syscall(number, arg[0], arg[1], arg[2], arg[3], arg[4], arg[5], arg[6]);
            break;
    }

    LOGV("Monitor: 'syscall' invoke number: %ld, result: %ld", number, ret);
    return ret;
}