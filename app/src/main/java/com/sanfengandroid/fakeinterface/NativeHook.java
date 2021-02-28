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
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import com.sanfengandroid.common.reflection.ReflectUtil;
import com.sanfengandroid.common.util.LogUtil;
import com.sanfengandroid.common.util.Util;
import com.sanfengandroid.fakelinker.BuildConfig;
import com.sanfengandroid.fakelinker.ErrorCode;
import com.sanfengandroid.fakelinker.FakeLinker;
import com.sanfengandroid.fakelinker.FileInstaller;

import java.io.File;

/**
 * Native Hook入口
 * 1. 先调用{@link #initFakeLinker}或{@link #defaultInitFakeLinker(Context)}初始化模块路径和缓存路径
 * 2. 调用 XXX设置各种配置
 * 3. 调用{@link #startNativeHook()}开启NativeHook
 */
@SuppressWarnings("JavaJniMissingFunction")
public final class NativeHook {
    private static final String TAG = NativeHook.class.getSimpleName();
    /**
     * Hook配置的默认保存路径
     */
    private static String configPath = "/data/system/sanfengandroid/fakexposed";
    private static String libraryPath = null;

    private static native int nativeAddIntBlackList(int type, String name, int value, boolean add);

    private static native int nativeAddIntBlackLists(int type, String[] names, int[] values, boolean add);

    private static native int nativeAddStringBlackList(int type, String name, String value, boolean add);

    private static native int nativeSetHookOptionInt(String name, int value);

    private static native int nativeSetHookOptionString(String name, String value);

    private static native void nativeClearBlackList(int type);

    private static native void nativeClearAll();

    public static native void nativeTest();

    public static native void nativeTest1();

    private static native int nativeStartHook();

    private static native boolean nativeSetRedirectFile(String src, String redirect, boolean dir);

    private static native void nativeRemoveRedirectFile(String src, boolean dir);

    private static native boolean nativeSetFileAccess(String path, int uid, int gid, int access);

    private static native void nativeRemoveFileAccess(String path);

    private static native int nativeRelinkSpecialLibrary(String libraryName);

    private static native int nativeOpenJniMonitor(boolean open);

    private static native int nativeSetJniMonitorLib(String name, boolean contain, boolean add);

    private static native int nativeSetJniMonitorAddress(long start, long end, boolean contain, boolean add);

    private static native int nativeSetRuntimeBlacklist(String oldCmd, String newCmd, String[] oldArgv, boolean matchArgv, String[] newArgv,
                                                        boolean replaceArgv, String input, String output, String error);

    public static ErrorCode openJniMonitor() {
        try {
            return ErrorCode.codeToError(nativeOpenJniMonitor(true));
        } catch (Throwable e) {
            Log.e(TAG, "open jni monitor error: " + e.getMessage(), e);
            return ErrorCode.ERROR_JAVA_EXECUTE;
        }
    }

    public static ErrorCode setJniMonitorLib(String name, boolean contain, boolean add) {
        try {
            if (TextUtils.isEmpty(name)) {
                return ErrorCode.ERROR_JAVA_EXECUTE;
            }
            return ErrorCode.codeToError(nativeSetJniMonitorLib(name, contain, add));
        } catch (Throwable e) {
            Log.e(TAG, "set jni monitor library error: " + e.getMessage(), e);
            return ErrorCode.ERROR_JAVA_EXECUTE;
        }
    }

    public static ErrorCode clearAll() {
        try {
            nativeClearAll();
            return ErrorCode.ERROR_NO;
        } catch (Throwable e) {
            Log.e(TAG, "clear all native data failed: " + e.getMessage(), e);
            return ErrorCode.ERROR_JAVA_EXECUTE;
        }
    }

    public static ErrorCode setJniMonitorAddress(long start, long end, boolean contain, boolean add) {
        try {
            if (end < start) {
                return ErrorCode.ERROR_JAVA_EXECUTE;
            }
            return ErrorCode.codeToError(nativeSetJniMonitorAddress(start, end, contain, add));
        } catch (Throwable e) {
            Log.e(TAG, "set jni monitor address error: " + e.getMessage(), e);
            return ErrorCode.ERROR_JAVA_EXECUTE;
        }
    }

    /**
     * @param libraryName 库soname名称
     * @return 调用结果
     */
    public static ErrorCode relinkLibrary(String libraryName) {
        try {
            if (libraryName == null || libraryName.isEmpty()) {
                return ErrorCode.ERROR_JAVA_EXECUTE;
            }
            return ErrorCode.codeToError(nativeRelinkSpecialLibrary(libraryName));
        } catch (Throwable e) {
            Log.e(TAG, "relink library error: " + libraryName, e);
            return ErrorCode.ERROR_JAVA_EXECUTE;
        }
    }

    private static ErrorCode callIntBlacklist(NativeOption.NativeIntOption option, String name, int value, boolean add) {
        if (option == null || TextUtils.isEmpty(name)) {
            return ErrorCode.ERROR_JAVA_EXECUTE;
        }
        try {
            return ErrorCode.codeToError(nativeAddIntBlackList(option.ordinal(), name, value, add));
        } catch (Throwable e) {
            e.printStackTrace();
            return ErrorCode.ERROR_JAVA_EXECUTE;
        }
    }

    private static ErrorCode callStringBlacklist(NativeOption.NativeStringOption option, String name, String value, boolean add) {
        if (option == null || TextUtils.isEmpty(name)) {
            return ErrorCode.ERROR_JAVA_EXECUTE;
        }
        try {
            return ErrorCode.codeToError(nativeAddStringBlackList(option.ordinal(), name, value, add));
        } catch (Throwable e) {
            e.printStackTrace();
            return ErrorCode.ERROR_JAVA_EXECUTE;
        }
    }

    private static ErrorCode callMapsBlackList(String name, int value, boolean add) {
        if (TextUtils.isEmpty(name)) {
            return ErrorCode.ERROR_JAVA_EXECUTE;
        }
        return callIntBlacklist(NativeOption.NativeIntOption.MAPS_RULE, name, value, add);
    }

    public static ErrorCode addBlackList(NativeOption.NativeIntOption option, String name, NativeHookStatus value) {
        if (value == null) {
            return ErrorCode.ERROR_JAVA_EXECUTE;
        }
        return callIntBlacklist(option, name, value.getOption(), true);
    }

    /**
     * @param option 黑名单类型
     * @param name   名字,传null则删除该类型下所有黑名单
     * @param value  字符串值
     */
    public static ErrorCode addBlackList(NativeOption.NativeStringOption option, String name, String value) {
        return callStringBlacklist(option, name, value, true);
    }

    public static ErrorCode removeBlackList(NativeOption.NativeIntOption option, String name) {
        return callIntBlacklist(option, name, 0, false);
    }

    public static ErrorCode removeBlackList(NativeOption.NativeStringOption option, String name) {
        return callStringBlacklist(option, name, "", false);
    }

    /**
     * @param option 黑名单类型
     * @param names  名字数组
     * @param values 值选项,开启/关闭
     * @return 添加黑名单是否成功
     */
    public static ErrorCode addBlackLists(NativeOption.NativeIntOption option, String[] names, NativeHookStatus[] values) {
        if (option == null) {
            return ErrorCode.ERROR_JAVA_EXECUTE;
        }
        if ((names == null || names.length == 0) || (values == null || values.length == 0)) {
            LogUtil.e(TAG, "warning: blacklist is empty,ignore this operation.");
            return ErrorCode.ERROR_JAVA_EXECUTE;
        }
        if (names.length != values.length) {
            LogUtil.e(TAG, "Key value cannot be one-to-one correspondence, key length: %d value length: %d", names.length, values.length);
            return ErrorCode.ERROR_JAVA_EXECUTE;
        }
        for (int i = 0; i < names.length; i++) {
            if (names[i] == null || names[i].isEmpty()) {
                LogUtil.e(TAG, "Contains empty keys and cannot continue, key index: %d", i);
                return ErrorCode.ERROR_JAVA_EXECUTE;
            }
        }
        int[] intValues = new int[values.length];
        for (int i = 0; i < values.length; i++) {
            if (values[i] == null) {
                LogUtil.e(TAG, "Contains empty value and cannot continue, value index: %d", i);
                return ErrorCode.ERROR_JAVA_EXECUTE;
            }
            intValues[i] = values[i].getOption();
        }
        try {
            return ErrorCode.codeToError(nativeAddIntBlackLists(option.ordinal(), names, intValues, true));
        } catch (Throwable e) {
            e.printStackTrace();
            return ErrorCode.ERROR_JAVA_EXECUTE;
        }
    }

    public static ErrorCode removeBlackLists(NativeOption.NativeIntOption option, String[] names) {
        if (option == null || names == null || names.length == 0) {
            return ErrorCode.ERROR_JAVA_EXECUTE;
        }
        for (int i = 0; i < names.length; i++) {
            if (TextUtils.isEmpty(names[i])) {
                LogUtil.e(TAG, "Contains empty keys and cannot continue, key index: %d", i);
                return ErrorCode.ERROR_JAVA_EXECUTE;
            }
        }
        try {
            return ErrorCode.codeToError(nativeAddIntBlackLists(option.ordinal(), names, null, false));
        } catch (Throwable e) {
            e.printStackTrace();
            return ErrorCode.ERROR_JAVA_EXECUTE;
        }
    }

    public static ErrorCode addRuntimeBlacklist(String oldCmd, String newCmd, String[] oldArgv, boolean matchArgv,
                                                String[] newArgv, boolean replaceArgv, String input, String output, String error) {
        if (TextUtils.isEmpty(oldCmd)) {
            return ErrorCode.ERROR_JAVA_EXECUTE;
        }
        if (TextUtils.equals(oldCmd, newCmd) || TextUtils.isEmpty(newCmd)) {
            newCmd = null;
        }
        if (matchArgv) {
            if (oldArgv == null || oldArgv.length == 0) {
                oldArgv = null;
            } else {
                for (String argv : oldArgv) {
                    if (TextUtils.isEmpty(argv)) {
                        return ErrorCode.ERROR_JAVA_EXECUTE;
                    }
                }
            }
        } else {
            oldArgv = null;
        }
        if (replaceArgv) {
            if (newArgv == null || newArgv.length == 0) {
                newArgv = null;
            } else {
                for (String argv : newArgv) {
                    if (TextUtils.isEmpty(argv)) {
                        return ErrorCode.ERROR_JAVA_EXECUTE;
                    }
                }
            }
        } else {
            newArgv = null;
        }
        if (TextUtils.isEmpty(input)) {
            input = null;
        }
        if (TextUtils.isEmpty(output)) {
            output = null;
        }
        if (TextUtils.isEmpty(error)) {
            error = null;
        }
        try {
            return ErrorCode.codeToError(nativeSetRuntimeBlacklist(oldCmd, newCmd, oldArgv, matchArgv, newArgv, replaceArgv, input, output, error));
        } catch (Throwable e) {
            e.printStackTrace();
            return ErrorCode.ERROR_JAVA_EXECUTE;
        }
    }

    /**
     * 添加/删除maps文件黑名单
     *
     * @param value 关键词,当模式为修改时格式为: oldValue?newValue
     */
    public static ErrorCode addMapsRule(String value) {
        if (TextUtils.isEmpty(value)) {
            return ErrorCode.ERROR_JAVA_EXECUTE;
        }
        return callMapsBlackList(value, MapsMode.MM_REMOVE.ordinal(), true);
    }

    public static ErrorCode addMapsRule(MapsMode mode, String key, String value) {
        if (TextUtils.isEmpty(key)) {
            return ErrorCode.ERROR_JAVA_EXECUTE;
        }
        if (mode == MapsMode.MM_REMOVE) {
            return addMapsRule(key);
        }
        if (mode == MapsMode.MM_MODIFY) {
            return addMapsRule(key, value);
        }
        return ErrorCode.ERROR_JAVA_EXECUTE;
    }

    public static ErrorCode addMapsRule(String oldValue, String newValue) {
        if (TextUtils.isEmpty(oldValue)) {
            return ErrorCode.ERROR_JAVA_EXECUTE;
        }
        String value = "m " + oldValue + "?" + (newValue == null ? "" : newValue);
        return callMapsBlackList(value, MapsMode.MM_MODIFY.ordinal(), true);
    }

    public static ErrorCode removeMapsRule(String value) {
        if (TextUtils.isEmpty(value)) {
            return ErrorCode.ERROR_JAVA_EXECUTE;
        }
        return callMapsBlackList("r " + value, MapsMode.MM_REMOVE.ordinal(), false);
    }

    public static ErrorCode removeMapsRule(String oldValue, String newValue) {
        if (TextUtils.isEmpty(oldValue)) {
            return ErrorCode.ERROR_JAVA_EXECUTE;
        }
        String value = "m " + oldValue + "?" + (newValue == null ? "" : newValue);
        return callMapsBlackList(value, MapsMode.MM_MODIFY.ordinal(), false);
    }

    public static ErrorCode setHookOption(NativeConfig option, NativeHookStatus value) {
        if (option == null || value == null) {
            return ErrorCode.ERROR_JAVA_EXECUTE;
        }
        try {
            return ErrorCode.codeToError(nativeSetHookOptionInt(option.name, value.getOption()));
        } catch (Throwable e) {
            e.printStackTrace();
            return ErrorCode.ERROR_JAVA_EXECUTE;
        }

    }

    public static ErrorCode setHookOption(NativeConfig option, String value) {
        if (option == null) {
            return ErrorCode.ERROR_JAVA_EXECUTE;
        }
        try {
            return ErrorCode.codeToError(nativeSetHookOptionString(option.name, TextUtils.isEmpty(value) ? "" : value));
        } catch (Throwable e) {
            e.printStackTrace();
            return ErrorCode.ERROR_JAVA_EXECUTE;
        }
    }

    public static void clearHook(NativeOption.NativeIntOption option) {
        if (option == null) {
            return;
        }
        try {
            nativeClearBlackList(option.ordinal());
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static ErrorCode startNativeHook() {
        try {
            return ErrorCode.codeToError(nativeStartHook());
        } catch (Throwable e) {
            e.printStackTrace();
            return ErrorCode.ERROR_JAVA_EXECUTE;
        }
    }


    /**
     * 由于文件权限的问题无法正确检测是文件或目录,因此根据最后的结束符判断
     *
     * @param src      原路径
     * @param redirect 重定向后的路径
     * @return 成功返回true
     */
    public static boolean addRedirectFile(String src, String redirect) {
        if (TextUtils.isEmpty(src) || TextUtils.isEmpty(redirect)) {
            return false;
        }
        return addRedirectFile(new File(src), new File(redirect), src.endsWith("/"));
    }

    /**
     * 由于权限问题,可能无法判断文件/目录,可以调用{@link #addRedirectFile(File, File, boolean)}或{@link #addRedirectFile(String, String)}代替
     * 没有权限时则会认为是文件
     */
    public static boolean addRedirectFile(File src, File redirect) {
        if (src == null || redirect == null) {
            return false;
        }
        if (src.isDirectory() != redirect.isDirectory()) {
            return false;
        }
        try {
            return nativeSetRedirectFile(src.getAbsolutePath(), redirect.getAbsolutePath(), src.isDirectory());
        } catch (Throwable e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean addRedirectFile(File src, File redirect, boolean dir) {
        try {
            return nativeSetRedirectFile(src.getAbsolutePath(), redirect.getAbsolutePath(), dir);
        } catch (Throwable e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void removeRedirectFile(String src) {
        if (TextUtils.isEmpty(src)) {
            return;
        }
        removeRedirectFile(new File(src));
    }

    public static void removeRedirectFile(File src) {
        if (src == null) {
            return;
        }
        try {
            nativeRemoveRedirectFile(src.getAbsolutePath(), src.isDirectory());
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static boolean setFilePermission(String path, int uid, int gid, int access) {
        if (TextUtils.isEmpty(path)) {
            return false;
        }
        return setFilePermission(new File(path), uid, gid, access);
    }

    public static boolean setFilePermission(String path, int uid, int gid, FileAccessMask access) {
        if (TextUtils.isEmpty(path)) {
            return false;
        }
        return setFilePermission(new File(path), uid, gid, access.mode);
    }

    public static boolean setFilePermission(File file, int uid, int gid, int access) {
        if (file == null) {
            return false;
        }
        try {
            return nativeSetFileAccess(file.getAbsolutePath(), uid, gid, access & FileAccessMask.MASK);
        } catch (Throwable e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void removeFilePermission(File file) {
        if (file == null) {
            return;
        }
        try {
            nativeRemoveFileAccess(file.getAbsolutePath());
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static void initLibraryPath(Context context) throws PackageManager.NameNotFoundException {
        if (libraryPath != null) {
            return;
        }
        ApplicationInfo info = context.getPackageManager().getApplicationInfo(com.sanfengandroid.fakexposed.BuildConfig.APPLICATION_ID, 0);
        libraryPath = info.nativeLibraryDir;
    }

    public static void initLibraryPath(Context context, int targetSdk) throws PackageManager.NameNotFoundException {
        if (targetSdk < Build.VERSION_CODES.R || Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            initLibraryPath(context);
            return;
        }
        try {
            libraryPath = findLibraryPath();
            LogUtil.d(TAG, "find library at: %s", libraryPath);
        } catch (Exception e) {
            LogUtil.e(TAG, "find library error", e);
            initLibraryPath(context);
        }
    }

    private static String findLibraryPath() throws Exception {
        ClassLoader loader = NativeHook.class.getClassLoader();
        Object pathList = ReflectUtil.getField(loader.getClass().getSuperclass(), loader, "pathList");
        Object[] dexElements = (Object[]) ReflectUtil.getFieldInstance(pathList, "dexElements");
        File apkPath = null;
        for (Object element : dexElements) {
            File path = (File) ReflectUtil.getFieldInstance(element, "path");
            if (path == null) {
                continue;
            }
            if (path.getName().endsWith(".apk") && path.getAbsolutePath().contains(com.sanfengandroid.fakexposed.BuildConfig.APPLICATION_ID)) {
                apkPath = path;
                break;
            }
        }
        File base = new File(apkPath.getParent(), "lib");
        File library = null;
        if (FileInstaller.isRunning64Bit()) {
            library = new File(base, FileInstaller.isX86() ? "x86_64" : "arm64");
            if (!library.exists() || !library.isDirectory()) {
                library = null;
            }
        }
        if (library == null) {
            library = new File(base, FileInstaller.isX86() ? "x86" : "arm");
        }
        return library.getAbsolutePath();
    }

    public static String getDefaultHookModulePath() {
        String name = "lib" + (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ? com.sanfengandroid.fakexposed.BuildConfig.HOOK_HIGH_MODULE_NAME : com.sanfengandroid.fakexposed.BuildConfig.HOOK_LOW_MODULE_NAME);
        return new File(libraryPath, FileInstaller.isRunning64Bit() ? name + "64.so" : name + ".so").getAbsolutePath();
    }

    public static String getConfigPath() {
        return configPath;
    }

    public static void setConfigPath(String configPath) {
        NativeHook.configPath = new File(configPath).getAbsolutePath();
    }

    public static String getDefaultFakeLinkerPath() {
        String name = "lib" + BuildConfig.LINKER_MODULE_NAME + "-" + (Build.VERSION.SDK_INT == Build.VERSION_CODES.N_MR1 ? Build.VERSION_CODES.N : Build.VERSION.SDK_INT);

        return new File(libraryPath, FileInstaller.isRunning64Bit() ? name + "64.so" : name + ".so").getAbsolutePath();
    }

    public static void defaultInitFakeLinker(Context context) {
        try {
            initLibraryPath(context);
        } catch (PackageManager.NameNotFoundException ignore) {
        }
        FakeLinker.initFakeLinker(getDefaultFakeLinkerPath(), getDefaultHookModulePath(), context.getCacheDir().getAbsolutePath(), getConfigPath(), Util.getProcessName(context));
    }

    public static void initFakeLinker(String cacheDir, String processName) {
        FakeLinker.initFakeLinker(getDefaultFakeLinkerPath(), getDefaultHookModulePath(), cacheDir, getConfigPath(), processName);
    }
}
