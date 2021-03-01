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
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.google.android.material.textfield.TextInputLayout;
import com.sanfengandroid.datafilter.R;
import com.sanfengandroid.datafilter.viewmodel.ApplicationViewModel;

public abstract class BaseKeyValueEditDataModel implements EditDataModel {
    protected EditText etKey;
    protected EditText etValue;
    protected int index = ApplicationViewModel.NO_INDEX;

    @Override
    public View onCreateView(Context context) {
        View view = LayoutInflater.from(context).inflate(R.layout.key_value_add_view_layout, null, false);
        TextInputLayout layout = view.findViewById(R.id.item_key);
        layout.setHint(getKeyHint(context));
        layout = view.findViewById(R.id.item_value);
        layout.setHint(getValueHint(context));
        etKey = view.findViewById(R.id.item_key_edit);
        etValue = view.findViewById(R.id.item_value_edit);
        return view;
    }

    protected String getKeyHint(Context context) {
        return context.getString(R.string.add_item_key_property);
    }

    protected String getValueHint(Context context) {
        return context.getString(R.string.add_item_value_property);
    }

    @Override
    public void bindData(Context context, ShowDataModel data, int index) {
        if (!(data instanceof BaseKeyValueModel)) {
            return;
        }
        BaseKeyValueModel model = (BaseKeyValueModel) data;
        this.index = index;
        etKey.setText(model.getKey());
        etValue.setText(model.getValue());
    }

    @Override
    public abstract void onSave();
}
