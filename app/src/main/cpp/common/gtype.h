//
// Created by lenovo-s on 2019/4/15.
//
#pragma once

#include <stdint.h>

#define LINE_MAX 4096

typedef void *gpointer;
typedef intptr_t gssize;
typedef uintptr_t gsize;
typedef uint64_t gaddress;
#define GSIZE_TO_POINTER(s)    ((gpointer) (gsize) (s))
#define GPOINTER_TO_SIZE(p)    ((gsize) (p))
