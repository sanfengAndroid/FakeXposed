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

package com.sanfengandroid.xp;

import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

/**
 * @author sanfengAndroid
 */
public class ObservableMap<K, V> extends HashMap<K, V> {
    private final MapObservable<K, V> mObservable = new MapObservable<>();

    @Nullable
    @Override
    public V put(K key, V value) {
        V ret = super.put(key, value);
        mObservable.update(key, value);
        return ret;
    }

    @Override
    public void putAll(@NonNull Map<? extends K, ? extends V> m) {
        super.putAll(m);
        mObservable.update(m);
    }

    public void addObserver(Observer observer) {
        mObservable.addObserver(observer, this);
    }

    public void deleteObserver(Observer observer) {
        mObservable.deleteObserver(observer);
    }

    public int countObservers() {
        return mObservable.countObservers();
    }

    public void deleteObservers() {
        mObservable.deleteObservers();
    }

    public boolean hasChanged() {
        return mObservable.hasChanged();
    }

    private static class MapObservable<K, V> extends Observable {
        public void update(K key, V value) {
            setChanged();
            notifyObservers(new Pair<>(key, value));
        }

        public void update(Map<? extends K, ? extends V> m) {
            if (!m.isEmpty()) {
                setChanged();
                notifyObservers(m);
            }
        }

        public synchronized void addObserver(Observer o, Map<? extends K, ? extends V> m) {
            // 这里不过滤重复添加,代码保证只被添加一次
            super.addObserver(o);
            o.update(this, m);
        }
    }
}
