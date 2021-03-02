//
// Created by beich on 2021/2/26.
//

#include "hook_java_native.h"

#include <jni_helper.h>
#include <scoped_utf_chars.h>
#include <sys/system_properties.h>


#ifndef NELEM
#define NELEM(x) ((int) (sizeof(x) / sizeof((x)[0])))
#endif

#define NATIVE_HOOK_DEF(ret, func, ...)         \
    static ret (*orig_##func)(__VA_ARGS__);     \
    static ret func(__VA_ARGS__)


#define NATIVE_METHOD(className, functionName, signature, isStatic) \
{{ #functionName,  signature, (void*)(className ## _ ## functionName) }, isStatic, (void**) (&orig_ ## className ## _ ## functionName)}

#define FAST_NATIVE_METHOD(className, functionName, signature, isStatic) \
{{ #functionName, "!" signature, (void*)(className ## _ ## functionName) }, isStatic, (void**) (&orig_ ## className ## _ ## functionName)}

struct {
    jfieldID java_lang_StackTraceElement_declaringClass;
    jclass java_lang_StackTraceElement;
    jclass java_lang_ClassNotFoundException;
} java_cache;

static jclass FindClass(JNIEnv *env, const char *name) {
    jclass clazz = env->FindClass(name);
    if (clazz == nullptr) {
        JNIHelper::PrintAndClearException(env);
    }
    return clazz;
}


NATIVE_HOOK_DEF(jboolean, VMDebug_isDebuggerConnected, JNIEnv *, jclass) {
    LOGD("fake VMDebug_isDebuggerConnected");
    return JNI_FALSE;
}

NATIVE_HOOK_DEF(jobjectArray, Throwable_nativeGetStackTrace, JNIEnv *env, jclass clazz, jobject javaStackState) {
    jobjectArray stacks = orig_Throwable_nativeGetStackTrace(env, clazz, javaStackState);
    jsize len = env->GetArrayLength(stacks);
    std::vector<int> deletes;
    for (int i = 0; i < len; ++i) {
        ScopedLocalRef<jobject> obj(env, env->GetObjectArrayElement(stacks, i));
        ScopedLocalRef<jstring> declaring_class(env, (jstring) env->GetObjectField(obj.get(), java_cache.java_lang_StackTraceElement_declaringClass));
        ScopedUtfChars name(env, declaring_class.get());
        if (FXHandler::StackClassNameBlacklisted(name.c_str())) {
            LOGMD("found black list name: %s", name.c_str());
            deletes.push_back(i);
        }
    }
    if (deletes.empty()) {
        return stacks;
    }
    jsize new_len = len - deletes.size();
    if (new_len == 0) {
        return nullptr;
    }
    jobjectArray new_stacks = env->NewObjectArray(new_len, java_cache.java_lang_StackTraceElement, nullptr);
    jsize pos = 0;
    for (int i = 0; i < len; ++i) {
        if (std::find(deletes.begin(), deletes.end(), i) == deletes.end()) {
            ScopedLocalRef<jobject> obj(env, env->GetObjectArrayElement(stacks, i));
            env->SetObjectArrayElement(new_stacks, pos++, obj.get());
        }
    }
    return new_stacks;
}

// static native Class<?> classForName(String className, boolean shouldInitialize,
//        ClassLoader classLoader) throws ClassNotFoundException;
NATIVE_HOOK_DEF(jclass, Class_classForName, JNIEnv *env, jclass clazz, jstring className, jboolean shouldInitialize, jobject classLoader) {
    ScopedUtfChars name(env, className, false);
    LOGMV("Fake classForName: %s", name.c_str());
    if (FXHandler::ClassNameBlacklisted(name.c_str())) {
        std::string message = "Invalid name: ";
        message += name.c_str();
        env->ThrowNew(java_cache.java_lang_ClassNotFoundException, message.c_str());
        return nullptr;
    }
    return orig_Class_classForName(env, clazz, className, shouldInitialize, classLoader);
}

static void initVectorFromBlock(const char **vector, const char *block, int count) {
    int i;
    const char *p;
    for (i = 0, p = block; i < count; i++) {
        /* Invariant: p always points to the start of a C string. */
        vector[i] = p;
        while (*(p++));
    }
    vector[count] = nullptr;
}

NATIVE_HOOK_DEF(jint, UNIXProcess_forkAndExec, JNIEnv *env, jobject process,
                jbyteArray prog, jbyteArray argBlock, jint argc, jbyteArray envBlock,
                jint envc, jbyteArray dir, jintArray std_fds, jboolean redirectErrorStream) {
    const char *cmd = reinterpret_cast<const char *>(env->GetByteArrayElements(prog, nullptr));
    const char *argv = reinterpret_cast<const char *>(env->GetByteArrayElements(argBlock, nullptr));
    const char *pargv[argc + 1];
    initVectorFromBlock(pargv, argv, argc);
    LOGMV("runtime exec cmd: %s", cmd);
    for (int i = 0; i < argc; ++i) {
        LOGMV("exec parameter %d: %s", i, pargv[i]);
    }
    jint pid;
    RuntimeBean *bean = FXHandler::FindRuntimeBean(cmd, pargv, argc);
    if (bean == nullptr) {
        pid = orig_UNIXProcess_forkAndExec(env, process, prog, argBlock, argc, envBlock, envc, dir, std_fds, redirectErrorStream);
    } else {
        const char *new_cmd = nullptr;
        const char *new_argv = nullptr;
        jint new_argc = argc;
        jsize block_length = 0;
        FXHandler::RuntimeReplaceCommandArgv(bean, &new_cmd, &new_argv, &block_length, &new_argc);
        jbyteArray new_prog = prog;
        jbyteArray new_argBlock = argBlock;
        if (new_cmd != nullptr) {
            jsize len = strlen(new_cmd) + 1;
            new_prog = env->NewByteArray(len);
            env->SetByteArrayRegion(new_prog, 0, len, reinterpret_cast<const jbyte *>(new_cmd));
        }
        if (new_argv != nullptr || new_argc != argc) {
            // 当没有参数时无需保证argBlock正确
            if (new_argc != 0) {
                new_argBlock = env->NewByteArray(block_length);
                env->SetByteArrayRegion(new_argBlock, 0, block_length, reinterpret_cast<const jbyte *>(new_argv));
            }
        }
        pid = orig_UNIXProcess_forkAndExec(env, process, new_prog, new_argBlock, new_argc, envBlock, envc, dir, std_fds, redirectErrorStream);
        if (!env->ExceptionCheck()) {
            // 命令结束后就有对应的流产生
            // fds[0] stdin fds[1] stdout fds[2] stderr
            jint *fds = env->GetIntArrayElements(std_fds, nullptr);
            int redirect[3] = {fds[0], fds[1], fds[2]};
            JNIHelper::ReleaseInts(env, std_fds, fds);
            if (FXHandler::RuntimeReplaceStream(bean, redirect)) {
                env->SetIntArrayRegion(std_fds, 0, 3, redirect);
            }
        }
    }
    JNIHelper::ReleaseBytes(env, prog, cmd);
    JNIHelper::ReleaseBytes(env, argBlock, argv);
    return pid;
}

static int GetFileDescriptorOfFD(JNIEnv *env, jobject fileDescriptor, jfieldID FileDescriptor_descriptor) {
    if (fileDescriptor == nullptr) {
        return -1;
    }
    return env->GetIntField(fileDescriptor, FileDescriptor_descriptor);
}

static void SetFileDescriptorOfFD(JNIEnv *env, jobject fileDescriptor, jfieldID FileDescriptor_descriptor, int value) {
    if (fileDescriptor == nullptr) {
        return;
    }
    env->SetIntField(fileDescriptor, FileDescriptor_descriptor, value);
}

NATIVE_HOOK_DEF(pid_t, ProcessManager_exec, JNIEnv *env, jclass clazz, jobjectArray javaCommands,
                jobjectArray javaEnvironment, jstring javaWorkingDirectory, jobject inDescriptor,
                jobject outDescriptor, jobject errDescriptor, jboolean redirectErrorStream) {
    pid_t pid;
    int length = env->GetArrayLength(javaCommands);
    char *array[length + 1];
    array[length] = nullptr;
    for (jsize i = 0; i < length; ++i) {
        ScopedLocalRef<jstring> java_string(env, reinterpret_cast<jstring>(env->GetObjectArrayElement(javaCommands, i)));
        char *string = const_cast<char *>(env->GetStringUTFChars(java_string.get(), nullptr));
        array[i] = string;
    }

    LOGMV("exec command: %s", array[0]);
    const char *old_argv = length > 1 ? array[1] : nullptr;
    RuntimeBean *bean = FXHandler::FindRuntimeBean(array[0], &old_argv, length - 1);
    if (bean == nullptr) {
        pid = orig_ProcessManager_exec(env, clazz, javaCommands, javaEnvironment, javaWorkingDirectory, inDescriptor, outDescriptor, errDescriptor, redirectErrorStream);
    } else {
        const char *new_cmd = nullptr;
        const char *new_argv = nullptr;
        jsize block_size = 0;
        jsize new_argc = length - 1;
        jobjectArray javaNewCommands = javaCommands;
        if (FXHandler::RuntimeReplaceCommandArgv(bean, &new_cmd, &new_argv, &block_size, &new_argc)) {
            javaNewCommands = env->NewObjectArray(new_argc + 1, JNIHelper::java_lang_String, nullptr);
            if (new_cmd == nullptr) {
                new_cmd = array[0];
            }
            ScopedLocalRef<jstring> cmd_(env, env->NewStringUTF(new_cmd));
            env->SetObjectArrayElement(javaNewCommands, 0, cmd_.get());
            if ((new_argv != nullptr || new_argc != length - 1)) {
                if (new_argc > 0){
                    const char *vectors[new_argc];
                    initVectorFromBlock(vectors, new_argv, new_argc);
                    for (int i = 0; i < new_argc; ++i) {
                        ScopedLocalRef<jstring> arg(env,env->NewStringUTF(vectors[i]));
                        env->SetObjectArrayElement(javaNewCommands, i + 1, arg.get());
                    }
                }
            }else{
                for (int i = 1; i < new_argc; ++i) {
                    ScopedLocalRef<jstring> arg(env,env->NewStringUTF(array[i]));
                    env->SetObjectArrayElement(javaNewCommands, i, arg.get());
                }
            }
        }
        pid = orig_ProcessManager_exec(env, clazz, javaNewCommands, javaEnvironment, javaWorkingDirectory, inDescriptor, outDescriptor, errDescriptor, redirectErrorStream);
        if (!env->ExceptionCheck()) {
            static jfieldID FileDescriptor_descriptor = nullptr;
            if (FileDescriptor_descriptor == nullptr) {
                ScopedLocalRef<jclass> sz(env, FindClass(env, "java/io/FileDescriptor"));
                FileDescriptor_descriptor = env->GetFieldID(sz.get(), "descriptor", "I");
                CHECK(FileDescriptor_descriptor);
            }
            int redirect[3] = {
                    GetFileDescriptorOfFD(env, outDescriptor, FileDescriptor_descriptor),
                    GetFileDescriptorOfFD(env, inDescriptor, FileDescriptor_descriptor),
                    GetFileDescriptorOfFD(env, errDescriptor, FileDescriptor_descriptor)
            };
            if (FXHandler::RuntimeReplaceStream(bean, redirect)) {
                SetFileDescriptorOfFD(env, outDescriptor, FileDescriptor_descriptor, redirect[0]);
                SetFileDescriptorOfFD(env, inDescriptor, FileDescriptor_descriptor, redirect[1]);
                SetFileDescriptorOfFD(env, errDescriptor, FileDescriptor_descriptor, redirect[2]);
            }
        }
    }

    jthrowable pending_exception = env->ExceptionOccurred();
    if (pending_exception != nullptr) {
        env->ExceptionClear();
    }
    for (jsize i = 0; i < length; ++i) {
        ScopedLocalRef<jstring> java_string(env, reinterpret_cast<jstring>(env->GetObjectArrayElement(javaCommands, i)));
        env->ReleaseStringUTFChars(java_string.get(), array[i]);
    }
    return pid;
}

struct {
    HookRegisterNativeUnit gVMDebug =
            FAST_NATIVE_METHOD(VMDebug, isDebuggerConnected, "()Z", true);
    HookRegisterNativeUnit gThrowable =
            FAST_NATIVE_METHOD(Throwable, nativeGetStackTrace, "(Ljava/lang/Object;)[Ljava/lang/StackTraceElement;", true);
    HookRegisterNativeUnit gClass =
            FAST_NATIVE_METHOD(Class, classForName, "(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;", true);

    HookRegisterNativeUnit gUNIXProcess = NATIVE_METHOD(UNIXProcess, forkAndExec, "([B[BI[BI[B[IZ)I", false);
    HookRegisterNativeUnit gProcessManager =
            NATIVE_METHOD(ProcessManager, exec,
                          "([Ljava/lang/String;[Ljava/lang/String;Ljava/lang/String;Ljava/io/FileDescriptor;Ljava/io/FileDescriptor;Ljava/io/FileDescriptor;Z)I", true);
} java_hooks;

static void CacheJNIData(JNIEnv *env) {
    java_cache.java_lang_StackTraceElement = JNIHelper::CacheClass(env, "java/lang/StackTraceElement");
    java_cache.java_lang_StackTraceElement_declaringClass = env->GetFieldID(java_cache.java_lang_StackTraceElement, "declaringClass", JNIHelper::java_lang_String_sign);
    java_cache.java_lang_ClassNotFoundException = JNIHelper::CacheClass(env, "java/lang/ClassNotFoundException");
}

static int RegisterNativeMethods(JNIEnv *env, const char *class_name, HookRegisterNativeUnit *items, int len) {
    ScopedLocalRef<jclass> c(env, FindClass(env, class_name));
    if (c.get() == nullptr) {
        return -1;
    }
    return remote->HookNativeFunction(env, c.get(), items, len);
}

bool JNHook::InitJavaNativeHook(JNIEnv *env) {
    JNIHelper::Init(env);
    CacheJNIData(env);
    LOGD("register VMDebug result: %d", RegisterNativeMethods(env, "dalvik/system/VMDebug", &java_hooks.gVMDebug, 1));
    LOGD("register Throwable result: %d", RegisterNativeMethods(env, "java/lang/Throwable", &java_hooks.gThrowable, 1));
    LOGD("register Throwable result: %d", RegisterNativeMethods(env, "java/lang/Class", &java_hooks.gClass, 1));
    if (FXHandler::Get()->api >= __ANDROID_API_N__) {
        LOGD("register UNIXProcess result: %d", RegisterNativeMethods(env, "java/lang/UNIXProcess", &java_hooks.gUNIXProcess, 1));
    } else {
        LOGD("register ProcessManager result: %d", RegisterNativeMethods(env, "java/lang/ProcessManager", &java_hooks.gProcessManager, 1));
    }
    return true;
}


