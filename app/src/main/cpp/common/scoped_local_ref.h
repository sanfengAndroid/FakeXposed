/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#pragma once

#include <jni.h>

#include <stddef.h>

#include "macros.h"
#include "proxy_jni.h"


template<typename T>
class ScopedLocalRef {
public:
    ScopedLocalRef(JNIEnv *env, T localRef) : mEnv(env), proxy_(nullptr), mLocalRef(localRef) {
    }

    ScopedLocalRef(ProxyJNIEnv *proxy, T localRef) : mEnv(nullptr), proxy_(proxy), mLocalRef(localRef) {
    }

    ~ScopedLocalRef() {
        reset();
    }

    void reset(T ptr = nullptr) {
        if (ptr != mLocalRef) {
            if (mLocalRef != nullptr) {
                if (proxy_ != nullptr) {
                    proxy_->DeleteLocalRef(mLocalRef);
                } else {
                    mEnv->DeleteLocalRef(mLocalRef);
                }
            }
            mLocalRef = ptr;
        }
    }

    T release() {
        T localRef = mLocalRef;
        mLocalRef = nullptr;
        return localRef;
    }

    T get() const {
        return mLocalRef;
    }

    // Allows "if (scoped_ref == nullptr)"
    bool operator==(std::nullptr_t) const {
        return mLocalRef == nullptr;
    }

    // Allows "if (scoped_ref != nullptr)"
    bool operator!=(std::nullptr_t) const {
        return mLocalRef != nullptr;
    }

private:
    JNIEnv *const mEnv;
    T mLocalRef;
    ProxyJNIEnv *const proxy_;

    DISALLOW_COPY_AND_ASSIGN(ScopedLocalRef);
};


