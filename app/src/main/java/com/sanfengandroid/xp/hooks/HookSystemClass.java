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
import android.os.Build;
import android.text.TextUtils;
import android.util.Pair;

import com.sanfengandroid.common.bean.EnvBean;
import com.sanfengandroid.fakeinterface.GlobalConfig;
import com.sanfengandroid.common.model.base.DataModelType;
import com.sanfengandroid.common.reflection.ReflectUtil;
import com.sanfengandroid.common.util.LogUtil;
import com.sanfengandroid.common.util.Util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Observer;
import java.util.Properties;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

/**
 * 这里应该监听有属性变化然后再次调整
 */
public class HookSystemClass implements IHook {
    private static final String TAG = HookSystemClass.class.getSimpleName();
    private final Observer envObserver = (o, arg) -> {
        if (arg != null && Build.VERSION.SDK_INT > Build.VERSION_CODES.N) {
            replaceEnvs();
        }
    };
    private Properties orig;
    private final Properties proxy = new Properties() {
        @Override
        public synchronized Object get(Object key) {
            // 只用拦截所有字符串属性
            return key instanceof String ? super.get(key) : orig.get(key);
        }
    };
    private final Observer propObserver = (o, arg) -> {
        if (arg instanceof Pair) {
            Pair<String, String> pair = (Pair<String, String>) arg;
            String value = proxy.getProperty(pair.first);
            if (value != null) {
                proxy.setProperty(pair.first, pair.second);
            }
        } else if (arg instanceof Map) {
            Map<String, String> map = (Map<String, String>) arg;
            for (Map.Entry<String, String> entry : map.entrySet()) {
                String value = proxy.getProperty(entry.getKey());
                if (value != null) {
                    if (TextUtils.isEmpty(entry.getValue())) {
                        proxy.remove(entry.getKey());
                    } else {
                        proxy.setProperty(entry.getKey(), entry.getValue());
                    }
                }
            }
        }
    };

    private void copyProperties(Properties orig, Properties target) {
        for (String s : orig.stringPropertyNames()) {
            String value = orig.getProperty(s);
            if (value != null) {
                target.put(s, value);
            }
        }
    }

    @Override
    public void hook(ClassLoader loader) throws Throwable {
        orig = System.getProperties();
        copyProperties(orig, proxy);
        ReflectUtil.setFieldStatic(System.class, "props", proxy);
        GlobalConfig.addObserver(propObserver, DataModelType.SYSTEM_PROPERTY_HIDE);

        Method method = System.class.getDeclaredMethod("setProperties", Properties.class);
        GlobalConfig.addHookMethodModifierFilter(method);
        XposedBridge.hookMethod(method, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                orig = System.getProperties();
                proxy.clear();
                copyProperties(orig, proxy);
                ReflectUtil.setFieldStatic(System.class, "props", proxy);
                propObserver.update(null, GlobalConfig.getMap(DataModelType.SYSTEM_PROPERTY_HIDE));
            }
        });

        shieldSystemEnv();
        GlobalConfig.addObserver(envObserver, DataModelType.SYSTEM_ENV_HIDE);
    }

    private void shieldSystemEnv() throws NoSuchMethodException {
        // native拦截然后回调java处理 getenv(String)
        Method method;
        // 21 ~ 23 每次调用生成一个Map
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N) {
            replaceEnvs();
        } else {
            method = System.class.getDeclaredMethod("getenv");
            GlobalConfig.addHookMethodModifierFilter(method);
            XposedBridge.hookMethod(method, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    // SystemEnvironment
                    Map<String, String> envs = (Map<String, String>) param.getResult();
                    Map<String, String> map = replaceEnvs(envs);
                    if (map == envs) {
                        return;
                    }
                    // 反射创建 SystemEnvironment
                    try {
                        Constructor<?> constructor = envs.getClass().getConstructor(Map.class);
                        constructor.setAccessible(true);
                        param.setResult(constructor.newInstance(map));
                    } catch (Throwable e) {
                        LogUtil.e(TAG, "Reflect create %s failed.", envs.getClass(), e);
                        param.setResult(map);
                    }
                }
            });
        }
    }

    @SuppressLint("BlockedPrivateApi")
    public void replaceEnvs() {
        // 直接修改实例
        // Map<Variable,Value>
        Map<String, String> envs = System.getenv();
        Map<String, String> newMap = replaceEnvs(envs);
        // ProcessEnvironment.theUnmodifiableEnvironment
        // Map<String, String> -> StringEnvironment(Map<Variable,Value> m)
        if (envs != newMap) {
            try {
                // Map<String, String> -> Map<Variable,Value> m
                Class<?> mVariable = Class.forName("java.lang.ProcessEnvironment$Variable");
                Method valueOfVariable = mVariable.getDeclaredMethod("valueOf", String.class);
                valueOfVariable.setAccessible(true);
                Class<?> mValue = Class.forName("java.lang.ProcessEnvironment$Value");
                Method valueOfValue = mValue.getDeclaredMethod("valueOf", String.class);
                valueOfValue.setAccessible(true);

                Map map = new HashMap();
                for (Map.Entry<String, String> entry : newMap.entrySet()) {
                    map.put(valueOfVariable.invoke(null, entry.getKey()), valueOfValue.invoke(null,
                            entry.getValue()));
                }
                // Map<Variable,Value> -> StringEnvironment
                Class<?> mStringEnvironmentClass = Class.forName("java.lang.ProcessEnvironment$StringEnvironment");
                Constructor<?> constructor = mStringEnvironmentClass.getConstructor(Map.class);
                constructor.setAccessible(true);
                map = (Map) constructor.newInstance(map);
                map = Collections.unmodifiableMap(map);
                // ProcessEnvironment.theUnmodifiableEnvironment
                Class<?> processEnvironment = Class.forName("java.lang.ProcessEnvironment");
                Field field = processEnvironment.getDeclaredField("theUnmodifiableEnvironment");
                Util.removeFinalModifier(field);
                field.setAccessible(true);
                field.set(null, map);
                LogUtil.v(TAG, "replace new environment variable");
            } catch (Throwable e) {
                LogUtil.e(TAG, "replace environment variable failed.", e);
            }
        }
    }

    private Map<String, String> replaceEnvs(Map<String, String> input) {
        if (input.isEmpty()) {
            return input;
        }
        Map<String, String> map = new HashMap<>();
        Collection<EnvBean> list = GlobalConfig.getBlackListValue(DataModelType.SYSTEM_ENV_HIDE, EnvBean.class);

        boolean hasChange = false;
        for (Map.Entry<String, String> entry : input.entrySet()) {
            boolean change = false;
            for (EnvBean bean : list) {
                if (bean.contain(entry.getKey())) {
                    map.put(entry.getKey(), bean.replace(entry.getValue()));
                    change = true;
                    hasChange = true;
                    break;
                }
            }
            if (!change) {
                map.put(entry.getKey(), entry.getValue());
            }
        }
        return hasChange ? map : input;
    }
}
