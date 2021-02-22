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

package com.sanfengandroid.common.util;

import android.app.ActivityManager;
import android.app.ActivityThread;
import android.content.Context;
import android.os.Process;
import android.text.TextUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * @author sanfengAndroid
 * @date 2020/10/31
 */
public class Util {

    public static void removeFinalModifier(final Field field) {
        removeFinalModifier(field, true);
    }

    /**
     * Removes the final modifier from a {@link Field}.
     *
     * @param field       to remove the final modifier
     * @param forceAccess whether to break scope restrictions using the
     *                    {@link java.lang.reflect.AccessibleObject#setAccessible(boolean)} method. {@code false} will only
     *                    match {@code public} fields.
     * @throws IllegalArgumentException if the field is {@code null}
     * @since 3.3
     */
    public static void removeFinalModifier(final Field field, final boolean forceAccess) {
        if (field == null) {
            return;
        }
        String[] names = {"accessFlags", "modifiers"};
        for (String name : names) {
            try {
                if (Modifier.isFinal(field.getModifiers())) {
                    // Do all JREs implement Field with a private ivar called "modifiers"?
                    final Field modifiersField = Field.class.getDeclaredField(name);
                    final boolean doForceAccess = forceAccess && !modifiersField.isAccessible();
                    if (doForceAccess) {
                        modifiersField.setAccessible(true);
                    }
                    try {
                        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
                    } finally {
                        if (doForceAccess) {
                            modifiersField.setAccessible(false);
                        }
                    }
                    return;
                }
            } catch (final NoSuchFieldException ignored) {
                // The field class contains always a modifiers field
            } catch (final IllegalAccessException ignored) {
                // The modifiers field is made accessible
            }
        }
    }

    public static String getProcessName(Context context) {
        String process = ActivityThread.currentProcessName();
        if (!TextUtils.isEmpty(process)) {
            return process;
        }
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        int pid = Process.myPid();
        for (ActivityManager.RunningAppProcessInfo processInfo : am.getRunningAppProcesses()) {
            if (processInfo.pid == pid) {
                return processInfo.processName;
            }
        }
        return "";
    }

    public static String getMemberFullName(Member member) {
        if (member == null) {
            return "";
        }
        Class<?>[] types;
        if (member instanceof Method) {
            types = ((Method) member).getParameterTypes();
        } else {
            types = ((Constructor) member).getParameterTypes();
        }
        return member.getDeclaringClass().getName() + '#' + member.getName() + getParametersString(types);
    }

    private static String getParametersString(Class<?>[] clazzes) {
        StringBuilder sb = new StringBuilder("(");
        boolean first = true;
        for (Class<?> clazz : clazzes) {
            if (first) {
                first = false;
            } else {
                sb.append(",");
            }

            if (clazz != null) {
                sb.append(clazz.getCanonicalName());
            } else {
                sb.append("null");
            }
        }
        sb.append(")");
        return sb.toString();
    }


}
