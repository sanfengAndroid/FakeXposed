//
// Created by beich on 2020/12/28.
//

#include "hook_vfs.h"
#include <unistd.h>
#include <syscall.h>
#include <sys/statfs.h>
#include <sys/statvfs.h>

#include "hook_syscall.h"

static __inline void __bionic_statfs_to_statvfs(const struct statfs *__src, struct statvfs *__dst) {
    __dst->f_bsize = __src->f_bsize;
    __dst->f_frsize = __src->f_frsize;
    __dst->f_blocks = __src->f_blocks;
    __dst->f_bfree = __src->f_bfree;
    __dst->f_bavail = __src->f_bavail;
    __dst->f_files = __src->f_files;
    __dst->f_ffree = __src->f_ffree;
    __dst->f_favail = __src->f_ffree;
    __dst->f_fsid = __src->f_fsid.__val[0] | __BIONIC_CAST(static_cast, uint64_t, __src->f_fsid.__val[1]) << 32;
    __dst->f_flag = __src->f_flags;
    __dst->f_namemax = __src->f_namelen;
}


// fstatfs 传入fd已更改
USE_SYSCALL HOOK_DEF(int, statfs, const char *path, struct statfs *result) {
#define ST_VALID 0x0020
#if defined(__LP64__)
    int rc = syscall(__NR_statfs, path, result);
#else
    int rc = syscall(__NR_statfs64, path, sizeof(*result), result);
#endif
    if (rc != 0) {
        return rc;
    }
    result->f_flags &= ~ST_VALID;
    return 0;
}
__strong_alias(statfs64, statfs);

USE_SYSCALL HOOK_DEF(int, __statfs64, const char *path, size_t size, struct statfs *result) {
#if defined(__LP64__)
    int rc = syscall(__NR_statfs, path, result);
#else
    int rc = syscall(__NR_statfs64, path, sizeof(*result), result);
#endif
    return rc;
}

STUB_SYMBOL HOOK_DEF(int, statvfs, const char *__path, struct statvfs *__result) {
    struct statfs tmp;
    int rc = statfs(__path, &tmp);
    if (rc != 0) {
        return rc;
    }
    __bionic_statfs_to_statvfs(&tmp, __result);
    return 0;
}
__strong_alias(statvfs64, statvfs);