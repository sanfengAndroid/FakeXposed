//
// Created by beich on 2020/12/28.
//

#include "hook_common.h"
#include <fcntl.h>

int force_O_LARGEFILE(int flags) {
#if defined(__LP64__)
    return flags; // No need, and aarch64's strace gets confused.
#else
    return flags | O_LARGEFILE;
#endif
}