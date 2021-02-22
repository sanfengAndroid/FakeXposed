/*
 * Copyright (c) 2021 FakeXposed by sanfengAndroid.
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
 *
 */

package com.sanfengandroid.xp.hooks;

import android.annotation.SuppressLint;

import com.sanfengandroid.common.Const;
import com.sanfengandroid.common.model.base.DataModelType;
import com.sanfengandroid.common.util.LogUtil;
import com.sanfengandroid.fakeinterface.GlobalConfig;

import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

/**
 * 使用native代替
 */
public class HookSystemProperties implements IHook {
    private static final String TAG = HookSystemProperties.class.getSimpleName();

    @Override
    public void hook(ClassLoader loader) throws Throwable {
        hookString(loader);
        hookInt(loader);
        hookLong(loader);
        hookBoolean(loader);
    }

    @SuppressLint("PrivateApi")
    private void hookString(ClassLoader loader) throws Exception {
        Class<?> clazz = Class.forName("android.os.SystemProperties", false, loader);
        Method method = clazz.getMethod("get", String.class);
        Method method1 = clazz.getMethod("get", String.class, String.class);
        GlobalConfig.addHookMethodModifierFilter(method, method1);

        XC_MethodHook callback = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                String name = (String) param.args[0];
                String value = GlobalConfig.getEqualBlacklistStringValue(name, DataModelType.GLOBAL_SYSTEM_PROPERTY_HIDE);
                if (value != null) {
                    param.setResult(value);
                    LogUtil.w(Const.JAVA_MONITOR_STATE, "Modify SystemProperties name: %s, value: %s", name, value);
                }
            }
        };

        XposedBridge.hookMethod(method, callback);
        XposedBridge.hookMethod(method1, callback);
    }

    @SuppressLint("PrivateApi")
    private void hookInt(ClassLoader loader) throws Exception {
        Class<?> clazz = Class.forName("android.os.SystemProperties", false, loader);
        Method method = clazz.getMethod("getInt", String.class, int.class);
        GlobalConfig.addHookMethodModifierFilter(method);
        XposedBridge.hookMethod(method, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                String name = (String) param.args[0];
                String value = GlobalConfig.getEqualBlacklistStringValue(name, DataModelType.GLOBAL_SYSTEM_PROPERTY_HIDE);
                if (value != null) {
                    param.setResult(Integer.parseInt(value));
                    LogUtil.w(Const.JAVA_MONITOR_STATE,  "Modify SystemProperties name: %s, value: %s", name, value);
                }
            }
        });
    }

    @SuppressLint("PrivateApi")
    private void hookLong(ClassLoader loader) throws Exception {
        Class<?> clazz = Class.forName("android.os.SystemProperties", false, loader);
        Method method = clazz.getMethod("getLong", String.class, long.class);
        GlobalConfig.addHookMethodModifierFilter(method);
        XposedBridge.hookMethod(method, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                String name = (String) param.args[0];
                String value = GlobalConfig.getEqualBlacklistStringValue(name, DataModelType.GLOBAL_SYSTEM_PROPERTY_HIDE);
                if (value != null) {
                    param.setResult(Long.parseLong(value));
                    LogUtil.w(Const.JAVA_MONITOR_STATE,  "Modify SystemProperties name: %s, value: %s", name, value);
                }
            }
        });
    }

    @SuppressLint("PrivateApi")
    private void hookBoolean(ClassLoader loader) throws Exception {
        Class<?> clazz = Class.forName("android.os.SystemProperties", false, loader);
        Method method = clazz.getMethod("getBoolean", String.class, boolean.class);
        GlobalConfig.addHookMethodModifierFilter(method);
        XposedBridge.hookMethod(method, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                String name = (String) param.args[0];
                String value = GlobalConfig.getEqualBlacklistStringValue(name, DataModelType.GLOBAL_SYSTEM_PROPERTY_HIDE);
                if (value != null) {
                    param.setResult(Boolean.parseBoolean(value));
                    LogUtil.w(Const.JAVA_MONITOR_STATE,  "Modify SystemProperties name: %s, value: %s", name, value);
                }
            }
        });
    }
}
