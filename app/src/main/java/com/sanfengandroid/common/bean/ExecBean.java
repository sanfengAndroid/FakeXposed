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

package com.sanfengandroid.common.bean;

import android.os.Build;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class ExecBean {

    /**
     * 旧命令
     */
    public String originalCommand;

    public byte[] cOriginalCommand;

    /*
     * 旧命令参数，添加参数更加细致的匹配
     * */
    public String originalParameters;

    public byte[] cOriginalParameters;

    // Android 7以下为了比较方便,第一个字符串是命令,第二个开始是参数
    public String[] javaCommands;

    public boolean matchParameter;
    /**
     * 替换新命令
     */
    public String replaceCommand;

    public byte[] cReplaceCommand;
    /**
     * true才替换参数
     */
    public boolean replaceParameter;
    /**
     * 新改变参数
     */
    public String replaceParameters;

    public byte[] cReplaceParameters;

    // Android 7以下为了比较方便,第一个字符串是命令,第二个开始是参数
    public String[] javaReplaceCommands;

    public int replaceParameterLength;
    /**
     * 替换并固定原始输入流
     */
    public String inputStream;
    /**
     * 替换并固定原始输出流
     */
    public String outStream;

    /**
     * 替换并固定错误流
     */
    public String errStream;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExecBean bean = (ExecBean) o;
        return matchParameter == bean.matchParameter &&
                originalCommand.equals(bean.originalCommand) &&
                Objects.equals(originalParameters, bean.originalParameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(originalCommand, originalParameters, matchParameter);
    }

    @Override
    public String toString() {
        return "ExecBean{" +
                "originalCommand='" + originalCommand + '\'' +
                ", originalParameters='" + originalParameters + '\'' +
                ", matchParameter=" + matchParameter +
                ", replaceCommand='" + replaceCommand + '\'' +
                ", replaceParameter=" + replaceParameter +
                ", replaceParameters='" + replaceParameters + '\'' +
                '}';
    }

    public static byte[] toCString(String s) {
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

    public static byte[] toCStrings(String[] args) {
        int len = 0;
        for (String s : args) {
            len += s.length();
        }
        byte[] bytes = new byte[args.length + len];
        int pos = 0;
        for (String s : args) {
            byte[] sb = s.getBytes();
            System.arraycopy(sb, 0, bytes, pos, sb.length);
            pos += sb.length;
            bytes[pos++] = 0;
        }
        return bytes;
    }

    public static byte[] toCStringArguments(String input) {
        if (input == null || input.isEmpty()) {
            return null;
        }
        byte[] args = input.getBytes();
        byte[] result = new byte[args.length * 2];
        int pos = 0;
        boolean escape = false;
        for (byte b : args) {
            if (escape) {
                switch (b) {
                    case 't':
                        result[pos] = '\t';
                        break;
                    case 'r':
                        result[pos] = '\r';
                        break;
                    case 'n':
                        result[pos] = '\n';
                        break;
                    case '\\':
                        result[pos] = '\\';
                        break;
                    case ' ':
                        result[pos] = ' ';
                        break;
                    default:
                        throw new UnsupportedOperationException("Unsupported string escape: \\" + b + ", only support \\t,\\r,\\n,\\\\,\\ ");
                }
                pos++;
                escape = false;
                continue;
            }

            switch (b) {
                case '\\':
                    escape = true;
                    break;
                case ' ':
                    result[pos++] = '\0';
                    break;
                default:
                    result[pos++] = b;
                    break;
            }
        }
        result[pos++] = '\0';
        return Arrays.copyOf(result, pos);
    }

    public static String[] toJavaStringArguments(String cmd, String input) {
        byte[] cargs = toCStringArguments(input);
        List<String> list = new ArrayList<>();
        list.add(cmd);
        if (cargs != null) {
            int start = 0;
            int end = 0;
            for (byte b : cargs) {
                if (b == '\0') {
                    list.add(new String(cargs, start, end++));
                    start = end;
                } else {
                    end++;
                }
            }
        }
        return list.toArray(new String[0]);
    }

    public static String toJavaString(byte[] bytes, int start) {
        int end = start;
        for (int i = start; i < bytes.length; i++) {
            if (bytes[i] == 0) {
                end = i;
            }
        }
        return new String(bytes, start, end);
    }

    public static String[] toJavaStrings(byte[] bytes, int len) {
        if (len == 0) {
            return null;
        }
        String[] arrs = new String[len];
        int pos = 0;
        for (int i = 0; i < len; i++) {
            arrs[i] = toJavaString(bytes, pos);
            pos += arrs[i].length();
            pos++;
        }
        return arrs;
    }


    public void transform() {
        // Android 7及以上使用的是c字符串,Android 7以下使用java字符串
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            cOriginalCommand = toCString(originalCommand);
            cOriginalParameters = toCStringArguments(originalParameters);
            if (cOriginalParameters == null) {
                cOriginalParameters = new byte[0];
            }
            cReplaceCommand = toCString(replaceCommand);
            cReplaceParameters = toCStringArguments(replaceParameters);
            if (cReplaceParameters != null) {
                replaceParameterLength = 0;
                for (byte b : cReplaceParameters) {
                    if (b == '\0') {
                        replaceParameterLength++;
                    }
                }
            } else {
                cReplaceParameters = new byte[0];
                replaceParameterLength = 0;
            }
        } else {
            if (TextUtils.isEmpty(replaceCommand)) {
                replaceCommand = originalCommand;
            }
            javaCommands = toJavaStringArguments(originalCommand, originalParameters);
            javaReplaceCommands = toJavaStringArguments(replaceCommand, replaceParameters);
        }
    }
}
