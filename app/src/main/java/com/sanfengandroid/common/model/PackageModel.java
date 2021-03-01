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

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.sanfengandroid.datafilter.R;
import com.sanfengandroid.datafilter.XpApplication;
import com.sanfengandroid.datafilter.listener.DataModelItemClickListener;
import com.sanfengandroid.datafilter.ui.DialogBuilder;

/**
 * @author sanfengAndroid
 */
public class PackageModel extends StringModel implements DataModelItemClickListener<PackageModel>, Comparable<PackageModel> {
    public static final String XPOSED_PACKAGE_MASK = "xposed";
    private InstallPackageModel installPackageModel;

    public PackageModel() {
    }

    public PackageModel(String packageName) {
        super(packageName);
        installPackageModel = XpApplication.getViewModel().getInstallApp(getPackageName());
    }

    @Override
    public int getLayoutResId() {
        return R.layout.fragment_installed_package_item;
    }

    @Override
    public void bindView(RecyclerView.ViewHolder vh) {
        if (installPackageModel == null) {
            installPackageModel = XpApplication.getViewModel().getInstallApp(getPackageName());
        }
        super.bindView(vh);
        if (installPackageModel != null) {
            ((TextView) vh.itemView.findViewById(R.id.installed_pkg_name)).setText(installPackageModel.pkg);
            ((TextView) vh.itemView.findViewById(R.id.installed_pkg_appname)).setText(installPackageModel.appName);
            ((TextView) vh.itemView.findViewById(R.id.installed_pkg_version_name)).setText(installPackageModel.versionName);
            ((ImageView) vh.itemView.findViewById(R.id.installed_pkg_icon)).setImageDrawable(installPackageModel.icon);
        } else {
            if (getPackageName().equals(XPOSED_PACKAGE_MASK)) {
                ((TextView) vh.itemView.findViewById(R.id.installed_pkg_name)).setText(getPackageName());
                ((TextView) vh.itemView.findViewById(R.id.installed_pkg_appname)).setText(R.string.filter_xposed_module);
            } else {
                ((TextView) vh.itemView.findViewById(R.id.installed_pkg_name)).setText(getPackageName());
                ((TextView) vh.itemView.findViewById(R.id.installed_pkg_appname)).setText(R.string.uninstall);
            }
        }
    }

    public String getPackageName() {
        return getValue();
    }

    public void setPackageName(String pkg) {
        setValue(pkg);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public boolean onItemLongClick(View view, int pos, PackageModel data) {
        DialogBuilder.confirmCancelShow(view.getContext(), R.string.delete_item_tip, (dialog, which) -> {
            XpApplication.getViewModel().deleteDataValue(data);
        }, null);
        return true;
    }

    @Override
    public int compareTo(PackageModel o) {
        if (getPackageName().equals(XPOSED_PACKAGE_MASK)) {
            return -1;
        }
        if (o.getPackageName().equals(XPOSED_PACKAGE_MASK)) {
            return 1;
        }
        return this.getPackageName().compareTo(o.getPackageName());
    }
}
