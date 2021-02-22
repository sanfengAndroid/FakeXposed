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

package com.sanfengandroid.common.bean;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class EnvBean {
    public String name;
    public List<String> regions = Collections.EMPTY_LIST;

    public EnvBean(String name) {
        this.name = name;
    }

    public EnvBean(String name, String region) {
        this.name = name;
        regions = new ArrayList<>(1);
        regions.add(region);
    }

    public EnvBean(String name, List<String> regions) {
        this.name = name;
        if (regions != null) {
            this.regions = regions;
        }
    }

    public boolean contain(String value) {
        if (TextUtils.isEmpty(value)) {
            return false;
        }
        List<String> newRegions = new ArrayList<>();
        for (String region : regions) {
            if (value.matches(region)) {
                newRegions.add(region);
            }
        }
        if (!newRegions.isEmpty()) {
            regions = newRegions;
            return true;
        }
        return false;
    }

    public String replace(String value) {
        for (String region : regions) {
            value = value.replaceAll(region, "");
        }
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EnvBean bean = (EnvBean) o;
        return name.equals(bean.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}