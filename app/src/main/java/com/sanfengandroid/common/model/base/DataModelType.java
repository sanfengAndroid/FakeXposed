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

package com.sanfengandroid.common.model.base;

import com.sanfengandroid.common.model.ComponentKeywordModel;
import com.sanfengandroid.common.model.FileAccessModel;
import com.sanfengandroid.common.model.FileBlacklistModel;
import com.sanfengandroid.common.model.FileRedirectModel;
import com.sanfengandroid.common.model.GlobalModel;
import com.sanfengandroid.common.model.GlobalSystemPropertyModel;
import com.sanfengandroid.common.model.LoadClassModel;
import com.sanfengandroid.common.model.MapsRuleModel;
import com.sanfengandroid.common.model.PackageEditModel;
import com.sanfengandroid.common.model.PackageModel;
import com.sanfengandroid.common.model.RuntimeExecModel;
import com.sanfengandroid.common.model.StackElementModel;
import com.sanfengandroid.common.model.StringModel;
import com.sanfengandroid.common.model.SymbolBlacklistModel;
import com.sanfengandroid.common.model.SystemEnvironmentModel;
import com.sanfengandroid.common.model.SystemPropertyModel;
import com.sanfengandroid.datafilter.R;

/**
 * 数据模型类型
 *
 * @author sanfengAndroid
 */
public enum DataModelType {
    /**
     * 啥也没有
     */
    NOTHING(R.string.data_model_name_nothing, R.string.data_model_name_nothing, "no", StringModel.class, StringModel.class),
    /**
     * 类加载过滤
     */
    LOAD_CLASS_HIDE(R.string.data_model_name_load_class, R.string.tip_load_class, "lcf", LoadClassModel.class, LoadClassModel.class),
    /**
     * 堆栈类过滤
     */
    STACK_ELEMENT_HIDE(R.string.data_model_name_statck_element, R.string.tip_stack_element, "sef", StackElementModel.class, StackElementModel.class),
    /**
     * App可见包名过滤
     */
    PACKAGE_HIDE(R.string.data_model_name_application_package, R.string.tip_app_package, "pf", PackageModel.class, PackageEditModel.class),
    /**
     * System属性过滤
     */
    SYSTEM_PROPERTY_HIDE(R.string.data_model_name_system_prop, R.string.tip_system_property, "spf", SystemPropertyModel.class, SystemPropertyModel.SystemPropertyEditModel.class),
    /**
     * System环境变量过滤
     */
    SYSTEM_ENV_HIDE(R.string.data_model_name_system_env, R.string.tip_system_environment, "senf", SystemEnvironmentModel.class, SystemEnvironmentModel.SystemEnvironmentEditModel.class),
    /**
     * 全局SystemProperties过滤
     */
    GLOBAL_SYSTEM_PROPERTY_HIDE(R.string.data_model_name_global_system_prop, R.string.tip_global_system_properties, "gspf", GlobalSystemPropertyModel.class, GlobalSystemPropertyModel.GlobalSystemPropertyEditModel.class),
    /**
     * 组件关键字过滤
     */
    COMPONENT_KEY_HIDE(R.string.data_model_name_component_key, R.string.tip_component_keyword, "ckf", ComponentKeywordModel.class, ComponentKeywordModel.class),
    /**
     * Global
     */
    GLOBAL_HIDE(R.string.data_model_name_global_get_int, R.string.tip_global_getint, "gif", GlobalModel.class, GlobalModel.GlobalIntEditModel.class),
    /**
     * Runtime.exec执行过滤
     */
    RUNTIME_EXEC_HIDE(R.string.data_model_name_runtime_exec, R.string.tip_runtime_exec, "rcf", RuntimeExecModel.class, RuntimeExecModel.RuntimeExecEditModel.class),

    FILE_REDIRECT_HIDE(R.string.data_model_name_file_redirect, R.string.tip_file_redirect, "nfr", FileRedirectModel.class, FileRedirectModel.FileRedirectEditModel.class),
    /**
     * native层文件屏蔽,需要开启native hook才生效
     */
    FILE_HIDE(R.string.data_model_name_file_blacklist, R.string.tip_file_blacklist, "nfb", FileBlacklistModel.class, FileBlacklistModel.class),

    FILE_ACCESS_HIDE(R.string.data_model_name_file_access, R.string.tip_file_access, "nfa", FileAccessModel.class, FileAccessModel.FileAccessEditModel.class),
    /**
     * native dlsym符号名过滤
     */
    SYMBOL_HIDE(R.string.data_model_name_symbol_blacklist, R.string.tip_symbol_blacklist, "nssb", SymbolBlacklistModel.class, SymbolBlacklistModel.class),
    /**
     * maps文件匹配规则删除/修改行
     */
    MAPS_HIDE(R.string.data_model_name_maps_blacklist, R.string.tip_maps_blacklist, "nmb", MapsRuleModel.class, MapsRuleModel.MapsRuleEditModel.class);


    public final int nameId;
    public final int tipId;
    public final String spKey;
    public final Class<? extends ShowDataModel> valueType;
    public final Class<? extends EditDataModel> addType;

    DataModelType(int nameId, int tipId, String key, Class<? extends ShowDataModel> setClass, Class<? extends EditDataModel> addClass) {
        this.nameId = nameId;
        this.tipId = tipId;
        this.spKey = key;
        this.valueType = setClass;
        this.addType = addClass;
    }

    public static DataModelType keyToDataModelType(String key) {
        for (DataModelType type : DataModelType.values()) {
            if (type.spKey.equals(key)) {
                return type;
            }
        }
        return NOTHING;
    }
}
