//
// Created by beich on 2020/12/18.
//

#pragma once

#include <string>
#include <jni.h>


class JNIHelper {
public:
    static std::string GetClassName(JNIEnv *env, jclass clazz);

    static std::string GetObjectClassName(JNIEnv *env, jobject obj);

    static std::string GetMethodName(JNIEnv *env, jmethodID mid);

    static std::string GetFieldName(JNIEnv *env, jfieldID fieldId);

    static bool IsClassObject(JNIEnv *env, jobject obj);

    static std::string ToString(JNIEnv *env, jboolean z);

    static std::string ToString(JNIEnv *env, jbyte b);

    static std::string ToString(JNIEnv *env, jchar c);

    static std::string ToString(JNIEnv *env, jshort s);

    static std::string ToString(JNIEnv *env, jint i);

    static std::string ToString(JNIEnv *env, jlong j);

    static std::string ToString(JNIEnv *env, jfloat f);

    static std::string ToString(JNIEnv *env, jdouble d);

    static std::string ToString(JNIEnv *env, jobject l);

    static std::string ToString(JNIEnv *env, jmethodID methodID);

    static std::string ToString(JNIEnv *env, jfieldID fieldID);
public:
    static void Init(JNIEnv *env);

    static void Clear();

public:
    static jclass java_lang_Object;
    static jclass java_lang_Class;
    static jclass java_lang_reflect_Method;
    static jclass java_lang_reflect_Field;

    static jmethodID java_lang_Object_toString;
    static jmethodID java_lang_Object_getClass;
    static jmethodID java_lang_reflect_Field_getName;
    static jmethodID java_lang_Class_getName;
    static jmethodID java_lang_reflect_Method_getName;
private:
    static bool init_;
};
