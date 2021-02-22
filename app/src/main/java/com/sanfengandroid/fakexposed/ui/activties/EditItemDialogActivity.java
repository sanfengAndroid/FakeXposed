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

package com.sanfengandroid.fakexposed.ui.activties;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.sanfengandroid.fakexposed.listener.ICallExternal;
import com.sanfengandroid.fakexposed.ui.fragments.EditItemDialogFragment;

public class EditItemDialogActivity extends AppCompatActivity implements ICallExternal {
    private EditItemDialogFragment fragment;
    private ICallExternal callback;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            fragment = new EditItemDialogFragment();
            getSupportFragmentManager().beginTransaction()
                    .replace(android.R.id.content, fragment, null)
                    .commitNow();
        }
    }

    @Override
    public void onBackPressed() {
        if (fragment != null && fragment.onBackPressed()) {
            return;
        }
        super.onBackPressed();
    }

    @Override
    public void setExternalCallback(ICallExternal listener) {
        callback = listener;
    }

    @Override
    public void callExternalIntent(Intent intent, int code) {
        startActivityForResult(intent, code);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (callback != null) {
            callback.callbackExternalResult(requestCode, resultCode, data);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
