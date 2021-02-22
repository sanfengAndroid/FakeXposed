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

import android.content.ContentResolver;
import android.os.Build;
import android.os.Debug;
import android.provider.Settings;

import com.sanfengandroid.common.Const;
import com.sanfengandroid.common.model.base.DataModelType;
import com.sanfengandroid.common.reflection.ReflectUtil;
import com.sanfengandroid.common.util.LogUtil;
import com.sanfengandroid.fakeinterface.GlobalConfig;

import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

import static com.sanfengandroid.xp.hooks.XposedFilter.TAG;

/**
 * @author sanfengAndroid
 * @date 2020/11/01
 */
public class HookDebugCheck implements IHook {
    @Override
    public void hook(ClassLoader loader) throws Exception {
        resetDebug();
        initGlobal();
    }

    private void resetDebug() {
        XposedHelpers.findAndHookMethod(Debug.class, "isDebuggerConnected", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        param.setResult(false);
                    }
                });
        if (!"release-keys".equals(Build.TAGS)) {
            LogUtil.v(TAG, "Original build tags: " + Build.TAGS);
            ReflectUtil.setFieldNoException(Build.class, null, "TAGS", "release-keys");
            LogUtil.v(TAG, "New build tags: " + Build.TAGS);
        }
    }

    private void initGlobal() throws Exception {
        Method method = Settings.Global.class.getMethod("getString", ContentResolver.class, String.class);
        GlobalConfig.addHookMethodModifierFilter(method);
        XposedBridge.hookMethod(method, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                String setting = (String) param.args[1];
                String value = GlobalConfig.getEqualBlacklistStringValue(setting, DataModelType.GLOBAL_HIDE);
                if (value != null) {
                    param.setResult(value);
                    LogUtil.w(Const.JAVA_MONITOR_STATE, "Modify Settings.Global name: %s, value: %s", setting, value);
                }
            }
        });
    }
}
