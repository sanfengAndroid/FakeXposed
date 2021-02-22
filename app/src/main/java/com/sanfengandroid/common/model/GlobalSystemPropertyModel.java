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
public class GlobalSystemPropertyModel extends BaseKeyValueModel {


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

    public static class GlobalSystemPropertyEditModel extends BaseKeyValueEditDataModel {

        @Override
        protected String getKeyHint(Context context) {
            return context.getString(R.string.add_item_key_system_property);
        }

        @Override
        protected String getValueHint(Context context) {
            return context.getString(R.string.add_item_value_system_property);
        }

        @Override
        public void bindData(Context context, ShowDataModel data, int index) {
            this.index = index;
            GlobalSystemPropertyModel model = (GlobalSystemPropertyModel) data;
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
            if (etValue.getText().toString().length() > 91){
                XpApplication.getViewModel().setMessage(R.string.property_value_too_long);
                return;
            }
            GlobalSystemPropertyModel model = new GlobalSystemPropertyModel();
            model.setKey(key);
            model.setValue(etValue.getText().toString());
            XpApplication.getViewModel().addDataValue(model, index);
            XpApplication.getViewModel().setSaveResult(true);
        }
    }
}
