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

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ExecBean {

    /**
     * 旧命令
     */
    public String oldCmd;

    /*
     * 旧命令参数，添加参数更加细致的匹配
     * */
    public String oldArgv;

    public String[] oldArgs;

    public boolean matchArgv;
    /**
     * 替换新命令
     */
    public String newCmd;

    /**
     * true才替换参数
     */
    public boolean replaceArgv;
    /**
     * 新改变参数
     */
    public String newArgv;

    public String[] newArgs;

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
        return matchArgv == bean.matchArgv &&
                oldCmd.equals(bean.oldCmd) &&
                Objects.equals(oldArgv, bean.oldArgv);
    }

    @Override
    public int hashCode() {
        return Objects.hash(oldCmd, oldArgv, matchArgv);
    }

    @Override
    public String toString() {
        return "ExecBean{" +
                "oldCmd='" + oldCmd + '\'' +
                ", oldArgv='" + oldArgv + '\'' +
                ", matchArgv=" + matchArgv +
                ", newCmd='" + newCmd + '\'' +
                ", replaceArgv=" + replaceArgv +
                ", newArgv='" + newArgv + '\'' +
                ", inputStream='" + inputStream + '\'' +
                ", outStream='" + outStream + '\'' +
                ", errStream='" + errStream + '\'' +
                '}';
    }

    public static String[] toJavaStringArguments(String input) {
        if (input == null || input.isEmpty()) {
            return null;
        }
        String[] parameters = input.split(" ");
        if (parameters.length == 1) {
            return parameters;
        }
        List<String> list = new ArrayList<>();
        String str = null;
        for (String parameter : parameters) {
            if (str == null) {
                str = parameter;
                continue;
            }
            if (str.endsWith("\\")) {
                str += parameter;
            } else {
                str = parameter;
                list.add(str);
            }
        }
        if (str != null){
            list.add(str);
        }
        return list.toArray(new String[0]);
    }

    public void transform() {
        if (!TextUtils.isEmpty(oldArgv)) {
            oldArgs = toJavaStringArguments(oldArgv);
        }
        if (!TextUtils.isEmpty(newArgv) && replaceArgv) {
            newArgs = toJavaStringArguments(newArgv);
        }
        if (!TextUtils.isEmpty(inputStream) && !inputStream.endsWith("\n")){
            inputStream += "\n";
        }
    }
}
