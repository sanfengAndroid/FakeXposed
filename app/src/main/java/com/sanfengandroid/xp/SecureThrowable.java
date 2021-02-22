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

package com.sanfengandroid.xp;

import android.text.TextUtils;

import com.sanfengandroid.common.util.LogUtil;
import com.sanfengandroid.xp.hooks.XposedFilter;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.util.Random;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

/**
 * @author sanfengAndroid
 * @date 2020/10/30
 */
public class SecureThrowable {
    private static final String TAG = XposedFilter.TAG;
    private static final Random ran = new Random();
    private static final Field stackTraceField;
    private static int stack = 0;
    private static StackTraceElement keyElement;

    static {
        Field stackTraceField1;
        try {
            stackTraceField1 = Throwable.class.getDeclaredField("stackTrace");
            stackTraceField1.setAccessible(true);
        } catch (NoSuchFieldException e) {
            LogUtil.e(TAG, "no find field", e);
            stackTraceField1 = null;
        }
        stackTraceField = stackTraceField1;
    }

    public synchronized static void initStack() {
        if (stack > 0) {
            return;
        }
        Throwable throwable = testStack2();
        XposedHelpers.findAndHookMethod(SecureThrowable.class, "testStack2", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                param.setResult(new Throwable("test stack"));
            }
        });
        Throwable throwable2 = testStack2();
        stack = throwable2.getStackTrace().length - throwable.getStackTrace().length;
        keyElement = throwable2.getStackTrace()[stack];
    }

    private static Throwable testStack2() {
        return new Throwable("sdd");
    }

    /**
     * 这里使用反射修改,避免Hook getStackTrace方法的情况
     */
    public static Throwable secureThrowable(Throwable e, Member method) {
        if (e == null || keyElement == null) {
            return e;
        }
        try {
            // 这里要主动调用一下获取堆栈,否者可能不会初始化堆栈
            StackTraceElement[] elements = e.getStackTrace();
            if (elements.length == 0) {
                return e;
            }
            int pos = -1;
            for (int i = 0; i < elements.length; i++) {
                if (TextUtils.equals(keyElement.getClassName(), elements[i].getClassName()) &&
                        TextUtils.equals(keyElement.getMethodName(), elements[i].getMethodName())) {
                    pos = i;
                    break;
                }
            }
            if (pos == -1) {
                return e;
            }
            // Hook回调后会少一层栈帧,因此需要恢复它
            StackTraceElement[] newElements = pos == 0 ? elements : new StackTraceElement[elements.length - pos];
            newElements[0] = new StackTraceElement(method.getDeclaringClass().getName(), method.getName(),
                    method.getDeclaringClass().getSimpleName() + ".java", ran.nextInt(3874));
            System.arraycopy(elements, pos + 1, newElements, 1, newElements.length - 1);
            e.setStackTrace(newElements);
        } catch (Throwable t) {
            LogUtil.e(TAG, "modify error", t);
        }
        return e;
    }

    /**
     * 这种方式只能舍弃修复最后一个堆栈元素,因为我们没有办法拿到最后一个原始的元素
     * 但是只有极少数情况下才需要修复
     */
    public static StackTraceElement[] secureStackElement(StackTraceElement[] elements) {
        if (elements == null || elements.length == 0) {
            return elements;
        }
        int pos = -1;
        for (int i = 0; i < elements.length; i++) {
            if (TextUtils.equals(keyElement.getClassName(), elements[i].getClassName()) &&
                    TextUtils.equals(keyElement.getMethodName(), elements[i].getMethodName())) {
                pos = i;
                break;
            }
        }
        if (pos == -1) {
            return elements;
        }
        StackTraceElement[] newElements = new StackTraceElement[elements.length - ++pos];
        System.arraycopy(elements, pos, newElements, 0, newElements.length);
        return newElements;
    }
}
