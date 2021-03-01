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

package com.sanfengandroid.datafilter.listener;

import java.util.UUID;

/**
 * @author sanfengAndroid
 */
public class AppFilterable {
    public static final String SYSTEM_MASK = UUID.randomUUID().toString();
    public static final String SYSTEM_UNMASK = UUID.randomUUID().toString();
    public static final String XPOSED_MASK = UUID.randomUUID().toString();
    public static final String XPOSED_UNMASK = UUID.randomUUID().toString();
    public static final String ALL = UUID.randomUUID().toString();

    public enum Option {
        /**
         * 显示所有
         */
        ALL,
        /**
         * 显示匹配项
         */
        THIS,
        /**
         * 显示非匹配项
         */
        OTHER
    }

    public interface SystemAppFilter {
        /**
         * @param filter
         */
        void filterSystemApp(Option filter);
    }

    public interface XposedAppFilter {
        /**
         * @param option 过滤选项
         */
        void filterXposedApp(Option option);
    }
}
