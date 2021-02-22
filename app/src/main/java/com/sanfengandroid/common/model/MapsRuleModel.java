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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sanfengandroid.common.model.base.BaseKeyValueModel;
import com.sanfengandroid.common.model.base.EditDataModel;
import com.sanfengandroid.common.model.base.ShowDataModel;
import com.sanfengandroid.fakeinterface.MapsMode;
import com.sanfengandroid.fakexposed.R;
import com.sanfengandroid.fakexposed.XpApplication;
import com.sanfengandroid.fakexposed.viewmodel.ApplicationViewModel;


public class MapsRuleModel extends BaseKeyValueModel {
    private MapsMode mode;
    private String rule;

    @Override
    public int getLayoutResId() {
        return R.layout.maps_card_view;
    }

    @Override
    public void bindView(RecyclerView.ViewHolder vh) {
        super.bindView(vh);
        ImageView image = vh.itemView.findViewById(R.id.maps_rule_icon);
        TextView tvKey = vh.itemView.findViewById(R.id.maps_orig_key);
        TextView tvValue = vh.itemView.findViewById(R.id.maps_new_key);
        switch (mode) {
            case MM_MODIFY:
                tvValue.setVisibility(View.VISIBLE);
                image.setImageResource(R.drawable.ic_modify_24dp);
                tvKey.setText(getKey());
                tvValue.setText(rule);
                break;
            case MM_REMOVE:
                tvValue.setVisibility(View.GONE);
                image.setImageResource(R.drawable.ic_delete_24dp);
                tvKey.setText(getKey());
                break;
            default:
                throw new UnsupportedOperationException("Unused " + mode.name());
        }

    }

    @Override
    public void setValue(String value) {
        super.setValue(value);
        if (!TextUtils.isEmpty(value)) {
            String[] args = value.split(":");
            mode = MapsMode.stringToMapsMode(args[0]);
            if (mode == MapsMode.MM_MODIFY) {
                rule = args[1];
            }
        }
    }

    public MapsMode getMode() {
        return mode;
    }

    public String getRule() {
        return rule;
    }

    @NonNull
    @Override
    public String toString() {
        String s = mode.key + " " + getKey();
        if (mode == MapsMode.MM_MODIFY) {
            s = s + "?" + (getValue() == null ? "" : getValue());
        }
        return s;
    }

    public static class MapsRuleEditModel implements EditDataModel {
        private EditText etKey, etValue;
        private RadioButton rbRemove, rbModify;
        private int index = ApplicationViewModel.NO_INDEX;

        @Override
        public View onCreateView(Context context) {
            View view = LayoutInflater.from(context).inflate(R.layout.maps_rule_add_item, null, false);
            etKey = view.findViewById(R.id.add_item_maps_key_et);
            etValue = view.findViewById(R.id.add_item_maps_value_et);
            rbRemove = view.findViewById(R.id.maps_rule_remove);
            rbModify = view.findViewById(R.id.maps_rule_modify);
            LinearLayout layout = view.findViewById(R.id.add_item_maps_replace_view);
            rbRemove.setOnCheckedChangeListener((buttonView, isChecked) -> layout.setVisibility(isChecked ? View.GONE : View.VISIBLE));
            return view;
        }

        @Override
        public void bindData(Context context, ShowDataModel data, int index) {
            this.index = index;
            MapsRuleModel model = (MapsRuleModel) data;
            etKey.setText(model.getKey());
            if (model.getMode() == MapsMode.MM_REMOVE) {
                rbRemove.setChecked(true);
                rbModify.setChecked(false);
            } else {
                rbRemove.setChecked(false);
                rbModify.setChecked(true);
                etValue.setText(model.getRule());
            }
        }

        @Override
        public void onSave() {
            String key = etKey.getText().toString();
            if (TextUtils.isEmpty(key)) {
                XpApplication.getViewModel().setMessage(R.string.input_empty_content);
                return;
            }
            MapsRuleModel model = new MapsRuleModel();
            String value = rbRemove.isChecked() ? MapsMode.MM_REMOVE.key :
                    MapsMode.MM_MODIFY.key + ":" + etValue.getText().toString();
            model.setKey(key);
            model.setValue(value);
            XpApplication.getViewModel().addDataValue(model, index);
            XpApplication.getViewModel().setSaveResult(true);
        }
    }
}
