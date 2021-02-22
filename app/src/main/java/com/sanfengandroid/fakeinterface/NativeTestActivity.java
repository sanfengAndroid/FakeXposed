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

package com.sanfengandroid.fakeinterface;

import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.sanfengandroid.common.util.LogUtil;
import com.sanfengandroid.common.util.Util;
import com.sanfengandroid.fakexposed.BuildConfig;
import com.sanfengandroid.fakexposed.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Method;

public class NativeTestActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "NativeTest";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.native_test_activity);
        findViewById(R.id.btn_test_maps).setOnClickListener(this);
        findViewById(R.id.btn_test_java_relink).setOnClickListener(this);
        findViewById(R.id.btn_local_load).setOnClickListener(this);
        findViewById(R.id.btn_test_classloader).setOnClickListener(this);
        findViewById(R.id.btn_test_global).setOnClickListener(this);
        findViewById(R.id.btn_test_getenv).setOnClickListener(this);
        findViewById(R.id.btn_test_properties).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_local_load) {
            try {
                NativeHook.initLibraryPath(this);
                NativeInit.initNative(this, Util.getProcessName(this));
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        } else if (id == R.id.btn_test_maps) {
            LogUtil.d(TAG, "Hook before file exist: " + new File("/proc/self/maps").exists());
            try {
                BufferedReader br = new BufferedReader(new FileReader(new File("/proc/self/maps")));
                String line;
                while ((line = br.readLine()) != null) {
                    LogUtil.d(TAG, line);
                }
                br.close();
            } catch (Throwable e) {
                LogUtil.e(TAG, "test read file failed.", e);
            }
        } else if (id == R.id.btn_test_classloader) {
            try {
                Class.forName("de.robv.android.xposed.XposedBridge");
            } catch (Throwable e) {
                LogUtil.e(TAG, "test class loader2", e);
            }

            try {
                LogUtil.d(TAG, "self class %s", Class.forName(BuildConfig.APPLICATION_ID));
            } catch (Throwable e) {
                LogUtil.e(TAG, "test self class", e);
            }
            try {
                Method method = ClassLoader.class.getDeclaredMethod("defineClass", String.class, byte[].class, int.class, int.class);
                method.setAccessible(true);
                Class<?> nativeHook = (Class<?>) method.invoke(getClassLoader(), BuildConfig.APPLICATION_ID, null, 0, 0);
                LogUtil.d(TAG, "get self class %s", nativeHook);
            }catch (Throwable e){
                LogUtil.e(TAG, "test self class2", e);
            }
        }  else if (id == R.id.btn_test_java_relink) {
            NativeHook.relinkLibrary("libdl.so");
        } else if (id == R.id.btn_test_global) {
            LogUtil.d(TAG, "Build TAGS: %s", Build.TAGS);
            LogUtil.d(TAG, "Global adb test: %s", Settings.Global.getString(getContentResolver(), "adb_enabled"));
        } else if (id == R.id.btn_test_getenv) {
            LogUtil.d(TAG, "All environment: %s", System.getenv());
            LogUtil.d(TAG, "ANDROID_ART_ROOT environment: %s", System.getenv("ANDROID_ART_ROOT"));
            LogUtil.d(TAG, "ANDROID_ART_ROOTS environment: %s", System.getenv("ANDROID_ART_ROOTS"));
        } else if (id == R.id.btn_test_properties) {
            try {
                Class<?> clazz = Class.forName("android.os.SystemProperties");
                Method method = clazz.getMethod("get", String.class, String.class);
                LogUtil.d(TAG, "test SystemProperties name: wlan.driver.status, value: %s", method.invoke(null, "ro.build.selinux", "null"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
