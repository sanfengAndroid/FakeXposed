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

import com.sanfengandroid.common.model.base.DataModelFilterable;
import com.sanfengandroid.fakexposed.listener.AppFilterable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author sanfengAndroid
 */
public class DataModelSelectRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements Filterable {

    private final int mLayoutId;
    private final DataModelSelectBindViewListener listener;
    private final List<Object> mRoot = new ArrayList<>();
    private final List<Object> mSources = new ArrayList<>();
    private final List<Object> mFilters = new ArrayList<>();
    private final boolean canFilter;
    private final boolean filterSystem;
    private final Filter filter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            String search = constraint.toString();
            FilterResults results = new FilterResults();
            mFilters.clear();
            if (TextUtils.equals(search, AppFilterable.SYSTEM_MASK)) {
                mSources.clear();
                for (Object item : mRoot) {
                    if (item instanceof DataModelFilterable) {
                        if (!((DataModelFilterable) item).isSystemApp()) {
                            mSources.add(item);
                        }
                    }
                }
                mFilters.addAll(mSources);
            } else if (TextUtils.equals(search, AppFilterable.SYSTEM_UNMASK)) {
                mSources.clear();
                mSources.addAll(mRoot);
                mFilters.addAll(mSources);
            } else if (search.isEmpty()) {
                mFilters.addAll(mSources);
            } else {
                mFilters.clear();
                for (Object item : mSources) {
                    if (item instanceof DataModelFilterable) {
                        if (((DataModelFilterable) item).filter(search)) {
                            mFilters.add(item);
                        }
                    }
                }
            }
            if (mFilters.size() > 0 && mFilters.get(0) instanceof Comparable) {
                Collections.sort(mFilters, null);
            }
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            notifyDataSetChanged();
        }
    };

    public DataModelSelectRecyclerViewAdapter(int layoutId, DataModelSelectBindViewListener listener) {
        this(layoutId, listener, false, false);
    }

    public DataModelSelectRecyclerViewAdapter(int layoutId, DataModelSelectBindViewListener listener, boolean canFilter, boolean filterSystem) {
        this.mLayoutId = layoutId;
        this.listener = listener;
        this.canFilter = canFilter;
        this.filterSystem = filterSystem;
    }

    public void setData(List data) {
        if (data != null) {
            mFilters.clear();
            mSources.clear();
            mRoot.clear();
            mRoot.addAll(data);
            mFilters.addAll(data);
            mSources.addAll(data);
            notifyDataSetChanged();
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(mLayoutId, parent, false);
        return new RecyclerView.ViewHolder(view) {
        };
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (listener != null) {
            listener.onBindView(holder.itemView, position, mFilters.get(position));
        }
        holder.itemView.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onItemClick(v, holder.getAdapterPosition(), mFilters.get(holder.getAdapterPosition()));
                    }
                }
        );
    }

    @Override
    public int getItemCount() {
        return mFilters.size();
    }

    @Override
    public Filter getFilter() {
        return canFilter || filterSystem ? filter : null;
    }

    public interface DataModelSelectBindViewListener {
        /**
         * 绑定数据到UI上
         *
         * @param view     视图
         * @param position 当前位置
         * @param data     当前数据
         */
        void onBindView(View view, int position, Object data);

        void onItemClick(View view, int position, Object data);
    }
}
