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
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.sanfengandroid.common.bean.ExecBean;
import com.sanfengandroid.common.model.base.BaseShowDataModel;
import com.sanfengandroid.common.model.base.EditDataModel;
import com.sanfengandroid.common.model.base.ShowDataModel;
import com.sanfengandroid.fakexposed.R;
import com.sanfengandroid.fakexposed.XpApplication;
import com.sanfengandroid.fakexposed.viewmodel.ApplicationViewModel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

public class RuntimeExecModel extends BaseShowDataModel {
    private ExecBean bean;

    @Override
    public int getLayoutResId() {
        return R.layout.runtime_exec_card_view;
    }

    @Override
    public void bindView(RecyclerView.ViewHolder vh) {
        super.bindView(vh);
        TextView tv = vh.itemView.findViewById(R.id.old_cmd_tv);
        tv.setText(bean.oldCmd);
        tv = vh.itemView.findViewById(R.id.new_cmd_tv);
        tv.setText(bean.newCmd);
        if (bean.args != null) {
            tv = vh.itemView.findViewById(R.id.new_cmd_args_tv);
            StringBuilder sb = new StringBuilder();
            for (String s : bean.args) {
                sb.append(s).append('\n');
            }
            tv.setText(sb.toString());
        }
        tv = vh.itemView.findViewById(R.id.input_stream_string);
        tv.setText(bean.inputStream == null ? "null" : bean.inputStream);
        tv = vh.itemView.findViewById(R.id.output_stream_string);
        tv.setText(bean.outStream == null ? "null" : bean.outStream);
        tv = vh.itemView.findViewById(R.id.error_stream_string);
        tv.setText(bean.errStream == null ? "null" : bean.errStream);
    }

    @Override
    public JSONObject serialization() throws JSONException {
        JSONObject json = super.serialization();
        json.put("old", bean.oldCmd);
        json.put("new", bean.newCmd);
        json.put("replace_args", bean.replaceArgs);
        if (bean.args != null) {
            JSONArray array = new JSONArray();
            for (String s : bean.args) {
                array.put(s);
            }
            json.put("args", array);
        }
        if (bean.inputStream != null) {
            json.put("input", bean.inputStream);
        }
        if (bean.errStream != null) {
            json.put("error", bean.errStream);
        }
        if (bean.outStream != null) {
            json.put("output", bean.outStream);
        }
        return json;
    }

    @Override
    public void unSerialization(JSONObject json) throws JSONException {
        super.unSerialization(json);
        bean = new ExecBean();
        bean.oldCmd = json.getString("old");
        bean.newCmd = json.getString("new");
        JSONArray array = json.optJSONArray("args");
        if (array != null) {
            bean.args = new String[array.length()];
            for (int i = 0; i < bean.args.length; i++) {
                bean.args[i] = array.getString(i);
            }
        }
        bean.replaceArgs = json.getBoolean("replace_args");
        bean.inputStream = json.optString("input");
        bean.errStream = json.optString("error");
        bean.outStream = json.optString("input");
    }

    public ExecBean getBean() {
        return bean;
    }

    public void setBean(ExecBean bean) {
        this.bean = bean;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RuntimeExecModel model = (RuntimeExecModel) o;
        return bean.equals(model.bean);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bean);
    }

    public static class RuntimeExecEditModel implements EditDataModel {
        private EditText oldCmdEt, newCmdEt, argsEt, inputEt, outputEt, errorEt;
        private CheckBox replaceArgsCb;
        private int index = ApplicationViewModel.NO_INDEX;

        @Override
        public View onCreateView(Context context) {
            View view = LayoutInflater.from(context).inflate(R.layout.runtime_exec_add_item, null, false);
            oldCmdEt = view.findViewById(R.id.item_old_cmd_et);
            newCmdEt = view.findViewById(R.id.item_cmd_et);
            argsEt = view.findViewById(R.id.item_args_et);
            inputEt = view.findViewById(R.id.item_input_string_et);
            outputEt = view.findViewById(R.id.item_output_string_et);
            errorEt = view.findViewById(R.id.item_error_string_et);
            replaceArgsCb = view.findViewById(R.id.item_args_replace);
            LinearLayout layout = view.findViewById(R.id.item_args_tip);
            replaceArgsCb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                layout.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            });

            return view;
        }

        @Override
        public void bindData(Context context, ShowDataModel data, int index) {
            this.index = index;
            RuntimeExecModel model = (RuntimeExecModel) data;
            oldCmdEt.setText(model.bean.oldCmd);
            newCmdEt.setText(model.bean.newCmd);
            replaceArgsCb.setChecked(model.bean.replaceArgs);
            if (model.bean.replaceArgs && model.bean.args.length > 0) {
                StringBuilder sb = new StringBuilder();
                for (String s : model.bean.args) {
                    sb.append(s).append(' ');
                }
                sb.deleteCharAt(sb.length() - 1);
                argsEt.setText(sb.toString());
            }
            inputEt.setText(model.bean.inputStream);
            outputEt.setText(model.bean.outStream);
            errorEt.setText(model.bean.errStream);
        }

        @Override
        public void onSave() {
            String old = oldCmdEt.getText().toString();
            if (TextUtils.isEmpty(old)) {
                XpApplication.getViewModel().setMessage(R.string.input_empty_content);
                return;
            }
            ExecBean bean = new ExecBean();
            bean.oldCmd = old;
            bean.newCmd = newCmdEt.getText().toString();
            bean.replaceArgs = replaceArgsCb.isChecked();
            if (bean.replaceArgs) {
                bean.args = argsEt.getText().toString().split(" ");
            }
            String value = inputEt.getText().toString();
            if (!TextUtils.isEmpty(value)) {
                bean.inputStream = value;
            }
            value = outputEt.getText().toString();
            if (!TextUtils.isEmpty(value)) {
                bean.outStream = value;
            }
            value = errorEt.getText().toString();
            if (!TextUtils.isEmpty(value)) {
                bean.errStream = value;
            }
            RuntimeExecModel model = new RuntimeExecModel();
            model.setBean(bean);
            XpApplication.getViewModel().addDataValue(model, index);
            XpApplication.getViewModel().setSaveResult(true);
        }
    }
}
