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

package com.sanfengandroid.fakeinterface;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Pair;

import com.sanfengandroid.common.bean.EnvBean;
import com.sanfengandroid.common.bean.ExecBean;
import com.sanfengandroid.common.model.PackageModel;
import com.sanfengandroid.common.model.RuntimeExecModel;
import com.sanfengandroid.common.model.StringModel;
import com.sanfengandroid.common.model.SystemEnvironmentModel;
import com.sanfengandroid.common.model.base.BaseKeyValueModel;
import com.sanfengandroid.common.model.base.DataModelType;
import com.sanfengandroid.common.model.base.ShowDataModel;
import com.sanfengandroid.common.util.LogUtil;
import com.sanfengandroid.common.util.Util;
import com.sanfengandroid.xp.ObservableMap;
import com.sanfengandroid.xp.DefaultLists;

import org.json.JSONObject;

import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Observer;
import java.util.Set;

/**
 * @author sanfengAndroid
 * @date 2020/10/30
 */
public class GlobalConfig {
    private static final String TAG = GlobalConfig.class.getSimpleName();
    private static final Map<String, ?>[] maps;
    private static final Object EXIST = new Object();
    private static final Map<String, String> classBlacklist = new HashMap<>();
    private static final Map<String, String> stackClassBlacklist = new HashMap<>();
    private static final Map<String, String> packageBlacklist = new HashMap<>();
    private static final Map<Integer, Object> hookMethodModifierFilter = new HashMap<>();
    private static final ObservableMap<String, String> propBlacklist = new ObservableMap<>();
    private static final ObservableMap<String, EnvBean> envBlacklist = new ObservableMap<>();
    private static final Map<String, String> globalPropertyBlacklist = new HashMap<>();
    private static final Map<String, String> componentKeyBlacklist = new HashMap<>();
    private static final Map<String, String> globalSettingsBlacklist = new HashMap<>();
    private static final Map<String, ExecBean> runtimeBlackList = new HashMap<>();
    private static final Map<String, String> fileBlacklist = new HashMap<>();
    private static final Map<String, String> symbolBlacklist = new HashMap<>();
    private static final Map<String, String> mapsBlacklist = new HashMap<>();
    private static final Map<String, String> fileRedirectList = new HashMap<>();
    private static final Map<String, String> fileAccessList = new HashMap<>();

    static {
        // 反射类过滤
        for (String s : DefaultLists.DEFAULT_CLASS_LIST) {
            classBlacklist.put(s, s);
        }
        // 函数堆栈过滤
        for (String s : DefaultLists.DEFAULT_STACK_LIST) {
            stackClassBlacklist.put(s, s);
        }
        // 安装包过滤
        packageBlacklist.put(PackageModel.XPOSED_PACKAGE_MASK, PackageModel.XPOSED_PACKAGE_MASK);
        for (String pkg : DefaultLists.DEFAULT_APPS_LIST) {
            packageBlacklist.put(pkg, pkg);
        }

        // 系统属性过滤
        for (Pair<String, String> pair : DefaultLists.DEFAULT_SYSTEM_PROP_LIST) {
            propBlacklist.put(pair.first, pair.second);
        }
        // 系统环境变量过滤
        for (EnvBean env : DefaultLists.DEFAULT_SYSTEM_ENV_LIST) {
            envBlacklist.put(env.name, env);
        }
        // 系统属性SystemProperties过滤
        for (Pair<String, String> pair : DefaultLists.DEFAULT_GLOBAL_PROPERTY_LIST) {
            globalPropertyBlacklist.put(pair.first, pair.second);
        }

        // 关键字屏蔽
        for (String name : DefaultLists.DEFAULT_KEYWORD_LIST) {
            componentKeyBlacklist.put(name, name);
        }
        // 全局设置屏蔽
        for (Pair<String, String> pair : DefaultLists.DEFAULT_GLOBAL_VARIABLE_LIST) {
            globalSettingsBlacklist.put(pair.first, pair.second);
        }
//        runtimeBlackList.put(bean.oldCmd, bean);

        // C层数据配置
        for (String file : DefaultLists.DEFAULT_FILES_LIST) {
            fileBlacklist.put(file, file);
        }
        for (String s : DefaultLists.DEFAULT_SYMBOL_LIST) {
            symbolBlacklist.put(s, s);
        }

        for (Pair<String, String> pair : DefaultLists.DEFAULT_MAPS_RULE_LIST) {
            mapsBlacklist.put(pair.first, pair.second);
        }

        maps = new Map[DataModelType.values().length];
        maps[DataModelType.NOTHING.ordinal()] = new HashMap<>();
        maps[DataModelType.LOAD_CLASS_HIDE.ordinal()] = classBlacklist;
        maps[DataModelType.STACK_ELEMENT_HIDE.ordinal()] = stackClassBlacklist;
        maps[DataModelType.PACKAGE_HIDE.ordinal()] = packageBlacklist;
        maps[DataModelType.SYSTEM_PROPERTY_HIDE.ordinal()] = propBlacklist;
        maps[DataModelType.SYSTEM_ENV_HIDE.ordinal()] = envBlacklist;
        maps[DataModelType.GLOBAL_SYSTEM_PROPERTY_HIDE.ordinal()] = globalPropertyBlacklist;
        maps[DataModelType.COMPONENT_KEY_HIDE.ordinal()] = componentKeyBlacklist;
        maps[DataModelType.GLOBAL_HIDE.ordinal()] = globalSettingsBlacklist;
        maps[DataModelType.RUNTIME_EXEC_HIDE.ordinal()] = runtimeBlackList;
        maps[DataModelType.FILE_HIDE.ordinal()] = fileBlacklist;
        maps[DataModelType.FILE_REDIRECT_HIDE.ordinal()] = fileRedirectList;
        maps[DataModelType.FILE_ACCESS_HIDE.ordinal()] = fileAccessList;
        maps[DataModelType.SYMBOL_HIDE.ordinal()] = symbolBlacklist;
        maps[DataModelType.MAPS_HIDE.ordinal()] = mapsBlacklist;
    }

    private GlobalConfig() {
    }

    public static boolean stringContainsFromSet(String base, Collection<String> values) {
        if (!(base == null || values == null)) {
            for (String tempString : values) {
                if (base.matches(".*(\\W|^)" + tempString + "(\\W|$).*")) {
                    return true;
                }
            }
        }
        return false;
    }

    public static List<ShowDataModel> getShowConfig(DataModelType type) {
        List<ShowDataModel> data = new ArrayList<>();
        try {
            switch (type) {
                case SYSTEM_ENV_HIDE:
                    for (EnvBean bean : envBlacklist.values()) {
                        SystemEnvironmentModel model = new SystemEnvironmentModel();
                        model.setBean(bean);
                        data.add(model);
                    }
                    break;
                case RUNTIME_EXEC_HIDE:
                    for (ExecBean bean : runtimeBlackList.values()) {
                        RuntimeExecModel model = new RuntimeExecModel();
                        model.setBean(bean);
                        data.add(model);
                    }
                    break;
                default:
                    if (StringModel.class.isAssignableFrom(type.valueType)) {
                        for (String value : getMap(type).keySet()) {
                            StringModel model = (StringModel) type.valueType.newInstance();
                            model.setValue(value);
                            data.add(model);
                        }
                    } else if (BaseKeyValueModel.class.isAssignableFrom(type.valueType)) {
                        Map<String, String> map = getMap(type, String.class);
                        for (Map.Entry<String, String> entry : map.entrySet()) {
                            BaseKeyValueModel model = (BaseKeyValueModel) type.valueType.newInstance();
                            model.setKey(entry.getKey());
                            model.setValue(entry.getValue());
                            data.add(model);
                        }
                    }
                    break;
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        if (!data.isEmpty() && data.get(0) instanceof Comparable) {
            Collections.sort(data, null);
        }
        return data;
    }

    public static void addHookMethodModifierFilter(Member member) {
        hookMethodModifierFilter.put(Util.getMemberFullName(member).hashCode(), EXIST);
    }

    public static void addHookMethodModifierFilter(Member... arg) {
        for (Member member : arg) {
            addHookMethodModifierFilter(member);
        }
    }

    public static boolean stringContainBlackList(String name, DataModelType type) {
        Map<String, ?> map = maps[type.ordinal()];
        for (String s : map.keySet()) {
            if (name.contains(s)) {
                return true;
            }
        }
        return false;
    }

    public static Object getEqualBlacklistValue(String name, DataModelType type) {
        return maps[type.ordinal()].get(name);
    }

    @SuppressWarnings("unchecked")
    public static <T> T getEqualBlacklistValue(String name, DataModelType type, Class<T> clazz) {
        return (T) getEqualBlacklistValue(name, type);
    }

    public static String getEqualBlacklistStringValue(String name, DataModelType type) {
        return getEqualBlacklistValue(name, type, String.class);
    }

    public static Integer getEqualsBlacklistIntValue(String name, DataModelType type) {
        return getEqualBlacklistValue(name, type, Integer.class);
    }

    public static boolean stringEqualBlacklist(String name, DataModelType type) {
        return maps[type.ordinal()].get(name) != null;
    }

    public static EnvBean getEnvBlacklistValue(String name) {
        return envBlacklist.get(name);
    }

    public static boolean isEmptyBlacklist(DataModelType type) {
        return maps[type.ordinal()].isEmpty();
    }

    public static boolean isHookMember(Member member) {
        if (member != null) {
            return hookMethodModifierFilter.get(Util.getMemberFullName(member).hashCode()) == EXIST;
        }
        return false;
    }

    public static Set<String> getBlacklistKeys(DataModelType type) {
        return maps[type.ordinal()].keySet();
    }

    @SuppressWarnings("unchecked")
    public static <T> Collection<T> getBlackListValue(DataModelType type, Class<T> clazz) {
        Map<String, ?> map = maps[type.ordinal()];
        return (Collection<T>) map.values();
    }

    public static void addObserver(Observer observer, DataModelType type) {
        Map<String, ?> map = getMap(type);
        if (map instanceof ObservableMap) {
            ((ObservableMap) map).addObserver(observer);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> Map<String, T> getMap(DataModelType type, Class<T> clazz) {
        return (Map<String, T>) getMap(type);
    }

    public static Map<String, ?> getMap(DataModelType type) {
        return maps[type.ordinal()];
    }

    @SuppressWarnings("unchecked")
    public static void init(Map<String, Set<ShowDataModel>> datas) {
        for (Map.Entry<String, Set<ShowDataModel>> entry : datas.entrySet()) {
            DataModelType type = DataModelType.keyToDataModelType(entry.getKey());
            for (ShowDataModel model : entry.getValue()) {
                switch (type) {
                    case SYSTEM_ENV_HIDE:
                        SystemEnvironmentModel sysModel = (SystemEnvironmentModel) model;
                        envBlacklist.put(sysModel.getBean().name, sysModel.getBean());
                        break;
                    case RUNTIME_EXEC_HIDE:
                        RuntimeExecModel runModel = (RuntimeExecModel) model;
                        runtimeBlackList.put(runModel.getBean().oldCmd, runModel.getBean());
                        break;
                    case NOTHING:
                        break;
                    default:
                        if (model instanceof StringModel) {
                            StringModel data = (StringModel) model;
                            Map<String, String> map = (Map<String, String>) maps[type.ordinal()];
                            map.put(data.getValue(), data.getValue());
                        } else if (model instanceof BaseKeyValueModel) {
                            BaseKeyValueModel keyModel = (BaseKeyValueModel) model;
                            Map<String, String> keyMap = (Map<String, String>) maps[type.ordinal()];
                            keyMap.put(keyModel.getKey(), keyModel.getValue());
                        }
                        break;
                }
            }
        }
    }

    public static Map<String, Set<ShowDataModel>> transformBundle(Bundle bundle) {
        if (bundle == null) {
            return null;
        }
        Map<String, Set<ShowDataModel>> data = new HashMap<>();
        for (DataModelType type : DataModelType.values()) {
            String[] settings = bundle.getStringArray(type.spKey);
            if (settings == null) {
                continue;
            }
            Set<ShowDataModel> set = new HashSet<>();
            Class<?> clazz = type.valueType;
            for (String value : settings) {
                try {
                    if (TextUtils.isEmpty(value)) {
                        continue;
                    }
                    JSONObject json = new JSONObject(value);
                    ShowDataModel model = (ShowDataModel) clazz.newInstance();
                    model.unSerialization(json);
                    if (!model.isAvailable()) {
                        continue;
                    }
                    set.add(model);
                } catch (Exception e) {
                    LogUtil.e(TAG, "un-serialization object error", e);
                }
            }
            data.put(type.spKey, set);
        }
        return data;
    }

    public static void remoteInit(Bundle bundle) {
        init(transformBundle(bundle));
    }

    public static void removeThis(String pkg) {
        packageBlacklist.remove(pkg);
    }

}
