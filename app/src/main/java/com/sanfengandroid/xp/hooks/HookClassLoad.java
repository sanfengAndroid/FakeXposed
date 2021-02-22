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

import android.text.TextUtils;

import com.sanfengandroid.common.Const;
import com.sanfengandroid.common.model.base.DataModelType;
import com.sanfengandroid.common.util.LogUtil;
import com.sanfengandroid.fakeinterface.GlobalConfig;
import com.sanfengandroid.fakeinterface.NativeHook;
import com.sanfengandroid.fakexposed.BuildConfig;
import com.sanfengandroid.xp.SecureThrowable;

import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class HookClassLoad implements IHook {
    private static final String TAG = HookClassLoad.class.getSimpleName();

    private static boolean isChild(ClassLoader parent, ClassLoader child) {
        if (child == parent) {
            return true;
        }
        ClassLoader childParent = child == null ? null : child.getParent();
        if (childParent != null) {
            return isChild(parent, childParent);
        }
        return false;
    }

    @Override
    public void hook(ClassLoader loader) throws Throwable {
        LogUtil.v(TAG, "hide class load");
        // EdXposed Hook只有部分生效，不知道是不是框架做了处理
        XC_MethodHook filterClass = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                String name = (String) param.args[0];
                ClassLoader found = (ClassLoader) (param.args.length == 2 ? param.thisObject : param.args[2]);
                if (isChild(loader, found) && GlobalConfig.stringContainBlackList(name, DataModelType.LOAD_CLASS_HIDE)) {
                    LogUtil.w(Const.JAVA_MONITOR_STATE, "Looking for blocked classes: %s", name);
                    param.setResult(null);
                    param.setThrowable(SecureThrowable.secureThrowable(new ClassNotFoundException(name), param.method));
                }
            }
        };
        Method method = ClassLoader.class.getDeclaredMethod("loadClass", String.class, boolean.class);
        GlobalConfig.addHookMethodModifierFilter(method);
        XposedBridge.hookMethod(method, filterClass);
        method = Class.class.getDeclaredMethod("forName", String.class, boolean.class, ClassLoader.class);
        XposedBridge.hookMethod(method, filterClass);
        GlobalConfig.addHookMethodModifierFilter(method);
        XposedHelpers.findAndHookMethod(ClassLoader.class, "defineClass", String.class, byte[].class, int.class, int.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                String name = (String) param.args[0];
                if (TextUtils.equals(name, BuildConfig.APPLICATION_ID)) {
                    LogUtil.d(TAG, "define class get self class");
                    param.setResult(NativeHook.class);
                }
            }
        });
        LogUtil.d(TAG, "hook class loader success");
    }
}
