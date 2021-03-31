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

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.pm.PackageInfo;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.content.pm.ParceledListSlice;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.VersionedPackage;
import android.os.Build;

import com.sanfengandroid.common.Const;
import com.sanfengandroid.common.model.base.DataModelType;
import com.sanfengandroid.common.reflection.ReflectUtil;
import com.sanfengandroid.common.util.LogUtil;
import com.sanfengandroid.fakeinterface.GlobalConfig;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class HookSystemComponent implements IHook {
    private static final String TAG = HookSystemComponent.class.getSimpleName();

    @Override
    public void hook(ClassLoader loader) throws Throwable {
        shieldPackageManager();
        shieldActivityManager();
        shieldActivityTaskManager();
    }

    @SuppressLint("PrivateApi")
    private void shieldPackageManager() throws Exception {
        Object oldPm = ReflectUtil.invoke("android.app.ActivityThread", null, "getPackageManager");
        Class<?> ipm = Class.forName("android.content.pm.IPackageManager");
        Object newPm = Proxy.newProxyInstance(ipm.getClassLoader(), new Class[]{ipm}, new InvocationHandler() {
            private Object orig;

            public InvocationHandler set(Object orig) {
                this.orig = orig;
                return this;
            }

            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                Object result = method.invoke(orig, args);
                if (GlobalConfig.isEmptyBlacklist(DataModelType.PACKAGE_HIDE) || result == null) {
                    return result;
                }
                try {
                    DataModelType type = DataModelType.PACKAGE_HIDE;
                    switch (method.getName().hashCode()) {
                        case -150905391:        // getInstalledPackages
                        case -232294012:        // getPackagesHoldingPermissions
                            Iterator<PackageInfo> piIt = ((ParceledListSlice<PackageInfo>) result).getList().iterator();
                            while (piIt.hasNext()) {
                                PackageInfo info = piIt.next();
                                if (GlobalConfig.stringEqualBlacklist(info.packageName, type)) {
                                    LogUtil.w(Const.JAVA_MONITOR_STATE, "hide filter PackageInfo : %s", info.packageName);
                                    piIt.remove();
                                }
                            }
                            break;
                        case 1700882705:            // queryContentProviders
                        case 964371846:             // queryInstrumentation
                        case 1600494599:            // getInstalledApplications
                            Iterator<PackageItemInfo> piiIt = (Iterator<PackageItemInfo>) ((ParceledListSlice<?>) result).getList().iterator();
                            while (piiIt.hasNext()) {
                                PackageItemInfo info = piiIt.next();
                                if (GlobalConfig.stringEqualBlacklist(info.packageName, type)) {
                                    LogUtil.w(Const.JAVA_MONITOR_STATE, "hide filter %s : %s", info.getClass(), info);
                                    piiIt.remove();
                                }
                            }
                            break;
                        case 1374193809:                    // queryIntentActivities
                        case -1655019925:                   // queryIntentActivityOptions
                        case 1786110784:                    // queryIntentReceivers
                        case -109758974:                    // queryIntentServices
                        case -1530208819:                   // queryIntentContentProviders
                            Iterator<ResolveInfo> riIt;
                            if (result instanceof List) {
                                riIt = ((List) result).iterator();
                            } else {
                                riIt = ((ParceledListSlice<ResolveInfo>) result).getList().iterator();
                            }
                            while (riIt.hasNext()) {
                                ResolveInfo info = riIt.next();
                                if (GlobalConfig.stringEqualBlacklist(info.resolvePackageName, type)) {
                                    LogUtil.w(Const.JAVA_MONITOR_STATE, "hide filter ResolveInfo : %s", info);
                                    riIt.remove();
                                    continue;
                                }
                                if (info.activityInfo != null) {
                                    if (GlobalConfig.stringEqualBlacklist(info.activityInfo.packageName, type)) {
                                        LogUtil.w(Const.JAVA_MONITOR_STATE, "hide filter ResolveInfo(ActivityInfo) : %s", info);
                                        riIt.remove();
                                        continue;
                                    }
                                }
                                if (info.serviceInfo != null) {
                                    if (GlobalConfig.stringEqualBlacklist(info.serviceInfo.packageName, type)) {
                                        LogUtil.w(Const.JAVA_MONITOR_STATE, "hide filter ResolveInfo(ServiceInfo) : %s", info);
                                        riIt.remove();
                                        continue;
                                    }
                                }
                                if (info.providerInfo != null) {
                                    if (GlobalConfig.stringEqualBlacklist(info.providerInfo.packageName, type)) {
                                        LogUtil.w(Const.JAVA_MONITOR_STATE, "hide filter ResolveInfo(ProviderInfo) : %s", info);
                                        riIt.remove();
                                        continue;
                                    }
                                }
                            }
                            break;
                        case 268353758:                     // getPackageInfo
                        case 268289313:                     // getPackageGids
                        case -1710913560:                   // getApplicationInfo
                        case -1280559169:                   // getInstallerPackageName
                            if (GlobalConfig.stringEqualBlacklist((String) args[0], type)) {
                                result = null;
                            }
                            break;
                        case -268426720:                    // getPackageUid
                            if (GlobalConfig.stringEqualBlacklist((String) args[0], type)) {
                                result = -1;
                            }
                            break;
                        case -1039974701:                   // getActivityInfo
                        case 871155123:                     // getReceiverInfo
                        case 1725989837:                    // getServiceInfo
                        case 1207111861:                    // getProviderInfo
                        case -760840698:                    // getInstrumentationInfo
                            ComponentName cn = (ComponentName) args[0];
                            if (GlobalConfig.stringEqualBlacklist(cn.getPackageName(), type)) {
                                result = null;
                            }
                            break;
                        case -432049674:                    // getComponentEnabledSetting
                            cn = (ComponentName) args[0];
                            if (GlobalConfig.stringEqualBlacklist(cn.getPackageName(), type)) {
                                result = 0;
                            }
                            break;
                        case -548994423:                    // getApplicationEnabledSetting
                            if (GlobalConfig.stringEqualBlacklist((String) args[0], type)) {
                                result = 0;
                            }
                            break;
                        case 501009465:                     // getPackageInfoVersioned
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                VersionedPackage vp = (VersionedPackage) args[0];
                                if (GlobalConfig.stringEqualBlacklist(vp.getPackageName(), type)) {
                                    result = null;
                                }
                            }
                            break;
                        case 686218487:                     // checkPermission
                            if (GlobalConfig.stringEqualBlacklist((String) args[1], type)) {
                                result = PackageManager.PERMISSION_DENIED;
                            }
                            break;
                        case -1879918258:                   // isPermissionRevokedByPolicy
                            if (GlobalConfig.stringEqualBlacklist((String) args[1], type)) {
                                result = false;
                            }
                            break;
                        case -1221197494:                   // isInstantApp
                            if (GlobalConfig.stringEqualBlacklist((String) args[0], type)) {
                                result = false;
                            }
                            break;
                        case -2108206142:                   // getPermissionFlags
                            if (GlobalConfig.stringEqualBlacklist((String) args[1], type)) {
                                result = 0;
                            }
                            break;
                        case -923564278:                    // getPackagesForUid
                            String[] pkgs = (String[]) result;
                            List<String> list = new ArrayList<>(pkgs.length);
                            boolean change = false;
                            for (String pkg : pkgs) {
                                if (GlobalConfig.stringEqualBlacklist(pkg, type)) {
                                    change = true;
                                } else {
                                    list.add(pkg);
                                }
                            }
                            if (change) {
                                result = list.isEmpty() ? null : list.toArray(new String[0]);
                            }
                            break;
                        case -564624472:                        // resolveIntent
                        case -297395415:                        // resolveService
                            ResolveInfo ri = (ResolveInfo) result;
                            if (GlobalConfig.stringEqualBlacklist(ri.resolvePackageName, type)) {
                                result = null;
                            }
                            break;
                        case 1326102014:                        // resolveContentProvider
                            ProviderInfo pi = (ProviderInfo) result;
                            if (GlobalConfig.stringEqualBlacklist(pi.packageName, type)) {
                                result = null;
                            }
                            break;
                        default:
                            break;
                    }
                }catch (Throwable e){
                    LogUtil.e(TAG, "resolve data error", e);
                }
                return result;
            }
        }.set(oldPm));
        ReflectUtil.setField("android.app.ActivityThread", null, "sPackageManager", newPm);
        newPm = ReflectUtil.invoke("android.app.ActivityThread", null, "getPackageManager");
    }

    @SuppressLint("PrivateApi")
    private void shieldActivityManager() {
        try {
            //获取的Singleton<IActivityManager>成员
            //获取Singleton<IActivityManager> gDefault的真实对象
            Object gDefault;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                gDefault = ReflectUtil.getFieldStatic("android.app.ActivityManager", "IActivityManagerSingleton");
            } else {
                gDefault = ReflectUtil.getFieldStatic("android.app.ActivityManagerNative", "gDefault");
            }
            //gDefault关联Singleton，所以mInstance就是IActivityManager的实例
            final Object iam = ReflectUtil.invoke("android.util.Singleton", gDefault, "get");
            Class<?> iActivityManagerInterface = Class.forName("android.app.IActivityManager");
            Object proxy = Proxy.newProxyInstance(iActivityManagerInterface.getClassLoader(), new Class[]{iActivityManagerInterface},
                    new InvocationHandler() {
                        private Object orig;

                        public InvocationHandler set(Object orig) {
                            this.orig = orig;
                            return this;
                        }

                        @Override
                        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                            Object result = method.invoke(orig, args);
                            if (result == null) {
                                return result;
                            }
                            try {
                                switch (method.getName()) {
                                    case "getServices":     // getRunningServices
                                        Iterator<ActivityManager.RunningServiceInfo> rsIt = ((List<ActivityManager.RunningServiceInfo>) result).iterator();
                                        while (rsIt.hasNext()) {
                                            String tempProcessName = rsIt.next().process;
                                            if (tempProcessName != null && GlobalConfig.stringContainBlackList(tempProcessName,
                                                    DataModelType.COMPONENT_KEY_HIDE)) {
                                                rsIt.remove();
                                                LogUtil.w(Const.JAVA_MONITOR_STATE, "hide filter running service: %s", tempProcessName);
                                            }
                                        }
                                        break;
                                    case "getTasks":  // "getRunningTasks":  sdk <= 28  List<ActivityManager.RunningTaskInfo>
                                        Iterator<ActivityManager.RunningTaskInfo> rtIt = ((List<ActivityManager.RunningTaskInfo>) result).iterator();
                                        while (rtIt.hasNext()) {
                                            String tempBaseActivity = rtIt.next().baseActivity.flattenToString();
                                            if (tempBaseActivity != null && GlobalConfig.stringContainBlackList(tempBaseActivity,
                                                    DataModelType.COMPONENT_KEY_HIDE)) {
                                                rtIt.remove();
                                                LogUtil.w(Const.JAVA_MONITOR_STATE, "hide filter running task: %s", tempBaseActivity);
                                            }
                                        }
                                        break;
                                    case "getRunningAppProcesses":
                                        Iterator<ActivityManager.RunningAppProcessInfo> rpIt = ((List<ActivityManager.RunningAppProcessInfo>) result).iterator();
                                        while (rpIt.hasNext()) {
                                            String tempProcessName = rpIt.next().processName;
                                            if (tempProcessName != null && GlobalConfig.stringContainBlackList(tempProcessName, DataModelType.COMPONENT_KEY_HIDE)) {
                                                rpIt.remove();
                                                LogUtil.w(Const.JAVA_MONITOR_STATE, "hide filter running process: %s", tempProcessName);
                                            }
                                        }
                                        break;
                                    default:
                                        break;
                                }
                            }catch (Throwable e){
                                LogUtil.e(TAG, "resolve IActivityManager data error", e);
                            }
                            return result;
                        }
                    }.set(iam));
            ReflectUtil.setField("android.util.Singleton", gDefault, "mInstance", proxy);
        } catch (Exception e) {
            LogUtil.e(TAG, "hookActivityManager: exception", e);
        }
    }

    @SuppressLint({"DiscouragedPrivateApi", "PrivateApi"})
    private void shieldActivityTaskManager() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return;
        }
        try {
            //获取的Singleton<IActivityTaskManager>成员
            Object gDefault = ReflectUtil.getFieldStatic("android.app.ActivityTaskManager", "IActivityTaskManagerSingleton");
            //获取Singleton<IActivityTaskManager> IActivityTaskManagerSingleton的真实对象
            //获取到了IActivityManager的真实对象 相当于getService或者getDefault
            //gDefault关联Singleton，所以mInstance就是IActivityManager的实例
            final Object iatm = ReflectUtil.invoke("android.util.Singleton", gDefault, "get");
            Class<?> iActivityTaskManagerInterface = Class.forName("android.app.IActivityTaskManager");
            Object proxy = Proxy.newProxyInstance(iActivityTaskManagerInterface.getClassLoader(),
                    new Class[]{iActivityTaskManagerInterface}, new InvocationHandler() {
                        private Object orig;

                        public InvocationHandler set(Object orig) {
                            this.orig = orig;
                            return this;
                        }

                        @Override
                        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                            Object result = method.invoke(orig, args);
                            if (result == null) {
                                return result;
                            }
                            try {
                                switch (method.getName()) {
                                    case "getTasks":    // sdk > 28
                                        Iterator<ActivityManager.RunningTaskInfo> rtIt = ((List<ActivityManager.RunningTaskInfo>) result).iterator();
                                        while (rtIt.hasNext()) {
                                            String tempBaseActivity = rtIt.next().baseActivity.flattenToString();
                                            if (tempBaseActivity != null && GlobalConfig.stringContainBlackList(tempBaseActivity,
                                                    DataModelType.COMPONENT_KEY_HIDE)) {
                                                rtIt.remove();
                                                LogUtil.w(Const.JAVA_MONITOR_STATE, "hide filter running task: %s", tempBaseActivity);
                                            }
                                        }
                                        break;
                                    default:
                                        break;
                                }
                            }catch (Throwable e){
                                LogUtil.e(TAG, "resolve ActivityTaskManager data error", e);
                            }
                            return result;
                        }
                    }.set(iatm));
            ReflectUtil.setField("android.util.Singleton", gDefault, "mInstance", proxy);
        } catch (Throwable e) {
            LogUtil.e(TAG, "replace ActivityTaskManager object failed.", e);
        }

    }
}
