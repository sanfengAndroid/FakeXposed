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

package com.sanfengandroid.datafilter.viewmodel;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Pair;

import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.sanfengandroid.fakeinterface.GlobalConfig;
import com.sanfengandroid.common.model.InstallPackageModel;
import com.sanfengandroid.common.model.PackageModel;
import com.sanfengandroid.common.model.base.DataModelType;
import com.sanfengandroid.common.model.base.ShowDataModel;
import com.sanfengandroid.common.util.LogUtil;
import com.sanfengandroid.datafilter.SPProvider;
import com.sanfengandroid.datafilter.XpApplication;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * @author sanfengAndroid
 */
public class ApplicationViewModel extends ViewModel {
    public static final int NO_INDEX = -1;
    private final MutableLiveData<List<InstallPackageModel>> installed = new MutableLiveData<>();
    private final Map<String, InstallPackageModel> apks = new HashMap<>();
    private final MutableLiveData<Boolean> saveResult = new SingleLiveData<>();
    private final MutableLiveData<String> messageData = new SingleLiveData<>();
    private final MutableLiveData<Pair<ShowDataModel, Integer>> editModel = new SingleLiveData<>();
    /**
     * 与{@link #dataModelType}配合使用
     */
    private final MutableLiveData<List<ShowDataModel>> data = new MutableLiveData<>();
    private final Map<DataModelType, List<ShowDataModel>> dataSources = new HashMap<>();
    private final MutableLiveData<Map<String, Boolean>> hookApps = new MutableLiveData<>();
    private final MutableLiveData<Pair<String, Boolean>> hookApp = new MutableLiveData<>();
    /**
     * 当前设置生效的包名
     */
    private String currentPackage;
    private DataModelType dataModelType = DataModelType.NOTHING;
    private final Observer<List<ShowDataModel>> saveObserver = new Observer<List<ShowDataModel>>() {
        @Override
        public void onChanged(List<ShowDataModel> objects) {
            AsyncTask.execute(() -> {
                if (objects == null || currentPackage == null || dataModelType == null || dataModelType == DataModelType.NOTHING) {
                    return;
                }
                JSONArray array = new JSONArray();
                for (ShowDataModel model : objects) {
                    JSONObject value;
                    try {
                        value = model.serialization();
                    } catch (JSONException e) {
                        throw new RuntimeException("serialization object " + model.getClass().getName() + " error", e);
                    }
                    if (value != null) {
                        array.put(value);
                    }
                }
                SPProvider.putAppStringConfig(XpApplication.getInstance(), currentPackage, dataModelType, array);
            });
        }
    };

    public LiveData<List<InstallPackageModel>> getInstalled() {
        return installed;
    }

    public List<InstallPackageModel> getInstalledAll() {
        initInstalledPackage();
        return installed.getValue();
    }

    private void initInstalledPackage() {
        if (installed.getValue() != null) {
            return;
        }
        synchronized (installed) {
            if (installed.getValue() != null) {
                return;
            }
            AsyncTask.execute(() -> {
                Context context = XpApplication.getInstance();
                List<InstallPackageModel> list = new ArrayList<>();
                PackageManager pm = context.getPackageManager();
                Set<String> xposedes = new HashSet<>();
                List<PackageInfo> infos = pm.getInstalledPackages(PackageManager.GET_META_DATA);
                for (PackageInfo info : infos) {
                    InstallPackageModel item = new InstallPackageModel();
                    item.pkg = info.packageName;
                    item.appName = info.applicationInfo.loadLabel(pm).toString();
                    item.versionName = info.versionName;
                    item.versionCode = info.versionCode;
                    item.isXposedModule = isXposedModule(info.applicationInfo.metaData, info.applicationInfo.publicSourceDir);
                    item.icon = info.applicationInfo.loadIcon(pm);
                    item.isSystemApp = (info.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
                    list.add(item);
                    apks.put(item.pkg, item);
                    if (item.isXposedModule) {
                        xposedes.add(item.pkg);
                    }
                }
                Collections.sort(list);
                installed.postValue(list);
                SPProvider.setXposedList(context, xposedes);
            });
        }
    }

    private boolean isXposedModule(Bundle bundle, String path) {
        if (bundle != null) {
            if (bundle.getString("xposeddescription") != null) {
                return true;
            }
            if (bundle.getString("xposedminversion") != null) {
                return true;
            }
            if (bundle.getString("xposedmodule") != null) {
                return true;
            }
        }
        return false;
//        ZipFile zipFile = null;
//        try {
//            zipFile = new ZipFile(path);
//        } catch (IOException e) {
//            return false;
//        }
//        return zipFile.getEntry("assets/xposed_init") != null;
    }

    public InstallPackageModel getInstallApp(String pkg) {
        return apks.get(pkg);
    }

    public DataModelType getDataModelType() {
        return dataModelType;
    }

    public void setDataModelType(DataModelType dataModelType) {
        if (this.dataModelType != dataModelType) {
            this.dataModelType = dataModelType;
            dataSources.remove(dataModelType);
            data.setValue(null);
        }
    }

    public List<? extends ShowDataModel> getDataValue() {
        List<ShowDataModel> set = dataSources.get(dataModelType);
        if (set == null) {
            initDataModelByType();
            set = new LinkedList<>();
            dataSources.put(dataModelType, set);
        }
        return set;
    }

    public void setDataValue(List<ShowDataModel> value) {
        dataSources.put(dataModelType, value);
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            data.setValue(value);
        } else {
            data.postValue(value);
        }
    }

    public void updateDataValue() {
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            data.setValue(data.getValue());
        } else {
            data.postValue(data.getValue());
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends ShowDataModel> void addDataValue(T value, int index) {
        if (value != null) {
            List<ShowDataModel> values = (List<ShowDataModel>) getDataValue();
            if (index != NO_INDEX) {
                values.set(index, value);
                setDataValue(values);
            } else if (!values.contains(value)) {
                values.add(value);
                setDataValue(values);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void addDataValue(Collection<? extends ShowDataModel> value) {
        if (value != null && !value.isEmpty()) {
            List<ShowDataModel> values = (List<ShowDataModel>) getDataValue();
            values.addAll(value);
            setDataValue(values);
        }
    }
    @SuppressWarnings("unchecked")
    public void deleteDataValue(ShowDataModel value) {
        List<ShowDataModel> values = (List<ShowDataModel>) getDataValue();
        if (values.remove(value)) {
            setDataValue(values);
        }
    }

    public void setEditModelValue(ShowDataModel value, int index) {
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            editModel.setValue(new Pair<>(value, index));
        } else {
            editModel.postValue(new Pair<>(value, index));
        }
    }

    public void setEditModelObserver(LifecycleOwner owner, Observer<Pair<ShowDataModel, Integer>> observer) {
        editModel.observe(owner, editDataModel -> {
            if (editDataModel != null && editDataModel.first != null) {
                observer.onChanged(editDataModel);
            }
        });
    }

    public void setDataObserver(LifecycleOwner owner, Observer<List<? extends ShowDataModel>> observer) {
        data.observe(owner, showDataModels -> {
            if (showDataModels != null) {
                observer.onChanged(showDataModels);
            }
        });
    }

    public void removeDataObserver(Observer<List<? extends ShowDataModel>> observer) {
        data.removeObserver(observer);
    }

    public void setSaveObserver(boolean open) {
        if (open) {
            data.observeForever(saveObserver);
        } else {
            data.removeObserver(saveObserver);
        }
    }

    public void setSaveResult(boolean success) {
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            saveResult.setValue(success);
        } else {
            saveResult.postValue(success);
        }
    }

    public void setMessage(Integer resId) {
        setMessage(XpApplication.getInstance().getString(resId));
    }

    public void setMessage(String msg) {
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            messageData.setValue(msg);
        } else {
            messageData.postValue(msg);
        }
    }

    public void setSnackMessageObserver(LifecycleOwner owner, Observer<String> observer) {
        messageData.observe(owner, observer);
    }

    public void setSaveResultObserver(LifecycleOwner owner, Observer<Boolean> observer) {
        saveResult.observe(owner, aBoolean -> {
            if (aBoolean != null) {
                observer.onChanged(aBoolean);
            }
        });
    }

    public void updateHookApp(String pkg, Boolean value) {
        if (TextUtils.isEmpty(pkg)) {
            return;
        }
        Objects.requireNonNull(hookApps.getValue()).put(pkg, value);
        syncHookApp(pkg, value);
    }

    public void setHookAppsObserver(LifecycleOwner owner, Observer<Map<String, Boolean>> observer) {
        hookApps.observe(owner, map -> {
            if (map != null) {
                observer.onChanged(map);
            }
        });
    }

    public void setHookAppObserver(LifecycleOwner owner, Observer<Pair<String, Boolean>> observer) {
        hookApp.observe(owner, pair -> {
            if (pair != null) {
                observer.onChanged(pair);
            }
        });
    }

    public String getCurrentPackage() {
        return currentPackage;
    }

    public void setCurrentPackage(String currentPackage) {
        if (!TextUtils.equals(this.currentPackage, currentPackage)) {
            dataSources.clear();
            this.currentPackage = currentPackage;
        }
    }

    public void initDataModelByType() {
        if (dataSources.get(dataModelType) != null) {
            return;
        }
        synchronized (data) {
            if (dataSources.get(dataModelType) != null) {
                return;
            }
            if (currentPackage == null || dataModelType == null || dataModelType == DataModelType.NOTHING) {
                return;
            }
            AsyncTask.execute(() -> {
                List<ShowDataModel> values;
                if (SPProvider.appHasInit(XpApplication.getInstance(), currentPackage, dataModelType)) {
                    values = new ArrayList<>();
                    String value = SPProvider.getAppTypeValue(XpApplication.getInstance(), currentPackage, dataModelType);
                    if (!TextUtils.isEmpty(value)) {
                        try {
                            JSONArray array = new JSONArray(value);
                            Class<? extends ShowDataModel> clazz = dataModelType.valueType;
                            try {
                                int size = array.length();
                                for (int i = 0; i < size; i++) {
                                    JSONObject json = array.getJSONObject(i);
                                    ShowDataModel model = clazz.newInstance();
                                    model.unSerialization(json);
                                    values.add(model);
                                }
                            } catch (IllegalAccessException | InstantiationException e) {
                                LogUtil.e("Failed to initialize configuration data", e);
                            }
                        } catch (JSONException e) {
                            throw new RuntimeException("unSerialization string error, type: " + dataModelType.name(), e);
                        }
                    }
                } else {
                    values = GlobalConfig.getShowConfig(dataModelType);
                    if (dataModelType == DataModelType.PACKAGE_HIDE) {
                        Iterator<ShowDataModel> iter = values.iterator();
                        while (iter.hasNext()) {
                            PackageModel model = (PackageModel) iter.next();
                            InstallPackageModel installPackageModel = getInstallApp(model.getPackageName());
                            if (installPackageModel == null && !PackageModel.XPOSED_PACKAGE_MASK.equals(model.getPackageName())) {
                                iter.remove();
                            }
                        }
                    }
                }
                dataSources.put(dataModelType, values);
                data.postValue(values);
            });
        }
    }

    public void initHookApps() {
        if (hookApps.getValue() != null) {
            return;
        }
        synchronized (hookApps) {
            if (hookApps.getValue() != null) {
                return;
            }
            AsyncTask.execute(() -> hookApps.postValue(SPProvider.getHookApps(XpApplication.getInstance())));
        }
    }

    private void syncHookApp(String pkg, Boolean value) {
        SPProvider.setHookApp(XpApplication.getInstance(), pkg, value);
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            hookApp.setValue(new Pair<>(pkg, value));
        } else {
            hookApp.postValue(new Pair<>(pkg, value));
        }
    }
}