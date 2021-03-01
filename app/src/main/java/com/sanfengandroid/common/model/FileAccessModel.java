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

import android.annotation.SuppressLint;
import android.content.Context;
import android.system.StructStat;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.sanfengandroid.common.model.base.BaseKeyValueEditDataModel;
import com.sanfengandroid.common.model.base.BaseKeyValueModel;
import com.sanfengandroid.common.model.base.ShowDataModel;
import com.sanfengandroid.common.util.FileUtil;
import com.sanfengandroid.fakeinterface.FileAccessMask;
import com.sanfengandroid.datafilter.R;
import com.sanfengandroid.datafilter.XpApplication;
import com.sanfengandroid.datafilter.ui.FileBrowseLayout;

import java.io.File;

public class FileAccessModel extends BaseKeyValueModel {
    private int uid = -1, gid = -1;
    private int access;

    public int getUid() {
        return uid;
    }

    public int getGid() {
        return gid;
    }

    public int getAccess() {
        return access;
    }

    @Override
    public int getLayoutResId() {
        return R.layout.fragment_file_access_item;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void bindView(RecyclerView.ViewHolder vh) {
        super.bindView(vh);

        TextView uidTv = vh.itemView.findViewById(R.id.item_uid);
        TextView gidTv = vh.itemView.findViewById(R.id.item_gid);
        TextView accessTv = vh.itemView.findViewById(R.id.item_sub_title);
        uidTv.setText("uid: " + uid);
        gidTv.setText("gid: " + gid);
        accessTv.setText(vh.itemView.getContext().getString(R.string.permission) + ": " + Integer.toOctalString(access));
    }

    @Override
    public String getValue() {
        return super.getValue();
    }

    @Override
    public void setValue(String value) {
        super.setValue(value);
        if (!TextUtils.isEmpty(value)) {
            String[] args = value.split(":");
            uid = Integer.parseInt(args[0]);
            gid = Integer.parseInt(args[1]);
            access = Integer.parseInt(args[2]);
        }
    }

    public static class FileAccessEditModel extends BaseKeyValueEditDataModel {
        private EditText uidEt, gidEt;
        private TextView fileAccessTv;
        private int access;
        private Context mContext;
        private final CompoundButton.OnCheckedChangeListener mListener = (buttonView, isChecked) -> {
            int id = buttonView.getId();
            FileAccessMask fileAccessMask;
            if (id == R.id.owner_read)
                fileAccessMask = FileAccessMask.USR_READ;
            else if (id == R.id.owner_write)
                fileAccessMask = FileAccessMask.USR_WRITE;
            else if (id == R.id.owner_execute)
                fileAccessMask = FileAccessMask.USR_EXEC;
            else if (id == R.id.group_read)
                fileAccessMask = FileAccessMask.GRP_READ;
            else if (id == R.id.group_write)
                fileAccessMask = FileAccessMask.GRP_WRITE;
            else if (id == R.id.group_execute)
                fileAccessMask = FileAccessMask.GRP_EXEC;
            else if (id == R.id.other_read)
                fileAccessMask = FileAccessMask.OTH_READ;
            else if (id == R.id.other_write)
                fileAccessMask = FileAccessMask.OTH_WRITE;
            else if (id == R.id.other_execute)
                fileAccessMask = FileAccessMask.OTH_EXEC;
            else if (id == R.id.set_gid)
                fileAccessMask = FileAccessMask.SET_GID;
            else if (id == R.id.set_uid)
                fileAccessMask = FileAccessMask.SET_UID;
            else if (id == R.id.set_sticky)
                fileAccessMask = FileAccessMask.SET_TX;
            else
                throw new IllegalArgumentException("Unknown");
            updateAccess(fileAccessMask.mode, isChecked);
        };
        private EditText filePathEt;
        private CheckBox dirCheckbox;
        private CheckBox uR, uW, uE, gR, gW, gE, oR, oW, oE, sG, sU, sT;

        private void updateAccess(int mask, boolean add) {
            access = add ? access | mask : access & ~mask;
            fileAccessTv.setText(mContext.getString(R.string.file_permission, Integer.toOctalString(access)));
        }

        @Override
        public void bindData(Context context, ShowDataModel data, int index) {
            this.index = index;
            FileAccessModel model = (FileAccessModel) data;
            String path = model.getKey();
            if (path.endsWith(File.separator)) {
                dirCheckbox.setChecked(true);
                path = path.substring(0, path.length() - 1);
            }
            filePathEt.setText(path);
            String[] args = model.getValue().split(":");
            uidEt.setText(args[0]);
            gidEt.setText(args[1]);
            access = Integer.parseInt(args[2]);
            uR.setChecked((access & FileAccessMask.USR_READ.mode) != 0);
            uW.setChecked((access & FileAccessMask.USR_WRITE.mode) != 0);
            uE.setChecked((access & FileAccessMask.USR_EXEC.mode) != 0);
            gR.setChecked((access & FileAccessMask.GRP_READ.mode) != 0);
            gW.setChecked((access & FileAccessMask.GRP_WRITE.mode) != 0);
            gE.setChecked((access & FileAccessMask.GRP_EXEC.mode) != 0);
            oR.setChecked((access & FileAccessMask.OTH_READ.mode) != 0);
            oW.setChecked((access & FileAccessMask.OTH_WRITE.mode) != 0);
            oE.setChecked((access & FileAccessMask.OTH_EXEC.mode) != 0);
            sU.setChecked((access & FileAccessMask.SET_UID.mode) != 0);
            sG.setChecked((access & FileAccessMask.SET_GID.mode) != 0);
            sT.setChecked((access & FileAccessMask.SET_TX.mode) != 0);
        }

        @SuppressLint("SetTextI18n")
        @Override
        public View onCreateView(Context context) {
            this.mContext = context;
            View view = LayoutInflater.from(context).inflate(R.layout.file_access_add_view, null, false);
            uR = view.findViewById(R.id.owner_read);
            uR.setOnCheckedChangeListener(mListener);
            uW = view.findViewById(R.id.owner_write);
            uW.setOnCheckedChangeListener(mListener);
            uE = view.findViewById(R.id.owner_execute);
            uE.setOnCheckedChangeListener(mListener);
            gR = view.findViewById(R.id.group_read);
            gR.setOnCheckedChangeListener(mListener);
            gW = view.findViewById(R.id.group_write);
            gW.setOnCheckedChangeListener(mListener);
            gE = view.findViewById(R.id.group_execute);
            gE.setOnCheckedChangeListener(mListener);
            oR = view.findViewById(R.id.other_read);
            oR.setOnCheckedChangeListener(mListener);
            oW = view.findViewById(R.id.other_write);
            oW.setOnCheckedChangeListener(mListener);
            oE = view.findViewById(R.id.other_execute);
            oE.setOnCheckedChangeListener(mListener);
            sG = view.findViewById(R.id.set_gid);
            sG.setOnCheckedChangeListener(mListener);
            sU = view.findViewById(R.id.set_uid);
            sU.setOnCheckedChangeListener(mListener);
            sT = view.findViewById(R.id.set_sticky);
            sT.setOnCheckedChangeListener(mListener);


            uidEt = view.findViewById(R.id.uid_et);
            gidEt = view.findViewById(R.id.gid_et);
            fileAccessTv = view.findViewById(R.id.access_text);
            filePathEt = view.findViewById(R.id.file_path);
            dirCheckbox = view.findViewById(R.id.item_is_dir);
            FileBrowseLayout.setFileBrowse(context, view.findViewById(R.id._file_layout), (success, path) -> filePathEt.setText(path));
            filePathEt.setOnFocusChangeListener((v, hasFocus) -> {
                if (!hasFocus) {
                    String path = filePathEt.getText().toString();
                    if (!TextUtils.isEmpty(path)) {
                        StructStat stat = FileUtil.getFileStat(path);
                        if (stat == null) {
                            return;
                        }
                        dirCheckbox.setChecked(FileUtil.isDirectory(stat));
                        uidEt.setText(Integer.toString(stat.st_uid));
                        gidEt.setText(Integer.toString(stat.st_gid));

                        uR.setChecked((stat.st_mode & FileAccessMask.USR_READ.mode) != 0);
                        uW.setChecked((stat.st_mode & FileAccessMask.USR_WRITE.mode) != 0);
                        uE.setChecked((stat.st_mode & FileAccessMask.USR_EXEC.mode) != 0);
                        gR.setChecked((stat.st_mode & FileAccessMask.GRP_READ.mode) != 0);
                        gW.setChecked((stat.st_mode & FileAccessMask.GRP_WRITE.mode) != 0);
                        gE.setChecked((stat.st_mode & FileAccessMask.GRP_EXEC.mode) != 0);
                        oR.setChecked((stat.st_mode & FileAccessMask.OTH_READ.mode) != 0);
                        oW.setChecked((stat.st_mode & FileAccessMask.OTH_WRITE.mode) != 0);
                        oE.setChecked((stat.st_mode & FileAccessMask.OTH_EXEC.mode) != 0);
                        sG.setChecked((stat.st_mode & FileAccessMask.SET_GID.mode) != 0);
                        sU.setChecked((stat.st_mode & FileAccessMask.SET_UID.mode) != 0);
                        sT.setChecked((stat.st_mode & FileAccessMask.SET_TX.mode) != 0);
                    }
                }
            });
            return view;
        }

        @Override
        public void onSave() {
            String path = filePathEt.getText().toString();
            if (TextUtils.isEmpty(path)) {
                XpApplication.getViewModel().setMessage(R.string.input_empty_content);
                return;
            }
            if (dirCheckbox.isChecked()) {
                path = path + File.separator;
            }
            FileAccessModel model = new FileAccessModel();
            model.setKey(path);
            int uid = uidEt.getText().toString().isEmpty() ? -1 : Integer.parseInt(uidEt.getText().toString());
            int gid = gidEt.getText().toString().isEmpty() ? -1 : Integer.parseInt(gidEt.getText().toString());
            model.setValue(uid + ":" + gid + ":" + access);
            XpApplication.getViewModel().addDataValue(model, index);
            XpApplication.getViewModel().setSaveResult(true);
        }
    }
}
