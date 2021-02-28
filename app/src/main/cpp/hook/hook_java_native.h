//
// Created by beich on 2021/2/26.
//

#pragma once

#include "hook_common.h"

class JNHook{
public:
    static bool InitJavaNativeHook(JNIEnv *env);
};

