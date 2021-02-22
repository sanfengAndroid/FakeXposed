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
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;

import com.sanfengandroid.common.model.base.BaseKeyValueEditDataModel;
import com.sanfengandroid.common.model.base.BaseKeyValueModel;
import com.sanfengandroid.common.model.base.ShowDataModel;
import com.sanfengandroid.fakexposed.R;
import com.sanfengandroid.fakexposed.XpApplication;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author sanfengAndroid
 */
public class GlobalModel extends BaseKeyValueModel {

    @Override
    public JSONObject serialization() throws JSONException {
        return super.serialization();
    }

    @Override
    public void unSerialization(JSONObject json) throws JSONException {
        super.unSerialization(json);
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    public static class GlobalIntEditModel extends BaseKeyValueEditDataModel {

        @Override
        public View onCreateView(Context context) {
            View view = super.onCreateView(context);
            etValue.setInputType(InputType.TYPE_CLASS_TEXT);
            return view;
        }

        @Override
        protected String getKeyHint(Context context) {
            return context.getString(R.string.add_item_key_property);
        }

        @Override
        protected String getValueHint(Context context) {
            return context.getString(R.string.add_item_value_property);
        }

        @Override
        public void bindData(Context context, ShowDataModel data, int index) {
            this.index = index;
            GlobalModel model = (GlobalModel) data;
            etKey.setText(model.getKey());
            etValue.setText(model.getValue());
        }

        @Override
        public void onSave() {
            String key = etKey.getText().toString();
            if (TextUtils.isEmpty(key)) {
                XpApplication.getViewModel().setMessage(R.string.input_empty_content);
                return;
            }
            GlobalModel model = new GlobalModel();
            model.setKey(key);
            model.setValue(etValue.getText().toString());
            XpApplication.getViewModel().addDataValue(model, index);
            XpApplication.getViewModel().setSaveResult(true);
        }
    }
}
