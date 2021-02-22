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

import java.util.Objects;

public class ExecBean {

    /**
     * 旧命令
     */
    public String oldCmd;
    /**
     * 替换新命令
     */
    public String newCmd;
    /**
     * true才替换参数
     */
    public boolean replaceArgs;
    /**
     * 新改变参数
     */
    public String[] args;
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
        return oldCmd.equals(bean.oldCmd) &&
                Objects.equals(newCmd, bean.newCmd);
    }

    @Override
    public int hashCode() {
        return Objects.hash(oldCmd, newCmd);
    }
}
