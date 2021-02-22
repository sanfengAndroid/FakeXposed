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

package com.sanfengandroid.common.model.base;

import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.sanfengandroid.fakexposed.R;

import org.json.JSONException;
import org.json.JSONObject;

public abstract class BaseShowDataModel implements ShowDataModel {
    private boolean available = true;

    @Override
    public JSONObject serialization() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("available", available);
        return json;
    }

    @Override
    public void unSerialization(JSONObject json) throws JSONException {
        available = json.getBoolean("available");
    }

    @Override
    public void bindView(RecyclerView.ViewHolder vh) {
        SwitchMaterial open = vh.itemView.findViewById(R.id.item_switch);
        if (open != null) {
            open.setChecked(available);
        }
    }

    @Override
    public void hookAvailable(boolean available) {
        this.available = available;
    }

    @Override
    public boolean isAvailable() {
        return available;
    }
}
