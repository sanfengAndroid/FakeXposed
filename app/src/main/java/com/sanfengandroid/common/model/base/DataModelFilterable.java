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

public interface DataModelFilterable {
    /**
     * 查询是否匹配
     *
     * @param condition 过滤添加
     * @return 是否过滤
     */
    boolean filter(String condition);

    /**
     * 查询系统App
     *
     * @return 是否是系统App
     */
    boolean isSystemApp();

    boolean isXposedApp();
}
