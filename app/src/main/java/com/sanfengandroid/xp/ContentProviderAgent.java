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

package com.sanfengandroid.xp;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Base64;

import com.sanfengandroid.common.Const;
import com.sanfengandroid.common.model.base.DataModelType;
import com.sanfengandroid.common.util.LogUtil;
import com.sanfengandroid.fakexposed.SPProvider;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Map;

/**
 * System进程和自身ContentProvider共用
 */
public class ContentProviderAgent {
    private static final String TAG = ContentProviderAgent.class.getSimpleName();
    private static final Uri URI = Const.XP_CONTENT_URI;
    private static final Bundle SUCCESS;
    private static ContentProviderAgent singleton = null;

    static {
        SUCCESS = new RemoteArgsBuild().success().build();
    }

    private WeakReference<Context> mContext;
    private ContentProviderAgent() {
    }

    public static ContentProviderAgent getInstance() {
        if (singleton == null) {
            synchronized (ContentProviderAgent.class) {
                if (singleton == null) {
                    singleton = new ContentProviderAgent();
                }
            }
        }
        return singleton;
    }

    public static boolean setHookAppEnable(Context context, String pkg, Boolean enable) {
        return new RemoteArgsUnpack(callRemote(context, RemoteCall.SET_APP_ENABLE, new RemoteArgsBuild(pkg).setBoolean(enable).build())).success();
    }

    public static boolean setAppStringConfig(Context context, String pkg, DataModelType type, String value) {
        RemoteArgsBuild build = new RemoteArgsBuild(pkg).setDataType(type).setString(value);
        return new RemoteArgsUnpack(callRemote(context, RemoteCall.SET_APP_STRING_CONFIG, build.build())).success();
    }

    public static RemoteArgsUnpack getHookAppConfig(Context context, String pkg) {
        return new RemoteArgsUnpack(callRemote(context, RemoteCall.GET_APP_CONFIG, new RemoteArgsBuild(pkg).build()));
    }

    public static String syncConfigurationToSystem(Context context) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            return "IO operation not allowed in the main thread.";
        }
        File[] files = SPProvider.getAllConfigurationFiles(context);
        if (files == null) {
            return null;
        }
        // 单次传递数据 375 kb上限
        char[] chars = new char[375 * 1024];
        FileReader fr = null;
        try {
            for (File file : files) {
                long length = file.length();
                if (length < 1) {
                    continue;
                }
                int block = -1;
                fr = new FileReader(file);
                int len;
                while ((len = fr.read(chars)) > 0) {
                    String data = new String(Base64.encode(new String(chars, 0, len).getBytes(), Base64.NO_WRAP));
                    RemoteArgsUnpack unBuild = new RemoteArgsUnpack(syncFileToSystem(context, file.getName(), data, block++));
                    if (!unBuild.success()) {
                        LogUtil.e(TAG, "remote sync file error, %s", unBuild.error());
                        return "sync file " + file.getName() + " block " + block + " error, " + unBuild.error();
                    }
                }
                fr.close();
            }
        } catch (Exception e) {
            LogUtil.e(TAG, "sync configuration file error", e);
            return "sync configuration file error: " + e.getMessage();
        } finally {
            if (fr != null) {
                try {
                    fr.close();
                } catch (IOException ignore) {
                }
            }
        }
        return null;
    }

    private static Bundle syncFileToSystem(Context context, String name, String data, int block) {
        try {
            RemoteArgsBuild build = new RemoteArgsBuild().setSyncFileName(name).setSyncFileData(data).setSyncFileBlock(block);
            return callRemote(context, RemoteCall.SYNC_CONFIGURATION, build.build());
        } catch (Throwable e) {
            LogUtil.e(TAG, "call remote method %s error.", RemoteCall.SYNC_CONFIGURATION, e);
        }
        return null;
    }

    private static boolean callRemoteSuccess(Context context, RemoteCall method, Bundle extra) {
        RemoteArgsUnpack build = new RemoteArgsUnpack(callRemote(context, method, extra));
        LogUtil.v(TAG, "call remote method: %s, result: %s", method, build.success());
        return build.success();
    }

    private static Bundle callRemote(Context context, RemoteCall method, Bundle extra) {
        try {
            LogUtil.v(TAG, "call remote uri: %s, method: %s", URI, method.method);
            return context.getContentResolver().call(URI, RemoteCall.METHOD_HOOK_FLAG, method.method, extra);
        } catch (Throwable e) {
            LogUtil.e(TAG, "call remote system method error, uri: %s", URI, e);
        }
        return null;
    }

    public static boolean remoteIsActive(Context context) {
        return callRemoteSuccess(context, RemoteCall.IS_ACTIVE, null);
    }

    public Bundle invoke(Context context, RemoteCall method, Bundle caller) {
        mContext = new WeakReference<>(context);
        Bundle ret;
        if (method == null) {
            RemoteArgsBuild build = new RemoteArgsBuild();
            build.setReturnCode(RemoteCall.RESULT_VALUE_UNDEFINED);
            return build.build();
        }
        if (method == RemoteCall.IS_ACTIVE) {
            ret = SUCCESS;
        } else {
            // 以下都是需要参数的
            RemoteArgsUnpack unBuild = new RemoteArgsUnpack(caller);
            ret = dispatcher(method, unBuild);
        }
        if (ret == null) {
            RemoteArgsBuild build = new RemoteArgsBuild();
            ret = build.setReturnCode(RemoteCall.RESULT_VALUE_CALL_ERROR).build();
        }
        return ret;
    }

    private Bundle dispatcher(RemoteCall call, RemoteArgsUnpack caller) {
        Bundle ret = null;
        if (!caller.exist()) {
            return new RemoteArgsBuild().setReturnCode(RemoteCall.RESULT_VALUE_PARAM_NULL_ERROR).build();
        }
        try {
            switch (call) {
                case GET_APP_CONFIG:
                    ret = getAppConfigRemote(caller);
                    break;
                case WRITE_APP_CONFIG:
                    break;
                case SET_APP_ENABLE:
                    ret = setHookAppRemote(caller);
                    break;
                case SET_APP_STRING_CONFIG:
                    ret = setHookAppStringConfigRemote(caller);
                    break;
                case SYNC_CONFIGURATION:
                    ret = writeConfiguration(caller);
                    break;
                default:
                    ret = new RemoteArgsBuild().setReturnCode(RemoteCall.RESULT_VALUE_UNDEFINED).build();
                    break;
            }
        } catch (IllegalArgumentException e) {
            ret = new RemoteArgsBuild().setReturnCode(RemoteCall.RESULT_VALUE_PARAM_ERROR).setErrorString(e.getMessage()).build();
        }
        return ret;
    }

    private Bundle getAppConfigRemote(RemoteArgsUnpack caller) {
        // 第一步判断该APP是否开启Hook,如果没开启Hook则没必要进行下一步
        RemoteArgsBuild build = new RemoteArgsBuild().enable(SPProvider.getHookAppEnable(mContext.get(), caller.getPackageNonNull()));
        if (build.appEnable) {
            Map<String, String[]> map = SPProvider.getOverloadAppJsonAvailable(mContext.get(), caller.getPackage());
            build.arrayMap(map);
        }
        build.success();
        return build.build();
    }

    private Bundle writeConfiguration(RemoteArgsUnpack caller) {
        boolean success = SPProvider.writeSyncConfiguration(caller.syncFileName(), new String(Base64.decode(caller.syncFileData(), Base64.NO_WRAP)), caller.syncFileBlock());
        if (success) {
            return SUCCESS;
        }
        RemoteArgsBuild build = new RemoteArgsBuild();
        build.setReturnCode(RemoteCall.RESULT_VALUE_EXEC_ERROR);
        return build.build();
    }

    private Bundle setHookAppRemote(RemoteArgsUnpack caller) {
        SPProvider.setHookApp(mContext.get(), caller.getPackageNonNull(), caller.getBoolean());
        return SUCCESS;
    }

    private Bundle setHookAppStringConfigRemote(RemoteArgsUnpack caller) {
        SPProvider.putAppStringConfig(mContext.get(), caller.getPackageNonNull(), caller.getDataType(), caller.getString());
        return SUCCESS;
    }

    private static class RemoteArgsBuild {
        private String pkg;
        private Boolean commonBoolean;
        private int dataType;
        private String string;
        private int retCode = RemoteCall.RESULT_VALUE_CALL_SUCCESS;

        private Map<String, String[]> arrayMap;

        private Map<String, String> stringMap;

        private Boolean appEnable;

        private Boolean isActive;

        private String errorString;

        private String syncFileName;
        private String syncFileData;
        private Integer syncFileBlock;

        private RemoteArgsBuild() {
        }

        private RemoteArgsBuild(String pkg) {
            this.pkg = pkg;
        }

        public RemoteArgsBuild setPackage(String pkg) {
            this.pkg = pkg;
            return this;
        }

        public RemoteArgsBuild setBoolean(Boolean value) {
            commonBoolean = value;
            return this;
        }

        public RemoteArgsBuild setString(String value) {
            string = value;
            return this;
        }

        public RemoteArgsBuild setDataType(DataModelType type) {
            dataType = type.ordinal();
            return this;
        }

        public RemoteArgsBuild success() {
            retCode = RemoteCall.RESULT_VALUE_CALL_SUCCESS;
            return this;
        }

        public RemoteArgsBuild setActive(boolean active) {
            isActive = active;
            return this;
        }

        public RemoteArgsBuild enable(boolean enable) {
            appEnable = enable;
            return this;
        }

        public RemoteArgsBuild setReturnCode(int code) {
            retCode = code;
            return this;
        }

        public RemoteArgsBuild setErrorString(String value) {
            errorString = value;
            return this;
        }

        public RemoteArgsBuild arrayMap(Map<String, String[]> value) {
            arrayMap = value;
            return this;
        }

        public RemoteArgsBuild stringMap(Map<String, String> value) {
            stringMap = value;
            return this;
        }

        public RemoteArgsBuild setSyncFileName(String fileName) {
            syncFileName = fileName;
            return this;
        }

        public RemoteArgsBuild setSyncFileData(String data) {
            syncFileData = data;
            return this;
        }

        public RemoteArgsBuild setSyncFileBlock(int block) {
            syncFileBlock = block;
            return this;
        }

        public Bundle build() {
            Bundle bundle = new Bundle();
            if (pkg != null) {
                bundle.putString(RemoteCall.KEY_APP_PKG, pkg);
            }
            if (commonBoolean != null) {
                bundle.putBoolean(RemoteCall.KEY_COMMON_BOOLEAN, commonBoolean);
            }
            if (dataType != 0) {
                bundle.putInt(RemoteCall.KEY_DATA_TYPE, dataType);
            }
            if (string != null) {
                bundle.putString(RemoteCall.KEY_COMMON_STRING, string);
            }
            if (arrayMap != null && !arrayMap.isEmpty()) {
                for (Map.Entry<String, String[]> entry : arrayMap.entrySet()) {
                    bundle.putStringArray(entry.getKey(), entry.getValue());
                }
            }
            if (stringMap != null && !stringMap.isEmpty()) {
                for (Map.Entry<String, String> entry : stringMap.entrySet()) {
                    bundle.putString(entry.getKey(), entry.getValue());
                }
            }
            if (appEnable != null) {
                bundle.putBoolean(RemoteCall.KEY_HOOK_APP_ENABLE, appEnable);
            }
            if (isActive != null) {
                bundle.putBoolean(RemoteCall.IS_ACTIVE.method, isActive);
            }
            if (errorString != null) {
                bundle.putString(RemoteCall.KEY_ERROR_DESCRIPTION, errorString);
            }
            if (syncFileName != null) {
                bundle.putString(RemoteCall.KEY_SYNC_FILE_NAME, syncFileName);
            }
            if (syncFileData != null) {
                bundle.putString(RemoteCall.KEY_SYNC_FILE_DATA, syncFileData);
            }
            if (syncFileBlock != null) {
                bundle.putInt(RemoteCall.KEY_SYNC_BLOCK_INDEX, syncFileBlock);
            }
            bundle.putInt(RemoteCall.KEY_CALL_RESULT, retCode);
            return bundle;
        }
    }

    public static class RemoteArgsUnpack {
        private final Bundle bundle;

        private RemoteArgsUnpack(Bundle caller) {
            this.bundle = caller;
        }

        public String getPackage() {
            return bundle.getString(RemoteCall.KEY_APP_PKG);
        }

        public String getPackageNonNull() throws IllegalArgumentException {
            String pkg = getPackage();
            if (TextUtils.isEmpty(pkg)) {
                throw new IllegalArgumentException("null package");
            }
            return pkg;
        }

        public boolean getBoolean() {
            return bundle.getBoolean(RemoteCall.KEY_COMMON_BOOLEAN);
        }

        public boolean enable() {
            return bundle.getBoolean(RemoteCall.KEY_HOOK_APP_ENABLE);
        }

        public String getString() {
            return bundle.getString(RemoteCall.KEY_COMMON_STRING);
        }

        public DataModelType getDataType() {
            return DataModelType.values()[bundle.getInt(RemoteCall.KEY_DATA_TYPE)];
        }

        public boolean success() {
            return bundle != null && bundle.getInt(RemoteCall.KEY_CALL_RESULT) == RemoteCall.RESULT_VALUE_CALL_SUCCESS;
        }

        public boolean exist() {
            return bundle != null;
        }

        public String error() {
            return bundle.getString(RemoteCall.KEY_ERROR_DESCRIPTION, "");
        }

        public String syncFileName() {
            String name = bundle.getString(RemoteCall.KEY_SYNC_FILE_NAME);
            if (TextUtils.isEmpty(name)) {
                throw new IllegalArgumentException(RemoteCall.KEY_SYNC_FILE_NAME + " parameter is empty.");
            }
            return name;
        }

        public String syncFileData() {
            return bundle.getString(RemoteCall.KEY_SYNC_FILE_DATA, "");
        }

        public int syncFileBlock() {
            return bundle.getInt(RemoteCall.KEY_SYNC_BLOCK_INDEX, -1);
        }

        public Bundle bundle() {
            return bundle;
        }
    }

}
