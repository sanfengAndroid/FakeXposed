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

import android.util.Log;

/**
 * @author sanfengAndroid
 * @date 2020/10/07
 */
public class LogUtil {
    private static final int MAX_LENGTH = 3 * 1024;
    private static final int STATE_NONE = 0;
    private static final LogCallback[] callbacks = new LogCallback[5];
    private static final boolean STATES[] = new boolean[]{true, true, true, true, true, true, true, true, true, true};
    public static String HEAD = "HookLog_";
    public static boolean ADD_HEAD = true;
    public static String DEFAULT_HEAD = "HookLog";
    public static int minLogLevel = Log.VERBOSE;
    private static int logType = LogMode.CALLBACK_AND_PRINT.getType();

    public static void v(String format, Object... msg) {
        print(STATE_NONE, Log.VERBOSE, DEFAULT_HEAD, format, msg);
    }

    public static void v(String tag, String format, Object... msg) {
        print(STATE_NONE, Log.VERBOSE, tag, format, msg);
    }

    public static void v(int state, String format, Object... msg) {
        print(state, Log.VERBOSE, DEFAULT_HEAD, format, msg);
    }

    public static void v(String tag, int state, String format, Object... msg) {
        print(state, Log.VERBOSE, tag, format, msg);
    }

    public static void d(String format, Object... msg) {
        print(STATE_NONE, Log.DEBUG, DEFAULT_HEAD, format, msg);
    }

    public static void d(String tag, String format, Object... msg) {
        print(STATE_NONE, Log.DEBUG, tag, format, msg);
    }

    public static void d(int state, String format, Object... msg) {
        print(state, Log.DEBUG, DEFAULT_HEAD, format, msg);
    }

    public static void d(String tag, int state, String format, Object... msg) {
        print(state, Log.DEBUG, tag, format, msg);
    }

    public static void i(String format, Object... msg) {
        print(STATE_NONE, Log.INFO, DEFAULT_HEAD, format, msg);
    }

    public static void i(String tag, String format, Object... msg) {
        print(STATE_NONE, Log.INFO, tag, format, msg);
    }

    public static void i(int state, String format, Object... msg) {
        print(state, Log.INFO, DEFAULT_HEAD, format, msg);
    }

    public static void i(String tag, int state, String format, Object... msg) {
        print(state, Log.INFO, tag, format, msg);
    }

    public static void w(String format, Object... msg) {
        print(STATE_NONE, Log.WARN, DEFAULT_HEAD, format, msg);
    }

    public static void w(String tag, String format, Object... msg) {
        print(STATE_NONE, Log.WARN, tag, format, msg);
    }

    public static void w(int state, String format, Object... msg) {
        print(state, Log.WARN, DEFAULT_HEAD, format, msg);
    }

    public static void w(String tag, int state, String format, Object... msg) {
        print(state, Log.WARN, tag, format, msg);
    }

    public static void e(String format, Object... msg) {
        print(STATE_NONE, Log.ERROR, DEFAULT_HEAD, format, msg);
    }

    public static void e(String tag, String format, Object... msg) {
        print(STATE_NONE, Log.ERROR, tag, format, msg);
    }

    public static void e(int state, String format, Object... msg) {
        print(state, Log.ERROR, DEFAULT_HEAD, format, msg);
    }

    public static void e(String tag, int state, String format, Object... msg) {
        print(state, Log.ERROR, tag, format, msg);
    }

    /**
     * @param state 0 ~ 10调用者不要传入超出范围值
     */
    private static void print(int state, int level, String tag, String format, Object... msg) {
        if (logType == 0) {
            return;
        }
        Throwable throwable = null;
        if (msg != null && msg.length > 0 && msg[msg.length - 1] instanceof Throwable) {
            throwable = (Throwable) msg[msg.length - 1];
        }
        String str = formatText(format, msg);
        if ((logType & 2) != 0 && callbacks[level - Log.VERBOSE] != null) {
            callbacks[level - Log.VERBOSE].visit(state, level, tag, str, throwable);
        }
        if ((logType & 0x1) == 0 || !STATES[state]) {
            return;
        }
        if (level < minLogLevel){
            return;
        }
        String[] ret = splitMsg(str);
        for (int i = 0; i < ret.length; i++) {
            String head;
            if (i == 0) {
                head = ADD_HEAD ? HEAD + tag : tag;
            } else {
                head = ADD_HEAD ? HEAD + tag + i : tag + i;
                throwable = null;
            }
            switch (level) {
                case Log.VERBOSE:
                    Log.v(head, ret[i], throwable);
                    break;
                case Log.DEBUG:
                    Log.d(head, ret[i], throwable);
                    break;
                case Log.INFO:
                    Log.i(head, ret[i], throwable);
                    break;
                case Log.WARN:
                    Log.w(head, ret[i], throwable);
                    break;
                case Log.ERROR:
                    Log.e(head, ret[i], throwable);
                    break;
                case Log.ASSERT:
                    break;
                default:
                    break;
            }
        }
    }

    public static void println(int priority, String tag, String format, Object... msg) {
        if ((logType & 1) != 0) {
            Log.println(priority, ADD_HEAD ? HEAD + tag : tag, formatText(format, msg));
        }
    }

    private static String formatText(String format, Object... msg) {
        try {
            if (msg != null) {
                return String.format(format, msg);
            }
        } catch (Throwable ignored) {
        }
        return format;
    }

    private static String[] splitMsg(String msg) {
        if (msg.length() <= MAX_LENGTH) {
            return new String[]{msg};
        }
        int len = msg.length() / MAX_LENGTH + 1;
        String[] ret = new String[len];
        int index = 0;
        for (int i = 0; index < msg.length(); i++) {
            if (msg.length() <= index + MAX_LENGTH) {
                ret[i] = msg.substring(index);
            } else {
                ret[i] = msg.substring(index, index + MAX_LENGTH);
            }
            index += MAX_LENGTH;
        }
        return ret;
    }

    public static void addCallback(int level, LogCallback callback) {
        if (level >= Log.VERBOSE && level <= Log.ERROR) {
            callbacks[level - Log.VERBOSE] = callback;
        }
    }

    public static void setStateNone(int state, boolean open) {
        if (state > 10 || state < 0) {
            Log.w(DEFAULT_HEAD, "state out of range: " + state + ", use 0 ~ 9");
            return;
        }
        STATES[state] = open;
    }

    public static void setLogMode(LogMode mode) {
        logType = mode == null ? LogMode.NONE.getType() : mode.getType();
    }

    public enum LogMode {
        /**
         * 既不回调也不打印
         */
        NONE(0),
        ONLY_PRINT(1),
        ONLY_CALLBACK(2),
        CALLBACK_AND_PRINT(3);

        int type;

        LogMode(int type) {
            this.type = type;
        }

        public int getType() {
            return type;
        }
    }

    public interface LogCallback {
        /**
         * @param state     状态标记,自行区分,默认0
         * @param level     日志等级
         * @param tag       日志标签
         * @param msg       日志内容
         * @param throwable 异常,没有异常则为null
         */
        void visit(int state, int level, String tag, String msg, Throwable throwable);
    }
}
