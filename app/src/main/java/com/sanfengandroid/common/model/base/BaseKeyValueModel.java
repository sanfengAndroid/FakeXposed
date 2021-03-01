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

import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.sanfengandroid.datafilter.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

public abstract class BaseKeyValueModel extends BaseShowDataModel {
    private String key;
    private String value;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public int getLayoutResId() {
        return R.layout.item_card_view;
    }

    @Override
    public void bindView(RecyclerView.ViewHolder vh) {
        super.bindView(vh);
        TextView title = vh.itemView.findViewById(R.id.item_title);
        if (title != null) {
            title.setText(getKey());
        }
        title = vh.itemView.findViewById(R.id.item_sub_title);
        if (title != null) {
            title.setText(getValue());
        }
    }

    @Override
    public JSONObject serialization() throws JSONException {
        JSONObject json = super.serialization();
        json.put("name", getKey());
        json.put("value", getValue());
        return json;
    }

    @Override
    public void unSerialization(JSONObject json) throws JSONException {
        super.unSerialization(json);
        setKey(json.getString("name"));
        setValue(json.getString("value"));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BaseKeyValueModel that = (BaseKeyValueModel) o;
        return getKey().equals(that.getKey());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getKey());
    }
}
