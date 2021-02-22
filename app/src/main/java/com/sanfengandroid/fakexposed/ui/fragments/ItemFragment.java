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
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.sanfengandroid.common.model.base.ShowDataModel;
import com.sanfengandroid.fakexposed.R;
import com.sanfengandroid.fakexposed.XpApplication;
import com.sanfengandroid.fakexposed.adapter.DataModelRecyclerViewAdapter;
import com.sanfengandroid.fakexposed.listener.DataModelItemClickListener;
import com.sanfengandroid.fakexposed.ui.DialogBuilder;
import com.sanfengandroid.fakexposed.ui.activties.EditItemDialogActivity;
import com.sanfengandroid.fakexposed.viewmodel.ApplicationViewModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * A fragment representing a list of Items.
 */
public class ItemFragment<T extends ShowDataModel> extends Fragment implements DataModelItemClickListener<T> {
    private DataModelRecyclerViewAdapter<T> adapter;
    private ApplicationViewModel mViewModel;
    private final Observer<List<? extends ShowDataModel>> observer = new Observer<List<? extends ShowDataModel>>() {
        @Override
        public void onChanged(List<? extends ShowDataModel> showDataModels) {
            mViewModel.setSaveObserver(true);
            List<T> list = new ArrayList<>((Collection<T>) showDataModels);
            if (!list.isEmpty() && list.get(0) instanceof Comparable) {
                Collections.sort(list, null);
            }
            adapter.setData(list, true);
        }
    };
    private String type;
    private RecyclerView mRecyclerView;
    private TextView tvTip;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ItemFragment() {
    }

    @SuppressWarnings("unused")
    public static <T extends ShowDataModel> ItemFragment<T> newInstance(int columnCount, Class<T> clazz) {
        return new ItemFragment<T>();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewModel = new ViewModelProvider(XpApplication.getInstance()).get(ApplicationViewModel.class);
        reload();
    }

    public void reload() {
        mViewModel.removeDataObserver(observer);
        adapter = new DataModelRecyclerViewAdapter<>(false);
        adapter.setItemClickListener(this);
        if (mRecyclerView != null) {
            mRecyclerView.setAdapter(adapter);
            tvTip.setText(mViewModel.getDataModelType().tipId);
        }
        mViewModel.setDataObserver(this, observer);
        mViewModel.initDataModelByType();
        this.type = mViewModel.getDataModelType().name();
    }

    @Override
    public void onDestroy() {
        mViewModel.setSaveObserver(false);
        super.onDestroy();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.item_fragment, container, false);
        // Set the adapter
        tvTip = view.findViewById(R.id.item_tip);
        tvTip.setText(mViewModel.getDataModelType().tipId);
        mRecyclerView = view.findViewById(R.id.list);
        if (mRecyclerView != null) {
            Context context = view.getContext();
            mRecyclerView.setLayoutManager(new LinearLayoutManager(context));
            mRecyclerView.addItemDecoration(new DividerItemDecoration(context, DividerItemDecoration.VERTICAL));
            mRecyclerView.setAdapter(adapter);
        }

        return view;
    }

    @Override
    public boolean onItemClick(View view, int pos, T data) {
        if (data instanceof DataModelItemClickListener) {
            DataModelItemClickListener<T> childListener = (DataModelItemClickListener<T>) data;
            if (childListener.onItemClick(view, pos, data)) {
                return true;
            }
        }
        SwitchMaterial hookSwitch = view.findViewById(R.id.item_switch);
        if (hookSwitch != null) {
            hookSwitch.setChecked(!hookSwitch.isChecked());
            data.hookAvailable(hookSwitch.isChecked());
            mViewModel.updateDataValue();
            return true;
        }
        DialogBuilder.confirmCancelShow(requireContext(), R.string.delete_item_tip, (dialog, which) -> {
            mViewModel.deleteDataValue(ItemFragment.this.adapter.getDataModelByPosition(pos));
        }, null);
        return true;
    }

    @Override
    public boolean onItemLongClick(View view, int pos, T data) {
        if (data instanceof DataModelItemClickListener) {
            DataModelItemClickListener<T> childListener = (DataModelItemClickListener<T>) data;
            if (childListener.onItemLongClick(view, pos, data)) {
                return true;
            }
        }

        DialogBuilder.confirmNeutralShow(requireContext(), R.string.edit_item_tip,
                (dialog, which) -> {
                    XpApplication.getViewModel().setEditModelValue(data, pos);
                    Intent intent = new Intent(getActivity(), EditItemDialogActivity.class);
                    startActivity(intent);
                },
                null,
                (dialog, which) -> mViewModel.deleteDataValue(ItemFragment.this.adapter.getDataModelByPosition(pos))
        );
        return false;
    }

    @Override
    public boolean onItemAddClick() {
        Intent intent = new Intent(getActivity(), EditItemDialogActivity.class);
        startActivity(intent);
        return true;
    }
}