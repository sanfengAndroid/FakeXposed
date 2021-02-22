//
// Created by beich on 2021/2/19.
//

#include "hook_env.h"

FUN_INTERCEPT HOOK_DEF(char*, getenv, const char *name) {
    LOGMV("Monitor: getenv name: %s", name);
    char *value = get_orig_getenv()(name);
    if (value == nullptr) {
        return value;
    }
    char *result = FindEnvReplace(name, value);
    return result == nullptr ? value : result;
}
