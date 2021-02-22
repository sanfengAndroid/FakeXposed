//
// Created by beich on 2020/12/28.
//

#include "hook_time.h"
#include <ctime>

#include "io_redirect.h"

FUN_INTERCEPT HOOK_DEF(int, utimes, const char *path, const timeval tv[2]) {
    IS_BLACKLIST_FILE(path);
    return get_orig_utimes()(IoRedirect::GetRedirect(path), tv);
}

FUN_INTERCEPT HOOK_DEF(int, utime, const char *path, const void *times) {
    IS_BLACKLIST_FILE(path);
    return get_orig_utime()(IoRedirect::GetRedirect(path), times);
}


FUN_INTERCEPT HOOK_DEF(int, lutimes, const char *path, const struct timeval times[2]) {
    IS_BLACKLIST_FILE(path);
    return get_orig_lutimes()(IoRedirect::GetRedirect(path), times);
}