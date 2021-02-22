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

package com.sanfengandroid.common.util;

import android.os.Build;
import android.system.ErrnoException;
import android.system.OsConstants;
import android.system.StructStat;

import com.sanfengandroid.common.reflection.ReflectUtil;

import java.io.File;
import java.lang.reflect.Method;

/**
 * @author sanfengAndroid
 */
public class FileUtil {

    private static final String TAG = FileUtil.class.getSimpleName();
    private static Object LINUX = null;
    private static Method accessMethod, statMethod, getxattrMethod, setxattrMethod;

    static {
        try {
            String filedName = Build.VERSION.SDK_INT < Build.VERSION_CODES.O ? "os" : "rawOs";
            LINUX = ReflectUtil.getFieldStatic("libcore.io.Libcore", filedName);
            if (LINUX != null) {
                accessMethod = LINUX.getClass().getDeclaredMethod("access", String.class, int.class);
                statMethod = LINUX.getClass().getDeclaredMethod("stat", String.class);
                getxattrMethod = LINUX.getClass().getDeclaredMethod("getxattr", String.class, String.class);
                setxattrMethod = LINUX.getClass().getDeclaredMethod("setxattr", String.class, String.class, byte[].class, int.class);
            }
        } catch (Throwable e) {
            LogUtil.e(TAG, "get Libcore object failed.", e);
        }
    }

    public static boolean exist(String path) {
        return exist(new File(path));
    }

    public static boolean exist(File file) {
        if (accessMethod == null) {
            return file.exists();
        }
        try {
            // F_OK
            return (boolean) accessMethod.invoke(LINUX, file.getAbsolutePath(), 0);
        } catch (Exception e) {
            if (e instanceof ErrnoException) {
                ErrnoException err = (ErrnoException) e;
                if (err.errno == OsConstants.ENOENT) {
                    return false;
                }
            }
            LogUtil.e(TAG, "invoke access method failed.", e);
        }
        return false;
    }

    public static boolean isDirectory(String path) {
        return isDirectory(new File(path));
    }

    public static boolean isDirectory(StructStat stat) {
        if (stat == null) {
            return false;
        }
        return OsConstants.S_ISDIR(stat.st_mode);
    }

    public static boolean isDirectory(File file) {
        StructStat ss = getFileStat(file);
        return isDirectory(ss);
    }

    public static StructStat getFileStat(String path) {
        return getFileStat(new File(path));
    }

    public static StructStat getFileStat(File path) {
        if (statMethod == null) {
            return null;
        }
        try {
            return (StructStat) statMethod.invoke(LINUX, path.getAbsolutePath());
        } catch (Exception e) {
            if (e instanceof ErrnoException) {
                ErrnoException err = (ErrnoException) e;
                if (err.errno == OsConstants.ENOENT) {
                    return null;
                }
            }
            LogUtil.e(TAG, "invoke stat method failed.", e);
        }
        return null;
    }

    public static String getFileXattr(String path) {
        return getFileXattr(new File(path));
    }

    public static String getFileXattr(File file) {
        if (getxattrMethod == null) {
            return null;
        }
        try {
            return toJString((byte[]) getxattrMethod.invoke(LINUX, file.getAbsolutePath(), "security.selinux"));
        } catch (Exception e) {
            LogUtil.e(TAG, "invoke getxattr method failed.", e);
        }
        return null;
    }

    public static boolean setFileXattr(String path, String attr) {
        return setFileXattr(new File(path), attr);
    }

    public static boolean setFileXattr(File file, String attr) {
        if (setxattrMethod == null) {
            return false;
        }
        try {
            setxattrMethod.invoke(LINUX, file.getAbsolutePath(), "security.selinux", toCString(attr), 1);
            return true;
        } catch (Exception e) {
            LogUtil.e(TAG, "invoke setxattr method failed.", e);
        }
        return false;
    }


    private static byte[] toCString(String s) {
        if (s == null) {
            return new byte[]{0};
        }
        byte[] bytes = s.getBytes();
        byte[] result = new byte[bytes.length + 1];
        System.arraycopy(bytes, 0,
                result, 0,
                bytes.length);
        result[result.length - 1] = (byte) 0;
        return result;
    }

    private static String toJString(byte[] bytes) {
        if (bytes != null && bytes.length > 0) {
            return new String(bytes, 0, bytes.length - 1);
        }
        return null;
    }
}
