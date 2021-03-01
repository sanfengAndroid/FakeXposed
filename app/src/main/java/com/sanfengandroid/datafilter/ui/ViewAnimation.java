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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.view.View;

import com.sanfengandroid.datafilter.R;

/**
 * @author sanfengAndroid
 */
public class ViewAnimation {

    public static boolean rotateFab(final View v, boolean rotate) {
        v.animate().setDuration(200)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                    }
                }).rotation(rotate ? 135f : 0f);
        return rotate;
    }

    public static void hide(final View v, int index) {
        v.setVisibility(View.VISIBLE);
        v.setAlpha(1f);
        v.setTranslationY(-((index + 1) * v.getHeight() + v.getResources().getDimension(R.dimen.fab_interval)));
        v.animate().setDuration(200)
                .translationY(0)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                    }
                })
                .alpha(0f)
                .start();
    }

    public static void show(final View v, int index) {
        v.setVisibility(View.VISIBLE);
        v.setAlpha(0f);
        v.setTranslationY(0);
        v.animate().setDuration(200)
                .translationY(-((index + 1) * v.getHeight() + v.getResources().getDimension(R.dimen.fab_interval)))
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                    }
                }).alpha(1f)
                .start();
    }

    public static void init(final View v) {
        v.setVisibility(View.INVISIBLE);
        v.setTranslationY(0);
        v.setAlpha(0f);
    }
}
