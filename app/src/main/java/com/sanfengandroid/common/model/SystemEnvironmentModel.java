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

package com.sanfengandroid.common.model;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.sanfengandroid.common.bean.EnvBean;
import com.sanfengandroid.common.model.base.BaseKeyValueEditDataModel;
import com.sanfengandroid.common.model.base.BaseShowDataModel;
import com.sanfengandroid.common.model.base.ShowDataModel;
import com.sanfengandroid.fakexposed.R;
import com.sanfengandroid.fakexposed.XpApplication;
import com.sanfengandroid.fakexposed.listener.DataModelItemClickListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

/**
 * 保存格式
 * key=Base64(value0),Base64(value1),Base64(value2) ...,Base64(valueN)
 *
 * @author sanfengAndroid
 */
public class SystemEnvironmentModel extends BaseShowDataModel implements DataModelItemClickListener<SystemEnvironmentModel> {
    private EnvBean bean;

    @Override
    public int getLayoutResId() {
        return R.layout.item_card_view;
    }

    @Override
    public void bindView(RecyclerView.ViewHolder vh) {
        super.bindView(vh);
        TextView title = vh.itemView.findViewById(R.id.item_title);
        title.setText(bean.name);
        TextView subtitle = vh.itemView.findViewById(R.id.item_sub_title);
        StringBuilder sb = new StringBuilder();
        for (String s : bean.regions) {
            sb.append(s).append('\n');
        }
        subtitle.setText(sb.toString());
    }

    public EnvBean getBean() {
        return bean;
    }

    public void setBean(EnvBean bean) {
        this.bean = bean;
    }

    @Override
    public JSONObject serialization() throws JSONException {
        if (TextUtils.isEmpty(bean.name)) {
            return null;
        }
        JSONObject json = super.serialization();
        json.put("name", bean.name);
        JSONArray array = new JSONArray();
        for (String s : bean.regions) {
            array.put(s);
        }
        json.put("regions", array);
        return json;
    }

    @Override
    public void unSerialization(JSONObject json) throws JSONException {
        super.unSerialization(json);
        bean = new EnvBean(json.getString("name"));
        bean.regions = new ArrayList<>();
        JSONArray array = json.getJSONArray("regions");
        int size = array.length();
        for (int i = 0; i < size; i++) {
            bean.regions.add(array.getString(i));
        }
    }

    @Override
    public boolean onItemClick(View view, int pos, SystemEnvironmentModel data) {
        return false;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SystemEnvironmentModel model = (SystemEnvironmentModel) o;
        return bean.equals(model.bean);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bean);
    }

    public static class SystemEnvironmentEditModel extends BaseKeyValueEditDataModel {
        @Override
        protected String getKeyHint(Context context) {
            return context.getString(R.string.add_item_key_system_env);
        }

        @Override
        protected String getValueHint(Context context) {
            return context.getString(R.string.add_item_value_system_env);
        }

        @Override
        public void bindData(Context context, ShowDataModel data, int index) {
            this.index = index;
            SystemEnvironmentModel model = (SystemEnvironmentModel) data;
            etKey.setText(model.bean.name);

            if (!model.bean.regions.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (String s : model.bean.regions) {
                    sb.append(s).append('\n');
                }
                sb.deleteCharAt(sb.length() - 1);
                etValue.setText(sb.toString());
            }
        }

        @Override
        public void onSave() {
            String name = etKey.getText().toString();
            if (TextUtils.isEmpty(name)) {
                XpApplication.getViewModel().setMessage(R.string.input_empty_content);
                return;
            }
            EnvBean bean = new EnvBean(name);
            bean.regions = new ArrayList<>();
            bean.regions.addAll(Arrays.asList(etValue.getText().toString().split("\n")));
            SystemEnvironmentModel model = new SystemEnvironmentModel();
            model.setBean(bean);
            XpApplication.getViewModel().addDataValue(model, index);
            XpApplication.getViewModel().setSaveResult(true);
        }
    }
}
