//
// Created by beich on 2020/12/18.
//

#pragma once

#include <jni.h>
#include <map>
#include <linker_export.h>
#include <macros.h>

class JNIInterfaceMonitor {
public:
    static void Init(const RemoteInvokeInterface *remote, JNIEnv *env);

    static JNIInterfaceMonitor *Get();

    int InitHookJNIInterfaces();

    /*
    * @param name so名称
    * @param contain true则只监听该库,false则监听除该库外的库,如果跟已存在的类型不同则会清空之前的类型
    **/
    void AddLibraryMonitor(const char *name, bool contain);

    void RemoveLibraryMonitor(const char *name);

    void AddAddressMonitor(uintptr_t start, size_t end, bool contain);

    void RemoveAddressMonitor(uintptr_t start, size_t end);

    bool InMonitoring(uintptr_t addr);

private:
    JNIInterfaceMonitor(const RemoteInvokeInterface *remote, JNIEnv *env);

private:
    const RemoteInvokeInterface *const remote_;
    JNINativeInterface *original_interface;
    static JNIInterfaceMonitor *singleton;
    std::map<size_t, size_t> monitors_;
    bool contain_ = true;
    DISALLOW_COPY_AND_ASSIGN(JNIInterfaceMonitor);
};