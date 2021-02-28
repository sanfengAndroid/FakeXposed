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

/**
 * 对应native层几种黑名单
 *
 * @author sanfengAndroid
 */
public class NativeOption {

    public enum NativeIntOption {
        /**
         * 文件黑名单
         */
        FILE_BLACKLIST,
        /**
         * 符号黑名单
         */
        SYMBOL_BLACKLIST,
        /**
         * maps文件规则
         */
        MAPS_RULE
    }

    public enum NativeStringOption {
        /**
         * 全局系统属性
         */
        SYSTEM_PROPERTY,

        /**
         * 类黑名单
         */
        CLASS_BLACKLIST,

        /**
         * 堆栈类黑名单
         */
        STACK_CLASS_BLACKLIST,
        /**
         * 环境变量
         */
        ENVIRONMENT,

    }
}
