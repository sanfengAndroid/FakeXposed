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

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.sanfengandroid.common.model.base.BaseShowDataModel;
import com.sanfengandroid.common.model.base.DataModelFilterable;
import com.sanfengandroid.fakexposed.R;

import org.json.JSONObject;

import java.util.Objects;

public class InstallPackageModel extends BaseShowDataModel implements Comparable<InstallPackageModel>, DataModelFilterable {
    public String pkg;
    public String appName;
    public String versionName;
    public long versionCode;
    public Drawable icon;
    public boolean isSystemApp;
    public boolean all;
    public boolean available;
    public boolean isXposedModule;


    public InstallPackageModel() {
    }

    @Override
    public int compareTo(InstallPackageModel o) {
        if (o == null) {
            return -1;
        }
        if (all) {
            return -1;
        }
        if (o.all) {
            return 1;
        }
        if (available && !o.available) {
            return -1;
        }
        if (!available && o.available) {
            return 1;
        }

        return appName.compareTo(o.appName);
    }

    @Override
    public int getLayoutResId() {
        return R.layout.fragment_installed_package_item;
    }

    @Override
    public void bindView(RecyclerView.ViewHolder vh) {
        ((TextView) vh.itemView.findViewById(R.id.installed_pkg_name)).setText(pkg);
        ((TextView) vh.itemView.findViewById(R.id.installed_pkg_appname)).setText(appName);
        ((TextView) vh.itemView.findViewById(R.id.installed_pkg_version_name)).setText(versionName);
        ((ImageView) vh.itemView.findViewById(R.id.installed_pkg_icon)).setImageDrawable(icon);
        SwitchMaterial appSwitch = vh.itemView.findViewById(R.id.item_switch);
        if (appSwitch != null) {
            appSwitch.setVisibility(View.GONE);
        }
        MaterialCardView cardView = (MaterialCardView) vh.itemView;
        cardView.setCheckable(true);
        cardView.setFocusable(true);
        cardView.setChecked(available);
    }

    @Override
    public boolean filter(String condition) {
        return appName.contains(condition) || pkg.contains(condition);
    }

    @Override
    public boolean isSystemApp() {
        return isSystemApp;
    }

    @Override
    public boolean isXposedApp() {
        return isXposedModule;
    }

    @Override
    public JSONObject serialization() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void unSerialization(JSONObject value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int hashCode() {
        return Objects.hash(pkg);
    }
}
