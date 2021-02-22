//
// Created by beich on 2020/12/28.
//

#pragma once

#include "hook_common.h"


HOOK_DECLARE(int, stat, const char *path, struct stat *buf);