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

import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.Observer;

import com.google.android.material.snackbar.Snackbar;
import com.sanfengandroid.common.model.base.EditDataModel;
import com.sanfengandroid.common.model.base.ShowDataModel;
import com.sanfengandroid.fakexposed.R;
import com.sanfengandroid.fakexposed.XpApplication;
import com.sanfengandroid.fakexposed.listener.AppFilterable;
import com.sanfengandroid.fakexposed.ui.DialogBuilder;
import com.sanfengandroid.fakexposed.viewmodel.ApplicationViewModel;

/**
 * @author sanfengAndroid
 */
public class EditItemDialogFragment extends AppCompatDialogFragment {
    private ApplicationViewModel mViewModel;
    private SearchView searchView;
    private EditDataModel editDataModel;
    private final Observer<Pair<ShowDataModel, Integer>> observer = new Observer<Pair<ShowDataModel, Integer>>() {
        @Override
        public void onChanged(Pair<ShowDataModel, Integer> pair) {
            editDataModel.bindData(requireContext(), pair.first, pair.second);
        }
    };
    private boolean showFilter, showFilterSystem;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, R.style.Widget_XpFilter_FullScreen_Dialog);
        setCancelable(true);
        setHasOptionsMenu(true);
        mViewModel = XpApplication.getViewModel();
        mViewModel.setSaveResultObserver(this, b -> {
            if (b) {
                Toast.makeText(requireContext(), R.string.add_data_success, Toast.LENGTH_SHORT).show();
                requireActivity().finish();
            }
        });
        mViewModel.setSnackMessageObserver(this, v -> {
            if (v != null) {
                Snackbar.make(this.requireView(), v, Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.edit_item_dialog_fragment, container, false);
        Toolbar toolbar = view.findViewById(R.id.toolbar);
        AppCompatActivity activity = (AppCompatActivity) requireActivity();
        activity.setSupportActionBar(toolbar);
        activity.getSupportActionBar().setDisplayShowCustomEnabled(true);
        toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_clear_material);
        initView(view);
        toolbar.setNavigationOnClickListener(v -> exit());
        mViewModel.setEditModelObserver(this, observer);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    private void exit() {
        if (searchView != null && !searchView.isIconified()) {
            searchView.setIconified(true);
            return;
        }
        if (editDataModel.hasChange()) {
            DialogBuilder.confirmCancelShow(requireContext(), R.string.data_has_changed, (dialog, which) -> requireActivity().finish(), null);
        } else {
            requireActivity().finish();
        }
    }

    public boolean onBackPressed() {
        exit();
        return true;
    }

    private void initView(View parent) {
        try {
            editDataModel = mViewModel.getDataModelType().addType.newInstance();
        } catch (IllegalAccessException | java.lang.InstantiationException e) {
            Toast.makeText(this.requireContext(), "创建对象失败,请确保类有个无参的public构造函数: " + mViewModel.getDataModelType().addType.getName(), Toast.LENGTH_SHORT).show();
            return;
        }
        LinearLayout layout = parent.findViewById(R.id.item_contain);

        View view = editDataModel.onCreateView(requireContext());
        showFilter = editDataModel.showFilter();
        showFilterSystem = editDataModel.showFilterSystem();
        if (view != null) {
            layout.addView(view, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        }
        ((AppCompatActivity) requireActivity()).getSupportActionBar().setTitle(mViewModel.getDataModelType().nameId);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.menu_edit, menu);
        MenuItem mSearchItem = menu.findItem(R.id.action_search);
        mSearchItem.setVisible(showFilter);
        if (showFilter) {
            searchView = (SearchView) mSearchItem.getActionView();
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    return false;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    Filter filter = editDataModel.getFilter();
                    if (filter != null) {
                        filter.filter(newText);
                    }
                    return true;
                }
            });
        }

        MenuItem mFilterSystemItem = menu.findItem(R.id.action_filter_system_app);
        mFilterSystemItem.setVisible(showFilterSystem);
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_filter_system_app) {
            item.setChecked(!item.isChecked());
            Filter filter = editDataModel.getFilter();
            if (filter != null) {
                filter.filter(item.isChecked() ? AppFilterable.SYSTEM_MASK : AppFilterable.SYSTEM_UNMASK);
            }
            return true;
        }
        if (id == R.id.save) {
            if (editDataModel != null) {
                editDataModel.onSave();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
