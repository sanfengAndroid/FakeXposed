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

package com.sanfengandroid.datafilter.ui.activties;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.sanfengandroid.datafilter.BuildConfig;
import com.sanfengandroid.datafilter.R;
import com.sanfengandroid.datafilter.ui.DialogBuilder;

public class SettingsActivity extends AppBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setViewLayout(R.layout.settings_activity);
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.container, new SettingsFragment())
                    .commit();
        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        setAppBarTitle(R.string.settings);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected int getMenuInflateId() {
        return R.menu.menu_setting;
    }

    public static class SettingsFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceClickListener {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.setting_preferences, rootKey);

            findPreference("native_tip").setOnPreferenceClickListener(this);
            findPreference("follow_wechat").setOnPreferenceClickListener(this);
            findPreference("donate").setOnPreferenceClickListener(this);
            findPreference("github").setOnPreferenceClickListener(this);
            findPreference("share").setOnPreferenceClickListener(this);
            Preference version = findPreference("version");
            version.setSummary(BuildConfig.VERSION_NAME);
            version.setOnPreferenceClickListener(this);
        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            String key = preference.getKey();
            SettingsActivity activity = ((SettingsActivity) requireActivity());
            switch (key) {
                case "native_tip":
                    DialogBuilder.confirmShow(requireContext(), R.string.native_hook_descriptor, R.string.native_hook_tip, null);
                    break;
                case "follow_wechat":
                    activity.copyWechat();
                    break;
                case "donate":
                    activity.donate();
                    break;
                case "github":
                    activity.github();
                    break;
                case "version":
                    activity.update(true);
                    break;
                case "share":
                    activity.shared();
                    break;
            }
            return true;
        }
    }
}