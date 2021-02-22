//
// Created by beich on 2020/11/20.
//

#pragma once

#include <string.h>
#include <jni.h>
#include "macros.h"
#include "scoped_local_ref.h"

inline bool JniThrowNullPointerException(JNIEnv *env, const char *msg) {
    if (env->ExceptionCheck()) {
        env->ExceptionClear();
    }

    ScopedLocalRef<jclass> exc_class(env, env->FindClass("java/lang/NullPointerException"));
    if (exc_class.get() == nullptr) {
        return -1;
    }

    return env->ThrowNew(exc_class.get(), msg) == JNI_OK;
}

class ScopedUtfChars {
public:
    ScopedUtfChars(JNIEnv *env, jstring s) : env_(env), string_(s) {
        if (s == nullptr) {
            utf_chars_ = nullptr;
            JniThrowNullPointerException(env, nullptr);
        } else {
            utf_chars_ = env->GetStringUTFChars(s, nullptr);
        }
    }

    ScopedUtfChars(JNIEnv *env, jstring s, bool check_null) : env_(env), string_(s) {
        if (s == nullptr) {
            utf_chars_ = nullptr;
            if (check_null) {
                JniThrowNullPointerException(env, nullptr);
            }
        } else {
            utf_chars_ = env->GetStringUTFChars(s, nullptr);
        }
    }

    ScopedUtfChars(ScopedUtfChars &&rhs) :
            env_(rhs.env_), string_(rhs.string_), utf_chars_(rhs.utf_chars_) {
        rhs.env_ = nullptr;
        rhs.string_ = nullptr;
        rhs.utf_chars_ = nullptr;
    }

    ~ScopedUtfChars() {
        if (utf_chars_) {
            env_->ReleaseStringUTFChars(string_, utf_chars_);
        }
    }

    ScopedUtfChars &operator=(ScopedUtfChars &&rhs) {
        if (this != &rhs) {
            // Delete the currently owned UTF chars.
            this->~ScopedUtfChars();

            // Move the rhs ScopedUtfChars and zero it out.
            env_ = rhs.env_;
            string_ = rhs.string_;
            utf_chars_ = rhs.utf_chars_;
            rhs.env_ = nullptr;
            rhs.string_ = nullptr;
            rhs.utf_chars_ = nullptr;
        }
        return *this;
    }

    const char *c_str() const {
        return utf_chars_;
    }

    size_t size() const {
        return strlen(utf_chars_);
    }

    const char &operator[](size_t n) const {
        return utf_chars_[n];
    }

private:
    JNIEnv *env_;
    jstring string_;
    const char *utf_chars_;

    DISALLOW_COPY_AND_ASSIGN(ScopedUtfChars);
};
