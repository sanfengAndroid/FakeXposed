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

package com.sanfengandroid.common;

import android.net.Uri;

import com.sanfengandroid.datafilter.BuildConfig;

public class Const {
    public static final String GLOBAL_PACKAGE = "all";
    public static final int JAVA_MONITOR_STATE = 1;
    private static final String XP_REMOTE_PATH = "xp";
    public static final Uri XP_CONTENT_URI = Uri.parse("content://" + BuildConfig.APPLICATION_ID + "/" + XP_REMOTE_PATH);

}
