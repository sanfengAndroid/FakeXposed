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

package com.sanfengandroid.datafilter.listener;

import android.view.View;

import com.sanfengandroid.common.model.base.ShowDataModel;

public interface DataModelItemClickListener<T extends ShowDataModel> {
    /**
     * 单击监听
     *
     * @param pos 位置
     */
    default boolean onItemClick(View view, int pos, T data) {
        return false;
    }

    /**
     * 长按监听
     *
     * @param pos 位置
     */
    default boolean onItemLongClick(View view, int pos, T data) {
        return false;
    }

    default boolean onItemAddClick() {
        return false;
    }
}
