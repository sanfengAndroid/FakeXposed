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

import com.sanfengandroid.xp.SecureThrowable;

/**
 * @author sanfengAndroid
 * @date 2020/10/30
 */
public class XposedFilter implements IHook {
    public static final String TAG = XposedFilter.class.getSimpleName();
    private final IHook[] hooks;

    public XposedFilter() {
        hooks = new IHook[]{new HookDebugCheck(),
                new HookRuntime(), new HookSystemClass(), new HookSystemComponent(),
//                new HookSystemProperties(),
                // 放在最后避免拦截框架调用
                new HookThrowableStackElement(),
                new HookClassLoad(),
                new HookNativeMethodChecked()
        };
    }

    @Override
    public void hook(ClassLoader loader) throws Throwable {
        SecureThrowable.initStack();
        for (IHook hook : hooks) {
            hook.hook(loader);
        }
    }
}
