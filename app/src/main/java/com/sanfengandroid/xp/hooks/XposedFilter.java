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

import com.sanfengandroid.common.util.LogUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * @author sanfengAndroid
 * @date 2020/10/30
 */
public class XposedFilter implements IHook {
    public static final String TAG = XposedFilter.class.getSimpleName();
    private final List<IHook> hooks = new ArrayList<>();

    public XposedFilter() {
        hooks.add(new HookDebugCheck());
        hooks.add(new HookSystemClass());
        hooks.add(new HookSystemComponent());
        hooks.add(new HookClassLoad());
        hooks.add(new HookNativeMethodChecked());
    }

    @Override
    public void hook(ClassLoader loader) {
        for (IHook hook : hooks) {
            try {
                hook.hook(loader);
            } catch (Throwable e) {
                LogUtil.e(TAG, "Hook java error", e);
            }
        }
    }
}
