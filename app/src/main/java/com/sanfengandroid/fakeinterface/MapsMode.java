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

import android.text.TextUtils;

public enum MapsMode {
    // 不操作
    MM_NONE("n"),
    // 增加行,暂未实现
    MM_ADD("a"),
    // 删除行
    MM_REMOVE("r"),
    // 修改行
    MM_MODIFY("m");

    public final String key;

    MapsMode(String key) {
        this.key = key;
    }

    public static MapsMode stringToMapsMode(String key) {
        for (MapsMode mode : MapsMode.values()) {
            if (TextUtils.equals(mode.key, key)) {
                return mode;
            }
        }
        throw new UnsupportedOperationException("Unknown maps mode key: " + key);
    }
}
