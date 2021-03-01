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

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.sanfengandroid.common.bean.EnvBean;
import com.sanfengandroid.common.model.base.DataModelType;
import com.sanfengandroid.common.util.LogUtil;
import com.sanfengandroid.common.util.Util;
import com.sanfengandroid.datafilter.BuildConfig;
import com.sanfengandroid.datafilter.R;
import com.sanfengandroid.datafilter.ui.fragments.MainFragment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.Map;

public class NativeTestActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "NativeTest";

    private static final String TEST_KEY = "test_key";
    private static final String TEST_VALUE = "sanfengandroid";
    private static final String TEST_RULE = "sanfeng";
    private static final String TEST_RIGHT = "android";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.native_test_activity);
        if (MainFragment.isActive()) {
            findViewById(R.id.tv_plugin).setVisibility(View.GONE);
        }
        findViewById(R.id.btn_test_maps).setOnClickListener(this);
        findViewById(R.id.btn_local_load).setOnClickListener(this);
        findViewById(R.id.btn_load_test).setOnClickListener(this);
        findViewById(R.id.btn_test_classloader).setOnClickListener(this);
        findViewById(R.id.btn_test_global).setOnClickListener(this);
        findViewById(R.id.btn_test_getenv).setOnClickListener(this);
        findViewById(R.id.btn_test_properties).setOnClickListener(this);
        findViewById(R.id.btn_test_runtime_exec).setOnClickListener(this);
        findViewById(R.id.btn_test_register).setOnClickListener(this);
        findViewById(R.id.btn_test_register1).setOnClickListener(this);
        findViewById(R.id.btn_test_file_hide).setOnClickListener(this);
        findViewById(R.id.btn_test_file_redirect).setOnClickListener(this);
        findViewById(R.id.btn_test_file_access).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_local_load) {
            try {
                NativeHook.initLibraryPath(this);
                AsyncTask.execute(() -> {
                    initTestData(this);
                    NativeInit.initNative(this, Util.getProcessName(this));
                    NativeHook.nativeTest();
                    NativeInit.startNative();
                });
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        } else if (id == R.id.btn_load_test) {
            AsyncTask.execute(() -> {
                initTestData(this);
                NativeHook.nativeTest();
                NativeInit.nativeSync();
            });
        } else if (id == R.id.btn_test_maps) {
            tip("测试maps文件过滤", testMaps());
        } else if (id == R.id.btn_test_classloader) {
            tip("测试类隐藏", testHideClass());
        } else if (id == R.id.btn_test_global) {
            tip("测试Debug属性", testDebugProperty());
        } else if (id == R.id.btn_test_getenv) {
            tip("测试环境变量", testEnv());
        } else if (id == R.id.btn_test_properties) {
            tip("测试全局属性", testSystemProperty());
        } else if (id == R.id.btn_test_runtime_exec) {
            testRuntimeExec();
        } else if (id == R.id.btn_test_file_hide) {
            tip("测试文件隐藏", testFileHide());
        } else if (id == R.id.btn_test_file_redirect) {
            testFileRedirect();
        } else if (id == R.id.btn_test_file_access) {
            tip("测试文件权限", testFileAccess());
        }
    }

    private void tip(String msg, boolean success) {
        Toast.makeText(this, msg + (success ? ": 通过" : ": 未通过"), Toast.LENGTH_SHORT).show();
    }

    public static void initTestData(Context context) {
        File cache = context.getCacheDir();
        Map<String, EnvBean> envs = GlobalConfig.getMap(DataModelType.SYSTEM_ENV_HIDE, EnvBean.class);
        EnvBean bean = new EnvBean(TEST_KEY, TEST_RULE);
        envs.put(bean.name, bean);
        Map<String, String> global = GlobalConfig.getMap(DataModelType.GLOBAL_HIDE, String.class);
        global.put(TEST_KEY, TEST_RIGHT);
        Map<String, String> globalProperty = GlobalConfig.getMap(DataModelType.GLOBAL_SYSTEM_PROPERTY_HIDE, String.class);
        globalProperty.put("ro.zygote", TEST_RIGHT);
        Map<String, String> mapHide = GlobalConfig.getMap(DataModelType.MAPS_HIDE, String.class);
        mapHide.put(BuildConfig.APPLICATION_ID, MapsMode.MM_REMOVE.key);
        try {
            File file = new File(cache, "su");
            if (!file.exists()) {
                file.createNewFile();
            }
            file = new File(cache, TEST_KEY);
            if (!file.exists()) {
                file.createNewFile();
            }
            Map<String, String> map = GlobalConfig.getMap(DataModelType.FILE_HIDE, String.class);
            map.put(file.getAbsolutePath(), file.getAbsolutePath());
            file = new File(cache, TEST_KEY + TEST_RULE);
            FileWriter fw = new FileWriter(file);
            fw.write("test value");
            fw.flush();
            fw.close();
            file = new File(cache, TEST_KEY + TEST_RIGHT);
            fw = new FileWriter(file);
            fw.write("has been redirect");
            fw.flush();
            fw.close();
            map = GlobalConfig.getMap(DataModelType.FILE_REDIRECT_HIDE, String.class);
            map.put(new File(cache, TEST_KEY + TEST_RULE).getAbsolutePath(), file.getAbsolutePath());
            file = new File(cache, TEST_VALUE);
            if (!file.exists()) {
                file.createNewFile();
            }
            map = GlobalConfig.getMap(DataModelType.FILE_ACCESS_HIDE, String.class);
            map.put(file.getAbsolutePath(), "-1:-1:" + FileAccessMask.USR_READ.mode);
        } catch (Throwable e) {
            LogUtil.e(TAG, "error initializing test data", e);
            return;
        }
    }

    private boolean testHideClass() {
        boolean success = false;
        try {
            Class.forName("de.robv.android.xposed.XposedBridge");
        } catch (Throwable e) {
            LogUtil.e(TAG, "test  Class.forName", e);
            success = true;
        }
        try {
            LogUtil.d(TAG, "self class %s", this.getClassLoader().loadClass("de.robv.android.xposed.XposedBridge"));
            success = false;
        } catch (Throwable e) {
            LogUtil.e(TAG, "test ClassLoader().loadClass", e);
        }
        return success;
    }

    private boolean testDebugProperty() {
        return TextUtils.equals(Settings.Global.getString(getContentResolver(), TEST_KEY), TEST_RIGHT);
    }

    private boolean testEnv() {
        return TextUtils.equals(System.getenv(TEST_KEY), TEST_RIGHT);
    }

    private boolean testMaps() {
        LogUtil.d(TAG, "Hook before file exist: " + new File("/proc/self/maps").exists());
        try {
            BufferedReader br = new BufferedReader(new FileReader(new File("/proc/self/maps")));
            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains(BuildConfig.APPLICATION_ID)) {
                    return false;
                }
                LogUtil.d(TAG, line);
            }
            br.close();
        } catch (Throwable e) {
            LogUtil.e(TAG, "test read file failed.", e);
        }
        return true;
    }

    private boolean testSystemProperty() {
        try {
            Class<?> clazz = Class.forName("android.os.SystemProperties");
            Method method = clazz.getMethod("get", String.class, String.class);
            String value = (String) method.invoke(null, "ro.zygote", null);
            LogUtil.d(TAG, "test SystemProperties name: %s, value: %s", "ro.zygote", value);
            return TextUtils.equals(value, TEST_RIGHT);
        } catch (Exception e) {
            LogUtil.e(TAG, "test property failed", e);
        }
        return false;
    }

    private void testRuntimeExec() {
        AsyncTask.execute(() -> {
            try {
                String[] cmd = new String[]{"ls", "/system/lib"};
                Process process = Runtime.getRuntime().exec(cmd);
                BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
                final String line = br.readLine();
                LogUtil.d(TAG, "test runtime result: %s", line);
                NativeTestActivity.this.getWindow().getDecorView().post(() -> tip("测试Runtime执行", TextUtils.equals(line, "fake exec ls /system/lib")));
            } catch (Throwable e) {
                LogUtil.e(TAG, "test runtime error", e);
            }
        });
    }

    private boolean testFileHide() {
        File file = new File(getCacheDir(), "su");
        LogUtil.d("file hide: %s, result: %s", file.getAbsolutePath(), !file.exists());
        boolean success = !file.exists();
        file = new File(getCacheDir(), TEST_KEY);
        LogUtil.d("file hide: %s, result: %s", file.getAbsolutePath(), !file.exists());
        return success && !file.exists();
    }

    private boolean testFileAccess() {
        File file = new File(getCacheDir(), TEST_VALUE);
        return !file.canWrite();
    }

    private void testFileRedirect() {
        AsyncTask.execute(() -> {
            File file = new File(getCacheDir(), TEST_KEY + TEST_RULE);
            try {
                FileReader fr = new FileReader(file);
                BufferedReader br = new BufferedReader(fr);
                final String line = br.readLine();
                NativeTestActivity.this.getWindow().getDecorView().post(() -> tip("测试文件重定向", TextUtils.equals(line, "has been redirect")));
            } catch (Exception e) {
                e.printStackTrace();
            }

        });
    }
}
