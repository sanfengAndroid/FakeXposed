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
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sanfengandroid.common.model.base.DataModelFilterable;
import com.sanfengandroid.common.model.base.EditDataModel;
import com.sanfengandroid.fakexposed.R;
import com.sanfengandroid.fakexposed.XpApplication;
import com.sanfengandroid.fakexposed.adapter.DataModelSelectRecyclerViewAdapter;
import com.sanfengandroid.fakexposed.viewmodel.ApplicationViewModel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author sanfengAndroid
 */
public class PackageEditModel implements EditDataModel {
    private final DataModelSelectRecyclerViewAdapter.DataModelSelectBindViewListener listener = new DataModelSelectRecyclerViewAdapter.DataModelSelectBindViewListener() {
        @Override
        public void onBindView(View view, int position, Object data) {
            AddPackageBean d = (AddPackageBean) data;
            ((ImageView) view.findViewById(R.id.add_pkg_icon)).setImageDrawable(d.icon);
            ((TextView) view.findViewById(R.id.add_pkg_name)).setText(d.appName);
            CheckBox box = view.findViewById(R.id.add_pkg_checked);
            box.setChecked(d.checked);
        }

        @Override
        public void onItemClick(View view, int position, Object data) {
            CheckBox box = view.findViewById(R.id.add_pkg_checked);
            AddPackageBean bean = (AddPackageBean) data;
            bean.checked = !bean.checked;
            box.setChecked(bean.checked);
        }
    };
    private List<AddPackageBean> addPackageBeans;
    private Filter filter;

    @Override
    public View onCreateView(Context context) {
        ApplicationViewModel viewModel = XpApplication.getViewModel();
        List<InstallPackageModel> all = viewModel.getInstalledAll();
        List<PackageModel> selects = (List<PackageModel>) viewModel.getDataValue();
        Set<String> added = new HashSet<>();
        if (selects != null) {
            for (PackageModel model : selects) {
                added.add(model.getValue());
            }
        }
        List<AddPackageBean> data = new ArrayList<>();
        for (InstallPackageModel pm : all) {
            if (!added.contains(pm.pkg)) {
                AddPackageBean bean = new AddPackageBean();
                bean.icon = pm.icon;
                bean.appName = pm.appName;
                bean.checked = false;
                bean.packageName = pm.pkg;
                bean.isSystem = pm.isSystemApp;
                data.add(bean);
            }
        }
        addPackageBeans = data;
        View view = LayoutInflater.from(context).inflate(R.layout.select_add_view, null, false);
        RecyclerView rv = view.findViewById(R.id.add_select_rv);
        DataModelSelectRecyclerViewAdapter adapter = new DataModelSelectRecyclerViewAdapter(R.layout.package_add_item_layout, listener, true, true);
        rv.setAdapter(adapter);
        rv.setLayoutManager(new LinearLayoutManager(context));
        adapter.setData(data);
        rv.addItemDecoration(new DividerItemDecoration(context, DividerItemDecoration.VERTICAL));
        filter = adapter.getFilter();
        return view;
    }

    @Override
    public void onSave() {
        Set<PackageModel> cs = new HashSet<>();

        for (AddPackageBean bean : addPackageBeans) {
            if (bean.checked) {
                cs.add(new PackageModel(bean.packageName));
            }
        }
        ApplicationViewModel viewModel = XpApplication.getViewModel();
        if (!cs.isEmpty()) {
            viewModel.addDataValue(cs);
        }
        viewModel.setSaveResult(true);
    }

    @Override
    public boolean showFilter() {
        return true;
    }

    @Override
    public Filter getFilter() {
        return filter;
    }

    @Override
    public boolean showFilterSystem() {
        return true;
    }

    private static class AddPackageBean implements DataModelFilterable {
        public Drawable icon;
        public String appName;
        public String packageName;
        public boolean checked;
        public boolean isSystem;
        public boolean isXposed;

        @Override
        public boolean filter(String condition) {
            return appName.contains(condition);
        }

        @Override
        public boolean isSystemApp() {
            return isSystem;
        }

        @Override
        public boolean isXposedApp() {
            return isXposed;
        }
    }
}
