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

import com.sanfengandroid.fakeinterface.GlobalConfig;
import com.sanfengandroid.common.model.base.DataModelType;
import com.sanfengandroid.common.util.LogUtil;
import com.sanfengandroid.xp.SecureThrowable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

public class HookThrowableStackElement implements IHook {
    private static final String TAG = HookThrowableStackElement.class.getSimpleName();

    @Override
    public void hook(ClassLoader loader) throws Throwable {
        // 21 - 23 getInternalStackTrace
        // 24 - 29 getOurStackTrace
//        if (true){
//            return;
//        }
//        String name = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ? "getOurStackTrace" : "getInternalStackTrace";
        String name = "getStackTrace";
        Method method = Throwable.class.getDeclaredMethod(name);
        GlobalConfig.addHookMethodModifierFilter(method);
        XposedBridge.hookMethod(method, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                StackTraceElement[] elements = (StackTraceElement[]) param.getResult();
                if (elements.length == 0) {
                    return;
                }
                List<StackTraceElement> list = new ArrayList<>();
                boolean change = false;
                for (StackTraceElement element : elements) {
                    if (GlobalConfig.stringContainBlackList(element.getClassName(), DataModelType.STACK_ELEMENT_HIDE)) {
                        change = true;
                        LogUtil.d(TAG, "hide danger class: %s", element.getClassName());
                    } else {
                        list.add(element);
                    }
                }
                if (change) {
                    elements = SecureThrowable.secureStackElement(list.toArray(new StackTraceElement[0]));
                } else {
                    elements = SecureThrowable.secureStackElement(elements);
                }
                param.setResult(elements);
            }
        });
    }
}
