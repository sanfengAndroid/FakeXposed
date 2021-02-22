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

package com.sanfengandroid.common.reflection;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * @author sanfengAndroid
 * @date 2020/10/30
 */
public class ReflectUtil {

    public static Object getField(String clazzName, Object target, String name) throws Exception {
        return getField(Class.forName(clazzName), target, name);
    }

    public static Object getField(Class<?> clazz, Object target, String name) throws Exception {
        Field field = clazz.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    public static Object getFieldStatic(Class<?> clazz, String name) throws Exception {
        return getField(clazz, null, name);
    }

    public static Object getFieldStatic(String clazzName, String name) throws Exception {
        return getField(Class.forName(clazzName), null, name);
    }

    public static Object getFieldInstance(String clazzName, Object target, String name) throws Exception {
        return getField(Class.forName(clazzName), target, name);
    }

    public static Object getFieldInstance(Object target, String name) throws Exception {
        return getField(target.getClass(), target, name);
    }

    public static Object getFieldInstance(Class<?> clazz, Object target, String name) throws Exception {
        return getField(clazz, target, name);
    }

    public static Object getFieldNoException(String clazzName, Object target, String name) {
        try {
            return getFieldNoException(Class.forName(clazzName), target, name);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Object getFieldNoException(Class<?> clazz, Object target, String name) {
        try {
            return ReflectUtil.getField(clazz, target, name);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void setFieldInstance(Object target, String name, Object value) throws Exception {
        setField(target.getClass(), target, name, value);
    }

    public static void setFieldStatic(String clazzName, String name, Object value) throws Exception {
        setField(clazzName, null, name, value);
    }

    public static void setField(String clazzName, Object target, String name, Object value) throws Exception {
        setField(Class.forName(clazzName), target, name, value);
    }

    public static void setFieldStatic(Class<?> clazz, String name, Object value) throws Exception {
        setField(clazz, null, name, value);
    }

    public static void setField(Class<?> clazz, Object target, String name, Object value) throws Exception {
        Field field = clazz.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    public static void setFieldInstanceNoException(Object target, String name, Object value) {
        setFieldNoException(target.getClass(), target, name, value);
    }

    public static void setFieldStaticNoException(String clazzName, String name, Object value) {
        setFieldNoException(clazzName, null, name, value);
    }

    public static void setFieldNoException(String clazzName, Object target, String name, Object value) {
        try {
            setFieldNoException(Class.forName(clazzName), target, name, value);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void setFieldStaticNoException(Class<?> clazz, String name, Object value) {
        setFieldNoException(clazz, null, name, value);
    }

    public static void setFieldNoException(Class<?> clazz, Object target, String name, Object value) {
        try {
            ReflectUtil.setField(clazz, target, name, value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Object invoke(String clazzName, Object target, String name, Object... args)
            throws Exception {
        return invoke(Class.forName(clazzName), target, name, args);
    }

    @SuppressWarnings("unchecked")
    public static Object invoke(Class<?> clazz, Object target, String name, Object... args)
            throws Exception {
        Class<?>[] parameterTypes = null;
        if (args != null) {
            parameterTypes = new Class[args.length];
            for (int i = 0; i < args.length; i++) {
                parameterTypes[i] = args[i].getClass();
            }
        }

        Method method = clazz.getDeclaredMethod(name, parameterTypes);
        method.setAccessible(true);
        return method.invoke(target, args);
    }

    public static Object invoke(String clazzName, Object target, String name, Class<?>[] parameterTypes, Object... args)
            throws Exception {
        return invoke(Class.forName(clazzName), target, name, parameterTypes, args);
    }

    public static Object invoke(Class<?> clazz, Object target, String name, Class<?>[] parameterTypes, Object... args)
            throws Exception {
        Method method = clazz.getDeclaredMethod(name, parameterTypes);
        method.setAccessible(true);
        return method.invoke(target, args);
    }

    public static Object invokeNoException(String clazzName, Object target, String name, Class<?>[] parameterTypes,
                                           Object... args) {
        try {
            return invokeNoException(Class.forName(clazzName), target, name, parameterTypes, args);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Object invokeNoException(Class<?> clazz, Object target, String name, Class<?>[] parameterTypes,
                                           Object... args) {
        try {
            return invoke(clazz, target, name, parameterTypes, args);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}