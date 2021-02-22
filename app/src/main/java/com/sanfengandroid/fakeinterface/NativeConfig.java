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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public enum NativeConfig {
    /*
     * Java层Runtime.exec调用execvp拦截,
     * 目前拦截它会导致子进程不返回
     * */
    JAVA_EXECVP("java_execvp"),
    RELINK_SYMBOL_FILTER("relink_symbol_filter");
    public static Map<String, Object> nativeConfigs = new HashMap<>();

    static {
        nativeConfigs.put(JAVA_EXECVP.name, NativeHookStatus.DISABLE);
    }

    public final String name;

    NativeConfig(String name) {
        this.name = name;
    }

    public static List<String> configToString() {
        List<String> list = new ArrayList<>();
        for (Map.Entry<String, Object> entry : nativeConfigs.entrySet()) {
            if (entry.getValue() instanceof NativeHookStatus) {
                list.add(entry.getKey() + "=" + ((NativeHookStatus) entry.getValue()).getOption());
            } else if (entry.getValue() instanceof String) {
                list.add(entry.getKey() + "=" + entry.getValue());
            }
        }
        return list;
    }
}
