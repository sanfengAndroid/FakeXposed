//
// Created by beich on 2020/12/28.
//

#include "hook_string.h"


FUN_INTERCEPT HOOK_DEF(int, strcasecmp, const char *s1, const char *s2) {
    LOGMV("s1: %s, s2: %s\n", s1, s2);
    return get_orig_strcasecmp()(s1, s2);
}

FUN_INTERCEPT HOOK_DEF_CPP(const char *, strcasestr, const char *haystack, const char *needle) {
    LOGMV("haystack: %s, needle: %s", haystack, needle);
    return get_orig_strcasestr()(haystack, needle);
}

