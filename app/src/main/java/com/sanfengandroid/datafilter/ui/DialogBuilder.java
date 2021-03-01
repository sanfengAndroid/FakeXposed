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

package com.sanfengandroid.datafilter.ui;

import android.content.Context;
import android.content.DialogInterface;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.sanfengandroid.datafilter.R;

public class DialogBuilder {
    private DialogBuilder() {
    }

    public static void confirmShow(Context context, int title, int message, DialogInterface.OnClickListener negativeCallback) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context).setTitle(title)
                .setNegativeButton(R.string.confirm, negativeCallback);
        if (message != 0) {
            builder.setMessage(message);
        }
        builder.show();
    }

    public static void confirmCancelShow(Context context, int title, String message, DialogInterface.OnClickListener positiveCallback) {
        new MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(R.string.confirm, positiveCallback)
                .setNegativeButton(R.string.cancel, null).show();
    }

    public static void confirmCancelShow(Context context, int title, DialogInterface.OnClickListener positiveCallback, DialogInterface.OnClickListener negativeCallback) {
        new MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setPositiveButton(R.string.confirm, positiveCallback)
                .setNegativeButton(R.string.cancel, negativeCallback).show();
    }

    public static void confirmNeutralShow(Context context, int title, DialogInterface.OnClickListener positiveCallback, DialogInterface.OnClickListener neutralCallback, DialogInterface.OnClickListener negativeCallback) {
        new MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setPositiveButton(R.string.confirm, positiveCallback)
                .setNeutralButton(R.string.cancel, neutralCallback)
                .setNegativeButton(R.string.delete, negativeCallback)
                .show();
    }

    public static void confirmNeutralShow(Context context, int title, String message, int positive, int neutral, int negative, DialogInterface.OnClickListener positiveCallback, DialogInterface.OnClickListener negativeCallback) {
        new MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(positive, positiveCallback)
                .setNegativeButton(negative, null)
                .setNeutralButton(neutral, negativeCallback)
                .show();
    }
}
