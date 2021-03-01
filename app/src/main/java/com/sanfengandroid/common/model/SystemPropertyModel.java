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

import android.text.TextUtils;

import com.sanfengandroid.common.model.base.BaseKeyValueEditDataModel;
import com.sanfengandroid.common.model.base.BaseKeyValueModel;
import com.sanfengandroid.datafilter.R;
import com.sanfengandroid.datafilter.XpApplication;

public class SystemPropertyModel extends BaseKeyValueModel {

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    public static class SystemPropertyEditModel extends BaseKeyValueEditDataModel {

        @Override
        public void onSave() {
            String input = etKey.getText().toString();
            if (TextUtils.isEmpty(input)) {
                XpApplication.getViewModel().setMessage(R.string.input_empty_content);
                return;
            }
            SystemPropertyModel addModel = new SystemPropertyModel();
            addModel.setKey(input);
            addModel.setValue(etValue.getText().toString());
            XpApplication.getViewModel().addDataValue(addModel, index);
            XpApplication.getViewModel().setSaveResult(true);
        }
    }
}
