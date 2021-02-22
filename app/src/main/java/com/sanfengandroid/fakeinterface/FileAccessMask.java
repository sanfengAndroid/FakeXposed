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
 * @author sanfengAndroid
 */

public enum FileAccessMask {
    /**
     * 文件权限
     */
    SET_UID(04000),
    SET_GID(02000),
    SET_TX(01000),
    USR_READ(0400),
    USR_WRITE(0200),
    USR_EXEC(0100),
    GRP_READ(040),
    GRP_WRITE(020),
    GRP_EXEC(010),
    OTH_READ(04),
    OTH_WRITE(02),
    OTH_EXEC(01),
    ONLY_400(0400),
    ONLY_644(0644),
    ONLY_744(0744),
    ONLY_777(0777),
    ONLY_700(0700);

    public static final int MASK = 07777;
    public final int mode;

    FileAccessMask(int i) {
        mode = i;
    }
}
