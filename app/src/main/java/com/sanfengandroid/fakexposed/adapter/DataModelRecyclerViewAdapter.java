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

package com.sanfengandroid.fakexposed.adapter;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sanfengandroid.common.model.InstallPackageModel;
import com.sanfengandroid.common.model.base.DataModelFilterable;
import com.sanfengandroid.common.model.base.ShowDataModel;
import com.sanfengandroid.fakexposed.R;
import com.sanfengandroid.fakexposed.listener.AppFilterable;
import com.sanfengandroid.fakexposed.listener.DataModelItemClickListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * {@link RecyclerView.Adapter} that can display a {@link InstallPackageModel}.
 *
 * @author sanfengAndroid
 */
public class DataModelRecyclerViewAdapter<T extends ShowDataModel> extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements Filterable {

    private static final int NO_VIEW_TYPE = -1;
    private static final int ADD_VIEW_TYPE = -2;
    private final List<T> mRoot;
    private final List<T> mSources;
    private final List<T> mFilters;
    private final boolean canSort;
    private final Filter filter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            String search = constraint.toString();
            FilterResults results = new FilterResults();
            mFilters.clear();
            if (TextUtils.equals(search, AppFilterable.SYSTEM_MASK)) {
                mSources.clear();
                for (T item : mRoot) {
                    if (item instanceof DataModelFilterable) {
                        if (((DataModelFilterable) item).isSystemApp()) {
                            mSources.add(item);
                        }
                    }
                }
                mFilters.addAll(mSources);
            } else if (TextUtils.equals(search, AppFilterable.SYSTEM_UNMASK)) {
                mSources.clear();
                for (T item : mRoot) {
                    if (item instanceof DataModelFilterable) {
                        if (!((DataModelFilterable) item).isSystemApp()) {
                            mSources.add(item);
                        }
                    }
                }
                mFilters.addAll(mSources);
            } else if (TextUtils.equals(search, AppFilterable.XPOSED_MASK)) {
                mSources.clear();
                for (T item : mRoot) {
                    if (item instanceof DataModelFilterable) {
                        if (((DataModelFilterable) item).isXposedApp()) {
                            mSources.add(item);
                        }
                    }
                }
                mFilters.addAll(mSources);
            } else if (TextUtils.equals(search, AppFilterable.XPOSED_UNMASK)) {
                mSources.clear();
                for (T item : mRoot) {
                    if (item instanceof DataModelFilterable) {
                        if (!((DataModelFilterable) item).isXposedApp()) {
                            mSources.add(item);
                        }
                    }
                }
                mFilters.addAll(mSources);
            } else if (TextUtils.equals(search, AppFilterable.ALL)) {
                mSources.clear();
                mSources.addAll(mRoot);
                mFilters.addAll(mSources);
            } else if (search.isEmpty()) {
                mFilters.addAll(mSources);
            } else {
                mFilters.clear();
                for (T item : mSources) {
                    if (item instanceof DataModelFilterable) {
                        if (((DataModelFilterable) item).filter(search)) {
                            mFilters.add(item);
                        }
                    }
                }
            }
            if (mFilters.size() > 0 && mFilters.get(0) instanceof Comparable) {
                if (canSort) {
                    Collections.sort(mFilters, null);
                }
            }
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            notifyDataSetChanged();
        }
    };
    private DataModelItemClickListener<T> listener;
    private boolean canAddData;
    private boolean hasLoadComplete = false;

    public DataModelRecyclerViewAdapter() {
        this(true);
    }

    public DataModelRecyclerViewAdapter(boolean sort) {
        mFilters = new ArrayList<>();
        mSources = new ArrayList<>();
        mRoot = new ArrayList<>();
        this.canSort = sort;
    }

    public void setData(List<T> items, boolean canAdd) {
        mFilters.clear();
        mSources.clear();
        mRoot.clear();
        mRoot.addAll(items);
        mFilters.addAll(items);
        mSources.addAll(items);
        canAddData = canAdd;
        hasLoadComplete = true;
        notifyDataSetChanged();
    }

    public void addData(T item) {
        mFilters.add(item);
        mRoot.add(item);
        mSources.add(item);
        if (canSort) {
            Collections.sort(mFilters, null);
        }
        notifyDataSetChanged();
    }

    public T getDataModelByPosition(int pos) {
        return mFilters.get(pos);
    }

    public List<T> getRoot() {
        return mRoot;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;
        if (viewType == NO_VIEW_TYPE) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycle_progress_layout, parent, false);
            view.setTag(NO_VIEW_TYPE);
            return new EmptyViewHolder(view);
        } else if (viewType == ADD_VIEW_TYPE) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycle_add_item_layout, parent, false);
            view.setTag(ADD_VIEW_TYPE);
            view.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemAddClick();
                }
            });
            return new EmptyViewHolder(view);
        }
        view = LayoutInflater.from(parent.getContext()).inflate(mRoot.get(0).getLayoutResId(), parent, false);
        ViewHolder holder = new ViewHolder(view);
        holder.itemView.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (listener != null) {
                listener.onItemClick(v, pos, mFilters.get(pos));
            }
        });
        holder.itemView.setOnLongClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (listener != null) {
                listener.onItemLongClick(v, pos, mFilters.get(pos));
            }
            return true;
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof ViewHolder) {
            T item = mFilters.get(position);
            item.bindView(holder);
        }
    }

    @Override
    public int getItemCount() {
        if (mRoot.isEmpty() && !hasLoadComplete) {
            return 1;
        }
        return mFilters.size() + (canAddData ? 1 : 0);
    }

    @Override
    public int getItemViewType(int position) {
        if (mRoot.isEmpty() && !hasLoadComplete) {
            return NO_VIEW_TYPE;
        }
        if (position >= mFilters.size()) {
            return ADD_VIEW_TYPE;
        }
        return super.getItemViewType(position);
    }

    @Override
    public Filter getFilter() {
        return filter;
    }

    public void setItemClickListener(DataModelItemClickListener<T> listener) {
        this.listener = listener;
    }

    private static class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(View view) {
            super(view);
        }
    }

    private static class EmptyViewHolder extends RecyclerView.ViewHolder {

        public EmptyViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}