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

package com.sanfengandroid.xp.hooks;

import android.content.ContentProvider;
import android.os.Bundle;

import com.sanfengandroid.common.util.LogUtil;
import com.sanfengandroid.xp.ContentProviderAgent;
import com.sanfengandroid.xp.RemoteCall;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

/** 提供数据同步,暂时不使用,因为如果能Hook system_server则意味着拿的到root权限,通过root搬迁配置数据更加方便
 * @author sanfengAndroid
 */
public class HookSettingContentProvider implements IHook {
    private static final String TAG = HookSettingContentProvider.class.getSimpleName();


    @Override
    public void hook(ClassLoader loader) throws Throwable {
        XposedHelpers.findAndHookMethod("com.android.providers.settings.SettingsProvider", loader, "call", String.class, String.class, Bundle.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                String method = (String) param.args[0];
                if (!method.equals(RemoteCall.METHOD_HOOK_FLAG)) {
                    return;
                }
                LogUtil.d(TAG, "system provider accept flag: %s, method: %s", param.args[0], param.args[1]);
                RemoteCall remoteCall = RemoteCall.nameToRemoteCall((String) param.args[1]);
                ContentProvider provider = (ContentProvider) param.thisObject;
                param.setResult(ContentProviderAgent.getInstance().invoke(provider.getContext(), remoteCall, (Bundle) param.args[2]));
            }
        });
        LogUtil.v(TAG, "Hook system providers");
    }
}
