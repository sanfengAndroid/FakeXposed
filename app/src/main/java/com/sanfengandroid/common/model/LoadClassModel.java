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

package com.sanfengandroid.common.model;

import android.text.TextUtils;

import com.sanfengandroid.datafilter.R;
import com.sanfengandroid.datafilter.XpApplication;

/**
 * @author sanfengAndroid
 */
public class LoadClassModel extends StringModel {

    private static boolean validJavaFullClassName(String fullName) {
        String split = ".";
        if (fullName.endsWith(split)) {
            return false;
        }
        int index = fullName.indexOf(split);
        if (index == -1) {
            return validJavaIdentifier(fullName);
        }
        for (String name : fullName.split("\\.")) {
            if (!validJavaIdentifier(name)) {
                return false;
            }
        }
        return true;
    }

    private static boolean validJavaIdentifier(String className) {
        if (className.length() == 0 || !Character.isJavaIdentifierStart(className.charAt(0))) {
            return false;
        }
        int size = className.length();
        for (int i = 1; i < size; i++) {
            if (!Character.isJavaIdentifierPart(className.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public void onSave() {
        String value = getInput();
        if (TextUtils.isEmpty(value)) {
            XpApplication.getViewModel().setMessage(R.string.input_empty_content);
            return;
        }
        if (!validJavaFullClassName(value)) {
            XpApplication.getViewModel().setMessage(R.string.input_class_name_illegal);
            return;
        }
        LoadClassModel add = new LoadClassModel();
        add.setValue(value);
        XpApplication.getViewModel().addDataValue(add, index);
        XpApplication.getViewModel().setSaveResult(true);
    }
}
