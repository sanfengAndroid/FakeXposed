//
// Created by beich on 2020/11/18.
//

#pragma once

#include <cstdarg>
#include <malloc.h>
#include <string.h>

template<class T>
struct VarLengthObject {
    size_t len;
    T elements[0];
};

template<class T>
VarLengthObject<T> *VarLengthObjectAlloc(int len) {
    size_t size = sizeof(VarLengthObject<T>) + sizeof(T) * len;
    auto *ret = reinterpret_cast<VarLengthObject<T> *>(malloc(size));
    memset(ret, 0, size);
    ret->len = len;
    return ret;
}

template<class T>
void VarLengthObjectFree(VarLengthObject<T> *var) {
    if (var != nullptr) {
        free(var);
    }
}

template<class T>
class VarLengthRef {
public:
    explicit VarLengthRef(VarLengthObject<T> *_data) : data(_data) {}

    explicit VarLengthRef(int len) {
        data = VarLengthObjectAlloc<T>(len);
    }

    VarLengthRef(VarLengthRef<T> &&that) noexcept {
        data = that.data;
        that.data = nullptr;
    }

    ~VarLengthRef() {
        if (data != nullptr) {
            VarLengthObjectFree(data);
        }
    }

    VarLengthObject<T> *data;
#ifndef DISALLOW_COPY_AND_ASSIGN
    #define DISALLOW_COPY_AND_ASSIGN(TypeName) \
    TypeName(const TypeName&) = delete;             \
    void operator=(const TypeName&) = delete
#endif
    DISALLOW_COPY_AND_ASSIGN(VarLengthRef);
};


template<class T>
VarLengthObject<T> *VaListToVarLengthObject(int len, va_list &list) {
    if (len < 1) {
        return nullptr;
    }
    VarLengthObject<T> *vas = VarLengthObjectAlloc<T>(len);
    for (int i = 0; i < len; ++i) {
        vas->elements[i] = va_arg(list, T);
    }
    return vas;
}

template<class T>
VarLengthObject<T> *VaArgsToVarLengthObject(int len, ...) {
    if (len < 1) {
        return nullptr;
    }
    va_list list;
    va_start(list, len);

    VarLengthObject<T> *result = VaListToVarLengthObject<T>(len, list);

    va_end(list);
    return result;
}