/*
 * Copyright (C) 2011 The Android Open Source Project
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


#include "exec_strings.h"
#include "scoped_local_ref.h"

ExecStrings::ExecStrings(JNIEnv *env, jobjectArray java_string_array)
        : env_(env), length(0), java_array_(java_string_array), array_(nullptr) {
    if (java_array_ == nullptr) {
        return;
    }

    length = env_->GetArrayLength(java_array_);
    array_ = new char *[length + 1];
    array_[length] = nullptr;
    for (jsize i = 0; i < length; ++i) {
        ScopedLocalRef<jstring> java_string(env_, reinterpret_cast<jstring>(env_->GetObjectArrayElement(java_array_, i)));
        // We need to pass these strings to const-unfriendly code.
        char *string = const_cast<char *>(env_->GetStringUTFChars(java_string.get(), nullptr));
        array_[i] = string;
    }
}

ExecStrings::~ExecStrings() {
    if (array_ == nullptr) {
        return;
    }

    // Temporarily clear any pending exception so we can clean up.
    jthrowable pending_exception = env_->ExceptionOccurred();
    if (pending_exception != nullptr) {
        env_->ExceptionClear();
    }
    for (jsize i = 0; i < length; ++i) {
        ScopedLocalRef<jstring> java_string(env_, reinterpret_cast<jstring>(env_->GetObjectArrayElement(java_array_, i)));
        env_->ReleaseStringUTFChars(java_string.get(), array_[i]);
    }
    delete[] array_;

    // Re-throw any pending exception.
    if (pending_exception != nullptr) {
        if (env_->Throw(pending_exception) < 0) {
            LOGE("Error rethrowing exception!");
        }
    }
}

char **ExecStrings::get() {
    return array_;
}