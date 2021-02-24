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

import android.os.Build;

import com.sanfengandroid.common.bean.ExecBean;
import com.sanfengandroid.common.model.base.DataModelType;
import com.sanfengandroid.common.proxy.ProxyInputStream;
import com.sanfengandroid.common.proxy.ProxyOutStream;
import com.sanfengandroid.common.reflection.ReflectUtil;
import com.sanfengandroid.common.util.LogUtil;
import com.sanfengandroid.fakeinterface.GlobalConfig;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

public class HookRuntime implements IHook {
    private static final String TAG = HookRuntime.class.getSimpleName();

    /**
     * Android 7.0及以上java.lang.UNIXProcess
     * Android 7.0以下java.lang.ProcessManager$ProcessImpl,但是要拦截ProcessManager.exec
     */
    @Override
    public void hook(ClassLoader loader) throws Throwable {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            shieldRuntimeHigh();
        } else {
            shieldRuntimeLow();
        }
    }

    private void shieldRuntimeHigh() throws Throwable {
        Constructor<?> ctor = Class.forName("java.lang.UNIXProcess").getDeclaredConstructors()[0];
        GlobalConfig.addHookMethodModifierFilter(ctor);
        XposedBridge.hookMethod(ctor, new XC_MethodHook() {
            private final ThreadLocal<ExecBean> founds = new ThreadLocal<>();

            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                //  byte[] prog,   byte[] argBlock, int argc,   byte[] envBlock,        int envc,       byte[] dir,          int[] fds,                  boolean redirectErrorStream
                //   C格式命令       C格式参数          参数数量      C格式环境变量       环境变量数量      C格式工作目录   3个标准I/O重定向文件描述符默认-1           重定向错误流默认false

                byte[] cmdBytes = (byte[]) param.args[0];
                String cmd = new String(cmdBytes, 0, cmdBytes.length - 1);
                byte[] cargs = (byte[]) param.args[1];
                LogUtil.v(TAG, "Call Runtime exec cmd: %s, args: %s, arg lens: %s", cmd, cargs.length > 0 ? new String(cargs, 0, cargs.length - 1) : "", param.args[2]);
                List<ExecBean> list = (List<ExecBean>) GlobalConfig.getEqualBlacklistValue(cmd, DataModelType.RUNTIME_EXEC_HIDE);
                if (list == null) {
                    return;
                }
                for (ExecBean bean : list) {
                    LogUtil.d(TAG, "查找命令: %s", bean);
                    if (bean.matchParameter && !Arrays.equals(bean.cOriginalParameters, cargs)) {
                        continue;
                    }
                    if (bean.cReplaceCommand != null) {
                        param.args[0] = bean.cReplaceCommand;
                        LogUtil.d(TAG, "replace cmd: %s", bean.replaceCommand);
                    }
                    if (bean.replaceParameter) {
                        param.args[1] = bean.cReplaceParameters;
                        param.args[2] = bean.replaceParameterLength;
                        LogUtil.d(TAG, "replace args: %s", bean.replaceParameters);
                    }
                    founds.set(bean);
                    break;
                }
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                ExecBean bean = founds.get();
                if (bean == null) {
                    return;
                }
                Process process = (Process) param.thisObject;
                // stdout
                if (bean.inputStream != null) {
                    ReflectUtil.setFieldInstance(process, "stdout", new ProxyInputStream(bean.inputStream));
                }
                // stdin
                if (bean.outStream != null) {
                    ReflectUtil.setFieldInstance(process, "stdin", new ProxyOutStream(process.getOutputStream(), bean.outStream));
                }
                // stderr
                if (bean.errStream != null) {
                    ReflectUtil.setFieldInstance(process, "stdin", new ProxyInputStream(bean.errStream));
                }
                founds.set(null);
            }
        });
    }

    private void shieldRuntimeLow() throws Throwable {
        Method method = Class.forName("java.lang.ProcessManager").getDeclaredMethod("exec", String[].class, String[].class, File.class, boolean.class);
        GlobalConfig.addHookMethodModifierFilter(method);
        XposedBridge.hookMethod(method, new XC_MethodHook() {
            private final ThreadLocal<ExecBean> founds = new ThreadLocal<>();

            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                // String[] taintedCommand, String[] taintedEnvironment, File workingDirectory, boolean redirectErrorStream
                String[] taintedCommand = (String[]) param.args[0];
                LogUtil.v(TAG, "Call Runtime exec : %s", Arrays.toString(taintedCommand));
                List<ExecBean> list = (List<ExecBean>) GlobalConfig.getEqualBlacklistValue(taintedCommand[0], DataModelType.RUNTIME_EXEC_HIDE);
                if (list == null) {
                    return;
                }
                for (ExecBean bean : list) {
                    if (bean.matchParameter && !Arrays.equals(taintedCommand, bean.javaCommands)) {
                        continue;
                    }
                    if (bean.replaceParameter) {
                        param.args[0] = bean.javaReplaceCommands;
                        LogUtil.d(TAG, "repalce args: %s", bean.replaceParameters);
                    } else {
                        taintedCommand[0] = bean.replaceCommand;
                        LogUtil.d(TAG, "repalce cmd: %s", bean.replaceCommand);
                    }
                    founds.set(bean);
                    break;
                }
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                ExecBean bean = founds.get();
                if (bean == null) {
                    return;
                }
                Process process = (Process) param.getResult();
                if (bean.inputStream != null) {
                    ReflectUtil.setFieldInstanceNoException(process, "inputStream", new ProxyInputStream(bean.inputStream));
                }
                if (bean.outStream != null) {
                    ReflectUtil.setFieldInstanceNoException(process, "outputStream", new ProxyOutStream(process.getOutputStream(), bean.outStream));
                }
                if (bean.errStream != null) {
                    ReflectUtil.setFieldInstanceNoException(process, "errorStream", new ProxyInputStream(bean.errStream));
                }
                founds.set(null);
            }
        });
    }
}
