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

package com.sanfengandroid.fakexposed.ui.fragments;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.sanfengandroid.common.Const;
import com.sanfengandroid.common.model.InstallPackageModel;
import com.sanfengandroid.common.model.base.DataModelType;
import com.sanfengandroid.common.model.base.ShowDataModel;
import com.sanfengandroid.fakexposed.R;
import com.sanfengandroid.fakexposed.XpApplication;
import com.sanfengandroid.fakexposed.adapter.DataModelRecyclerViewAdapter;
import com.sanfengandroid.fakexposed.listener.AppFilterable;
import com.sanfengandroid.fakexposed.listener.DataModelItemClickListener;
import com.sanfengandroid.fakexposed.viewmodel.ApplicationViewModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A fragment representing a list of Items.
 */
public class InstalledPackageFragment extends Fragment implements Filterable, AppFilterable.SystemAppFilter, AppFilterable.XposedAppFilter, DataModelItemClickListener<InstallPackageModel> {

    public static final String VIEW_TAG = "installed";
    private DataModelRecyclerViewAdapter<InstallPackageModel> mAdapter;
    private ApplicationViewModel mViewModel;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public InstalledPackageFragment() {
    }

    // TODO: Customize parameter initialization
    @SuppressWarnings("unused")
    public static InstalledPackageFragment newInstance() {
        return new InstalledPackageFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewModel = new ViewModelProvider(XpApplication.getInstance()).get(ApplicationViewModel.class);
        mAdapter = new DataModelRecyclerViewAdapter<>();
        mAdapter.setItemClickListener(this);
        mViewModel.getInstalled().observe(this, installPackageModels -> {
            InstallPackageModel item = new InstallPackageModel();
            item.pkg = Const.GLOBAL_PACKAGE;
            item.appName = requireContext().getString(R.string.all_pkg);
            item.all = true;
            List<InstallPackageModel> apps = new ArrayList<>(installPackageModels);
            apps.add(item);

            mViewModel.setHookAppsObserver(this, map -> {
                for (Map.Entry<String, Boolean> entry : map.entrySet()) {
                    if (entry.getValue() != null && entry.getValue()) {
                        String pkg = entry.getKey();
                        for (InstallPackageModel model : apps) {
                            if (TextUtils.equals(model.pkg, pkg)) {
                                model.available = true;
                            }
                        }
                    }
                }
                Collections.sort(apps, null);
                mAdapter.setData(apps, false);
            });
            mViewModel.initHookApps();
        });
        mViewModel.getInstalledAll();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_recycler_view, container, false);
        RecyclerView recyclerView = view.findViewById(R.id.list);
        if (recyclerView != null) {
            Context context = view.getContext();
            recyclerView.setLayoutManager(new LinearLayoutManager(context));
            recyclerView.addItemDecoration(new DividerItemDecoration(context, DividerItemDecoration.VERTICAL));
            recyclerView.setAdapter(mAdapter);
        }
        return view;
    }

    @Override
    public Filter getFilter() {
        if (mAdapter != null) {
            return mAdapter.getFilter();
        }
        return null;
    }

    @Override
    public void filterSystemApp(AppFilterable.Option option) {
        Filter filtered = getFilter();
        String str;
        switch (option) {
            case ALL:
                str = AppFilterable.ALL;
                break;
            case THIS:
                str = AppFilterable.SYSTEM_MASK;
                break;
            case OTHER:
            default:
                str = AppFilterable.SYSTEM_UNMASK;
                break;
        }
        if (filtered != null) {
            filtered.filter(str);
        }
    }

    @Override
    public void filterXposedApp(AppFilterable.Option option) {
        Filter filtered = getFilter();
        String str;
        switch (option) {
            case ALL:
                str = AppFilterable.ALL;
                break;
            case THIS:
                str = AppFilterable.XPOSED_MASK;
                break;
            case OTHER:
            default:
                str = AppFilterable.XPOSED_UNMASK;
                break;
        }
        if (filtered != null) {
            filtered.filter(str);
        }
    }

    @Override
    public boolean onItemClick(View view, int pos, InstallPackageModel data) {
        DataModelType type = mViewModel.getDataModelType();
        FragmentManager fm = getParentFragmentManager();
        mViewModel.setCurrentPackage(data.pkg);
        ItemFragment<? extends ShowDataModel> itemFragment = ItemFragment.newInstance(1, type.valueType);
        fm.beginTransaction().addToBackStack(null).replace(R.id.container, itemFragment).commit();
        return true;
    }

    @Override
    public boolean onItemLongClick(View view, int pos, InstallPackageModel data) {
        MaterialCardView cardView = (MaterialCardView) view;
        cardView.setChecked(!cardView.isChecked());
        data.available = cardView.isChecked();
        mViewModel.updateHookApp(data.pkg, data.available);
        return true;
    }

    @Override
    public boolean onItemAddClick() {
        return true;
    }


}