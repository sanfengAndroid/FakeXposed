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

package com.sanfengandroid.fakexposed.viewmodel;

import com.sanfengandroid.fakexposed.BuildConfig;

import org.json.JSONException;
import org.json.JSONObject;

public class AppBean {
    public static final String UPDATE_URL = "https://cdn.jsdelivr.net/gh/sanfengAndroid/service-configure@main/fakexposed.json";
    private static final String KEY_APP_STANDARD = "app_standard_" + BuildConfig.APP_TYPE;
    private String version;
    private long versionCode;
    private String link;
    private String noteCN;
    private String noteEN;

    public AppBean(String data) {
        try {
            JSONObject json = new JSONObject(data);
            json = json.getJSONObject(KEY_APP_STANDARD);
            version = json.optString("version", "");
            versionCode = json.optLong("versionCode", 0);
            link = json.optString("link", "");
            noteCN = json.optString("noteCN", "");
            noteEN = json.optString("noteEN", "");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public String getVersion() {
        return version;
    }

    public long getVersionCode() {
        return versionCode;
    }

    public String getLink() {
        return link;
    }

    public String getNoteCN() {
        return noteCN;
    }

    public String getNoteEN() {
        return noteEN;
    }

    @Override
    public String toString() {
        return "AppBean{" +
                "version='" + version + '\'' +
                ", versionCode=" + versionCode +
                ", link='" + link + '\'' +
                ", noteCN='" + noteCN + '\'' +
                ", noteEN='" + noteEN + '\'' +
                '}';
    }
}
