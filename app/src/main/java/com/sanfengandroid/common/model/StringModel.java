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
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputLayout;
import com.sanfengandroid.common.model.base.BaseShowDataModel;
import com.sanfengandroid.common.model.base.EditDataModel;
import com.sanfengandroid.common.model.base.ShowDataModel;
import com.sanfengandroid.fakexposed.R;
import com.sanfengandroid.fakexposed.XpApplication;
import com.sanfengandroid.fakexposed.viewmodel.ApplicationViewModel;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

/**
 * @author sanfengAndroid
 */
public class StringModel extends BaseShowDataModel implements EditDataModel {
    public static final int INPUT_EMPTY_TIP = R.string.input_empty_content;
    protected int index = ApplicationViewModel.NO_INDEX;
    protected EditText et;
    private String value;

    public StringModel() {
    }

    public StringModel(String value) {
        this.value = value;
    }

    @Override
    public int getLayoutResId() {
        return R.layout.fragment_string_item;
    }

    @Override
    public void bindView(RecyclerView.ViewHolder vh) {
        super.bindView(vh);
        TextView content = vh.itemView.findViewById(R.id.item_content);
        if (content != null) {
            content.setText(value);
        }
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public JSONObject serialization() throws JSONException {
        JSONObject json = super.serialization();
        json.put("name", value);
        return json;
    }

    @Override
    public void unSerialization(JSONObject json) throws JSONException {
        super.unSerialization(json);
        this.value = json.getString("name");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        StringModel model = (StringModel) o;
        return value.equals(model.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public View onCreateView(Context context) {
        View view = LayoutInflater.from(context).inflate(R.layout.edit_string_view, null, false);
        et = view.findViewById(R.id.add_string_et);
        TextView tip = view.findViewById(R.id.add_string_tip);
        TextInputLayout layout = view.findViewById(R.id.item_key);
        layout.setHint(getInputHint(context));
        tip.setText(XpApplication.getViewModel().getDataModelType().tipId);
        return view;
    }

    @Override
    public void bindData(Context context, ShowDataModel data, int index) {
        this.index = index;
        StringModel edit = (StringModel) data;
        value = edit.getValue();
        et.setText(value);
    }

    protected String getInputHint(Context context) {
        return context.getString(R.string.add_item_key_name);
    }

    @Override
    public void onSave() {
        if (et != null) {
            String input = et.getText().toString();
            if (!input.isEmpty()) {
                StringModel model;
                try {
                    model = this.getClass().newInstance();
                } catch (IllegalAccessException | InstantiationException e) {
                    throw new RuntimeException(this.getClass().getName() + " no parameterless constructor", e);
                }
                model.setValue(input);
                XpApplication.getViewModel().addDataValue(model, index);
                XpApplication.getViewModel().setSaveResult(true);
            } else {
                XpApplication.getViewModel().setMessage(R.string.input_empty_content);
            }
        }
    }

    protected String getInput() {
        if (et != null) {
            return et.getText().toString();
        }
        return null;
    }
}
