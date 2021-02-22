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

import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONException;
import org.json.JSONObject;


/**
 * 子类应重写equals方法才能保证正常添加和删除
 *
 * @author sanfengAndroid
 */
public interface ShowDataModel {

    /**
     * 获取该数据源绑定的布局
     *
     * @return 布局资源id
     */
    int getLayoutResId();

    /**
     * 动态绑定视图
     *
     * @param vh 该项viewHolder
     */
    void bindView(RecyclerView.ViewHolder vh);


    JSONObject serialization() throws JSONException;

    void unSerialization(JSONObject json) throws JSONException;

    void hookAvailable(boolean available);

    boolean isAvailable();
}
