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

import com.sanfengandroid.common.Const;
import com.sanfengandroid.common.bean.EnvBean;
import com.sanfengandroid.common.util.LogUtil;

/**
 * native回调java
 */
public class NativeCall {
    private static final String TAG = NativeCall.class.getSimpleName();

    public static String nativeReplaceEnv(String name, String value) {
        EnvBean bean = GlobalConfig.getEnvBlacklistValue(name);
        if (bean != null) {
            String replace = bean.replace(value);
            LogUtil.w(Const.JAVA_MONITOR_STATE,  "native get env name: %s, orig value: %s, replace: %s", name, value, replace);
        }
        return null;
    }
}
