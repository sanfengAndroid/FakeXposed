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

public enum NativeHookStatus {
    /*
     * 关闭
     * */
    DISABLE(0),
    /**
     * 开启
     */
    OPEN(1);

    private final int option;

    NativeHookStatus(int i) {
        this.option = i;
    }

    public int getOption() {
        return option;
    }
}
