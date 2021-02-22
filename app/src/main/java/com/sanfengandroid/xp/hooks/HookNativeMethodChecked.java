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

import android.os.Build;

import com.sanfengandroid.common.Const;
import com.sanfengandroid.common.util.LogUtil;
import com.sanfengandroid.common.util.Util;
import com.sanfengandroid.fakeinterface.GlobalConfig;

import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

public class HookNativeMethodChecked implements IHook {
    private static final String TAG = HookNativeMethodChecked.class.getSimpleName();

    @Override
    public void hook(ClassLoader loader) throws Throwable {
        String name = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? "getModifiersInternal" : "getModifiers";
        Method method = Method.class.getSuperclass().getDeclaredMethod(name);
        GlobalConfig.addHookMethodModifierFilter(method);
        XC_MethodHook callback = new XC_MethodHook() {

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (!GlobalConfig.isHookMember((Member) param.thisObject)) {
                    return;
                }
                try {
                    int modify = (int) param.getResult();
                    modify = modify & ~Modifier.NATIVE;
                    LogUtil.w(Const.JAVA_MONITOR_STATE,  "Fix Hook Native flag: %s", Util.getMemberFullName((Member) param.thisObject));
                    param.setResult(modify);
                } catch (Throwable e) {
                    LogUtil.e(TAG, "replace native modify error", e);
                }
            }
        };
        XposedBridge.hookMethod(method, callback);
        LogUtil.v(TAG, "Hook Method Native check success");
    }
}
