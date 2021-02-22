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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

public class HookRuntime implements IHook {
    private static final String TAG = HookRuntime.class.getSimpleName();

    private static byte[] toCString(String s) {
        if (s == null) {
            return null;
        }
        byte[] bytes = s.getBytes();
        byte[] result = new byte[bytes.length + 1];
        System.arraycopy(bytes, 0,
                result, 0,
                bytes.length);
        result[result.length - 1] = (byte) 0;
        return result;
    }

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
            private ExecBean bean;

            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                //  byte[] prog,   byte[] argBlock, int argc,   byte[] envBlock,        int envc,       byte[] dir,          int[] fds,                  boolean redirectErrorStream
                //   C格式命令       C格式参数          参数数量      C格式环境变量       环境变量数量      C格式工作目录   3个标准I/O重定向文件描述符默认-1           重定向错误流默认false
                bean = null;
                byte[] cmdBytes = (byte[]) param.args[0];
                String cmd = new String(cmdBytes, 0, cmdBytes.length - 1);
                byte[] cargs = (byte[]) param.args[1];
                LogUtil.v(TAG, "Call Runtime exec cmd: %s, args: %s", cmd, cargs.length > 0 ? new String(cargs, 0, cargs.length - 1) : "");
                bean = GlobalConfig.getEqualBlacklistValue(cmd, DataModelType.RUNTIME_EXEC_HIDE, ExecBean.class);
                if (bean == null) {
                    return;
                }
                if (bean.newCmd != null) {
                    param.args[0] = toCString(bean.newCmd);
                }
                if (bean.replaceArgs) {
                    byte[][] args = new byte[bean.args.length][];
                    int size = args.length; // For added NUL bytes
                    for (int i = 0; i < args.length; i++) {
                        args[i] = bean.args[i].getBytes();
                        size += args[i].length;
                    }
                    byte[] argBlock = new byte[size];
                    int i = 0;
                    for (byte[] arg : args) {
                        System.arraycopy(arg, 0, argBlock, i, arg.length);
                        i += arg.length + 1;
                        // No need to write NUL bytes explicitly
                    }
                    param.args[1] = argBlock;
                    param.args[2] = args.length;
                }
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
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
            }
        });
    }

    private void shieldRuntimeLow() throws Throwable {
        Method method = Class.forName("java.lang.ProcessManager").getDeclaredMethod("exec", String[].class, String[].class, File.class, boolean.class);
        GlobalConfig.addHookMethodModifierFilter(method);
        XposedBridge.hookMethod(method, new XC_MethodHook() {
            private ExecBean bean;

            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                // String[] taintedCommand, String[] taintedEnvironment, File workingDirectory, boolean redirectErrorStream
                bean = null;
                String[] taintedCommand = (String[]) param.args[0];
                LogUtil.v(TAG, "Runtime exec : %s", Arrays.toString(taintedCommand));
                bean = GlobalConfig.getEqualBlacklistValue(taintedCommand[0], DataModelType.RUNTIME_EXEC_HIDE, ExecBean.class);
                if (bean == null || (bean.newCmd == null && !bean.replaceArgs)) {
                    return;
                }
                List<String> list = new ArrayList<>();
                list.add(bean.newCmd == null ? taintedCommand[0] : bean.newCmd);
                if (bean.args != null) {
                    for (String s : bean.args) {
                        if (s != null) {
                            list.add(s);
                        }
                    }
                }
                param.args[0] = list.toArray(new String[0]);
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
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
            }
        });
    }
}
