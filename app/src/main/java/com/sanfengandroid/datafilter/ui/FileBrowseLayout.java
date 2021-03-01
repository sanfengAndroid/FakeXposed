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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.google.android.material.textfield.TextInputLayout;
import com.sanfengandroid.common.util.FileChooseUtil;
import com.sanfengandroid.datafilter.R;
import com.sanfengandroid.datafilter.listener.ICallExternal;

public class FileBrowseLayout {

    public static void setFileBrowse(Context context, TextInputLayout layout, FileBrowseResult callback) {
        if (!(context instanceof ICallExternal)) {
            layout.setEndIconMode(TextInputLayout.END_ICON_NONE);
            return;
        }
        ICallExternal callExternal = (ICallExternal) context;
        layout.setEndIconMode(TextInputLayout.END_ICON_CUSTOM);
        layout.setEndIconDrawable(R.drawable.ic_open_file_24dp);
        layout.setEndIconOnClickListener(v -> {
            callExternal.setExternalCallback(new ICallExternal() {
                @Override
                public void callbackExternalResult(int requestCode, int resultCode, Intent data) {
                    boolean success = resultCode == Activity.RESULT_OK && data != null && data.getData() != null;
                    if (callback != null) {
                        callback.onResult(success, success ? new FileChooseUtil(context).getChooseFileResultPath(data.getData()) : "");
                    }
                }
            });
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT).setType("*/*");
            callExternal.callExternalIntent(intent, 1);
        });

    }

    public interface FileBrowseResult {
        void onResult(boolean success, String path);
    }
}
