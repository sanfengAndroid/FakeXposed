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


import android.content.Context;
import android.view.View;
import android.widget.Filter;

/**
 * 添加数据模型
 *
 * @author sanfengAndroid
 */
public interface EditDataModel {

    /**
     * 对具体的模型创建布局
     *
     * @param context 上下文
     * @return 布局
     */
    default View onCreateView(Context context) {
        return null;
    }

    /**
     * 保存数据回调,这里也做一些数据校验
     */
    default void onSave() {
    }

    /**
     * 判断已经修改
     *
     * @return 已经改变返回true
     */
    default boolean hasChange() {
        return false;
    }

    default void bindData(Context context, ShowDataModel data, int index) {
    }

    default boolean showFilter() {
        return false;
    }

    default boolean showFilterSystem() {
        return false;
    }

    default Filter getFilter() {
        return null;
    }
}
