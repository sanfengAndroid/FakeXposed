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

/**
 * 插件通过ContentProvider调用
 *
 * @author sanfengAndroid
 */

public enum RemoteCall {

    /**
     * 远程调用是否激活
     */
    IS_ACTIVE("activation"),

    GET_APP_CONFIG("get_config"),

    WRITE_APP_CONFIG("write_config"),

    SET_APP_ENABLE("set_app_enable"),

    SET_APP_STRING_CONFIG("set_app_string"),

    SYNC_CONFIGURATION("sync_config");

    public static final String KEY_CALL_RESULT = "ret";
    /**
     * 通用布尔参数名,根据不同方法解释不同
     */
    public static final String KEY_COMMON_BOOLEAN = "common_boolean";
    /**
     * 通用字符串参数,不同方法不同解析
     */
    public static final String KEY_COMMON_STRING = "common_string";
    public static final String KEY_ERROR_DESCRIPTION = "error_string";
    public static final String KEY_HOOK_APP_ENABLE = "app_enable";
    /**
     * 该方法未定义
     */
    public static final int RESULT_VALUE_UNDEFINED = -1;
    /**
     * 调用方法出错
     */
    public static final int RESULT_VALUE_CALL_ERROR = -2;
    public static final int RESULT_VALUE_PARAM_NULL_ERROR = -3;
    public static final int RESULT_VALUE_PARAM_ERROR = -4;
    /**
     * 内部执行出错
     */
    public static final int RESULT_VALUE_EXEC_ERROR = -5;
    /**
     * 调用成功
     */
    public static final int RESULT_VALUE_CALL_SUCCESS = 1;
    /**
     * 调用方传入的包名
     */
    public static final String KEY_APP_PKG = "pkg";
    public static final String KEY_DATA_TYPE = "data_type";
    public static final String KEY_SYNC_FILE_NAME = "sync_name";
    public static final String KEY_SYNC_FILE_DATA = "sync_data";
    /**
     * 同步数据太大则需要分段
     */
    public static final String KEY_SYNC_BLOCK_INDEX = "sync_block";
    public static final String METHOD_HOOK_FLAG = "sanfengandroid";
    public final String method;

    RemoteCall(String method) {
        this.method = method;
    }

    public static RemoteCall nameToRemoteCall(String name) {
        for (RemoteCall call : RemoteCall.values()) {
            if (call.method.equals(name)) {
                return call;
            }
        }
        return null;
    }
}
