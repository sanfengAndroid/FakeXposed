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

import com.google.android.material.textfield.TextInputLayout;
import com.sanfengandroid.common.model.base.BaseKeyValueEditDataModel;
import com.sanfengandroid.common.model.base.BaseKeyValueModel;
import com.sanfengandroid.common.model.base.ShowDataModel;
import com.sanfengandroid.common.util.FileUtil;
import com.sanfengandroid.datafilter.R;
import com.sanfengandroid.datafilter.XpApplication;
import com.sanfengandroid.datafilter.ui.FileBrowseLayout;

import java.io.File;

public class FileRedirectModel extends BaseKeyValueModel {

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    public static class FileRedirectEditModel extends BaseKeyValueEditDataModel {
        private CheckBox box;

        @Override
        public View onCreateView(Context context) {
            View view = LayoutInflater.from(context).inflate(R.layout.file_redirect_add_view, null, false);
            TextInputLayout keyLayout = view.findViewById(R.id.item_key);
            TextInputLayout valueLayout = view.findViewById(R.id.item_value);
            FileBrowseLayout.setFileBrowse(context, keyLayout, (success, path) -> etKey.setText(path));
            FileBrowseLayout.setFileBrowse(context, valueLayout, (success, path) -> etValue.setText(path));
            keyLayout.setHint(getKeyHint(context));
            valueLayout.setHint(getValueHint(context));
            etKey = view.findViewById(R.id.item_key_edit);
            etValue = view.findViewById(R.id.item_value_edit);
            box = view.findViewById(R.id.item_is_dir);
            etKey.setOnFocusChangeListener((v, hasFocus) -> {
                if (!hasFocus) {
                    String path = etKey.getText().toString();
                    if (!TextUtils.isEmpty(path)) {
                        box.setChecked(FileUtil.isDirectory(path));
                    }
                }
            });
            return view;
        }

        @Override
        protected String getKeyHint(Context context) {
            return context.getString(R.string.src_path);
        }

        @Override
        protected String getValueHint(Context context) {
            return context.getString(R.string.dst_path);
        }

        @Override
        public void onSave() {
            String src = etKey.getText().toString();
            String dst = etValue.getText().toString();

            if (TextUtils.isEmpty(src) || TextUtils.isEmpty(dst)) {
                XpApplication.getViewModel().setMessage(R.string.input_empty_content);
                return;
            }
            FileRedirectModel fileModel = new FileRedirectModel();
            File srcFile = new File(src);
            File dstFile = new File(dst);
            boolean dir = box.isChecked();
            if (srcFile.canRead() && dstFile.canRead()) {
                if (srcFile.isDirectory() != dstFile.isDirectory()) {
                    XpApplication.getViewModel().setMessage(R.string.file_type_inconsistent);
                    return;
                }
                dir = srcFile.isDirectory();
            }
            fileModel.setKey(dir ? srcFile.getAbsolutePath() + File.separator : srcFile.getAbsolutePath());
            fileModel.setValue(dir ? dstFile.getAbsolutePath() + File.separator : dstFile.getAbsolutePath());
            XpApplication.getViewModel().addDataValue(fileModel, index);
            XpApplication.getViewModel().setSaveResult(true);
        }

        @Override
        public void bindData(Context context, ShowDataModel data, int index) {
            FileRedirectModel fm = (FileRedirectModel) data;
            boolean dir = fm.getKey().endsWith("/");
            this.index = index;
            box.setChecked(dir);
            if (dir) {
                etKey.setText(fm.getKey().substring(0, fm.getKey().length() - 1));
                etValue.setText(fm.getValue().substring(0, fm.getValue().length() - 1));
            } else {
                super.bindData(context, data, index);
            }
        }
    }
}
