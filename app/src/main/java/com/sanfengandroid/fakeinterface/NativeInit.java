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

import com.sanfengandroid.common.model.FileAccessModel;
import com.sanfengandroid.common.model.MapsRuleModel;
import com.sanfengandroid.common.model.base.DataModelType;
import com.sanfengandroid.common.util.LogUtil;
import com.sanfengandroid.fakexposed.BuildConfig;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

public class NativeInit {
    private static final String TAG = NativeInit.class.getSimpleName();

    public static void initNative(Context context, String process) {
        try {
            NativeHook.initFakeLinker(context.getCacheDir().getAbsolutePath(), process);
            nativeSync();
            if (BuildConfig.DEBUG){
                NativeHook.openJniMonitor();
            }
            LogUtil.v(TAG, "native init result: %s", NativeHook.startNativeHook().code);
        } catch (Throwable e) {
            LogUtil.e(TAG, "native init error", e);
        }
    }

    public static void nativeSync(){
        NativeHook.clearAll();
        addNativeBlacklist(DataModelType.FILE_HIDE);
        addNativeBlacklist(DataModelType.SYMBOL_HIDE);
        addNativeStringOption(DataModelType.GLOBAL_SYSTEM_PROPERTY_HIDE);
        Map<String, String> map = GlobalConfig.getMap(DataModelType.MAPS_HIDE, String.class);
        for (Map.Entry<String, String> entry : map.entrySet()) {
            MapsRuleModel model = new MapsRuleModel();
            model.setKey(entry.getKey());
            model.setValue(entry.getValue());
            LogUtil.v(TAG, "add native maps rule: %s, result: %s", model, NativeHook.addMapsRule(model.getMode(), model.getKey(), model.getRule()));
        }
        for (Map.Entry<String, String> entry : GlobalConfig.getMap(DataModelType.FILE_REDIRECT_HIDE, String.class).entrySet()) {
            LogUtil.v(TAG, "add file redirect path src: %s, dst: %s, result: %s", entry.getKey(), entry.getValue(),
                    NativeHook.addRedirectFile(entry.getKey(), entry.getValue()));
        }
        for (Map.Entry<String, String> entry : GlobalConfig.getMap(DataModelType.FILE_ACCESS_HIDE, String.class).entrySet()) {
            FileAccessModel model = new FileAccessModel();
            model.setValue(entry.getValue());
            LogUtil.v(TAG, "add file access control path: %s, uid: %d, gid: %d, access: %s, result: %s", entry.getKey(), model.getUid(),
                    model.getGid(), Integer.toOctalString(model.getAccess()),
                    NativeHook.setFilePermission(entry.getKey(), model.getUid(), model.getGid(), model.getAccess()));
        }
    }

    private static void addNativeBlacklist(DataModelType type) {
        Collection<String> list = GlobalConfig.getBlackListValue(type, String.class);
        for (String s : list) {
            LogUtil.v(TAG, "native filter type %s, keyword: %s", type.name(), s);
        }
        if (!list.isEmpty()) {
            NativeHookStatus[] options = new NativeHookStatus[list.size()];
            Arrays.fill(options, NativeHookStatus.OPEN);
            NativeOption.NativeIntOption option = null;
            switch (type) {
                case FILE_HIDE:
                    option = NativeOption.NativeIntOption.FILE_BLACKLIST;
                    break;
                case SYMBOL_HIDE:
                    option = NativeOption.NativeIntOption.SYMBOL_BLACKLIST;
                    break;
                default:
                    break;
            }
            LogUtil.v(TAG, "add native %s blacklist result: %s", type.name(), NativeHook.addBlackLists(option, list.toArray(new String[0]), options));
        }
    }

    private static void addNativeStringOption(DataModelType type){
        Map<String,String> map = GlobalConfig.getMap(type, String.class);
        if (map.isEmpty()){
            return;
        }
        if (type == DataModelType.GLOBAL_SYSTEM_PROPERTY_HIDE){
            for (Map.Entry<String,String> entry : map.entrySet()){
                LogUtil.v(TAG, "add native %s string option name: %s, value: %s, result: %s", type.name(), entry.getKey(), entry.getValue(),
                        NativeHook.addBlackList(NativeOption.NativeStringOption.SYSTEM_PROPERTY, entry.getKey(), entry.getValue()));
            }
        }
    }
}
