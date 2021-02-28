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

import com.sanfengandroid.common.bean.ExecBean;
import com.sanfengandroid.common.model.FileAccessModel;
import com.sanfengandroid.common.model.MapsRuleModel;
import com.sanfengandroid.common.model.base.DataModelType;
import com.sanfengandroid.common.util.LogUtil;
import com.sanfengandroid.fakexposed.BuildConfig;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class NativeInit {
    private static final String TAG = NativeInit.class.getSimpleName();

    public static void initNative(Context context, String process) {
        try {
            NativeHook.initFakeLinker(context.getCacheDir().getAbsolutePath(), process);
            nativeSync();
            if (BuildConfig.DEBUG) {
                NativeHook.openJniMonitor();
            }
        } catch (Throwable e) {
            LogUtil.e(TAG, "native init error", e);
        }
    }

    public static void startNative() {
        LogUtil.v(TAG, "native init result: %s", NativeHook.startNativeHook().code);
    }

    public static void nativeSync() {
        NativeHook.clearAll();
        addNativeBlacklist(DataModelType.FILE_HIDE);
        addNativeBlacklist(DataModelType.SYMBOL_HIDE);
        addNativeStringOption(DataModelType.GLOBAL_SYSTEM_PROPERTY_HIDE);
        addNativeStringOption(DataModelType.LOAD_CLASS_HIDE);
        addNativeStringOption(DataModelType.STACK_ELEMENT_HIDE);
        addNativeRuntime();
//        addNativeStringOption(DataModelType.SYSTEM_ENV_HIDE);
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

    private static void addNativeStringOption(DataModelType type) {
        NativeOption.NativeStringOption option = null;
        switch (type){
            case GLOBAL_SYSTEM_PROPERTY_HIDE:
                option = NativeOption.NativeStringOption.SYSTEM_PROPERTY;
                break;
            case SYSTEM_ENV_HIDE:
                option = NativeOption.NativeStringOption.ENVIRONMENT;
                break;
            case STACK_ELEMENT_HIDE:
                option = NativeOption.NativeStringOption.STACK_CLASS_BLACKLIST;
                break;
            case LOAD_CLASS_HIDE:
                option = NativeOption.NativeStringOption.CLASS_BLACKLIST;
                break;
            default:
                break;
        }
        if (option == null) {
            return;
        }
        Map<String, String> map = GlobalConfig.getMap(type, String.class);
        if (map.isEmpty()) {
            return;
        }

        for (Map.Entry<String, String> entry : map.entrySet()) {
            LogUtil.v(TAG, "add native %s string option name: %s, value: %s, result: %s", type.name(), entry.getKey(), entry.getValue(),
                    NativeHook.addBlackList(option, entry.getKey(), entry.getValue()));
        }
    }

    private static void addNativeRuntime() {
        Map<String, List<ExecBean>> map = (Map<String, List<ExecBean>>) GlobalConfig.getMap(DataModelType.RUNTIME_EXEC_HIDE);
        for (Map.Entry<String, List<ExecBean>> entry : map.entrySet()) {
            for (ExecBean bean : entry.getValue()) {
                LogUtil.v(TAG, "add runtime option result: %s",
                        NativeHook.addRuntimeBlacklist(bean.oldCmd, bean.newCmd, bean.oldArgs, bean.matchArgv,
                                bean.newArgs, bean.replaceArgv, bean.outStream, bean.inputStream, bean.errStream));
            }
        }
    }
}
