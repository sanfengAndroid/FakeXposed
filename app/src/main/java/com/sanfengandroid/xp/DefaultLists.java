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

import com.sanfengandroid.common.bean.EnvBean;
import com.sanfengandroid.common.bean.ExecBean;
import com.sanfengandroid.fakeinterface.MapsMode;

import java.util.ArrayList;
import java.util.List;

/**
 * @author sanfengAndroid
 * @date 2020/11/01
 */
public class DefaultLists {
    public static final String[] DEFAULT_APPS_LIST = {"de.robv.android.xposed.installer", "org.meowcat.edxposed.manager", "com.fde.DomesticDigitalCopy", "com.directv.application.android.go.production", "com.res.bby", "dk.excitor.dmemail", "com.BHTV", "com.bradfordnetworks.bma", "com.apriva.mobile.bams", "com.apriva.mobile.aprivapay", "pl.pkobp.iko", "au.com.auspost", "com.rogers.citytv.phone", "com.zenprise", "net.flixster.android", "com.starfinanz.smob.android.sfinanzstatus", "com.ovidos.yuppi", "klb.android.lovelive", "klb.android.lovelive_en", "com.nintendo.zaaa", "com.incube.epub", "com.airwatch.androidagent", "com.zappware.twintv.d3", "com.starfinanz.mobile.android.pushtan", "com.stofa.webtv", "com.barclays.android.barclaysmobilebanking", "com.bskyb.skygo", "com.hanaskcard.rocomo.potal", "com.hanabank.ebk.channel.android.hananbank", "com.ahnlab.v3mobileplus", "com.good.android.gfe", "it.phoenixspa.inbank", "dk.tv2.tv2play", "com.enterproid.divideinstaller", "com.isis.mclient.verizon.activity", "com.isis.mclient.atnt.activity", "be.telenet.yelo", "no.rdml.android.mobiletv", "uk.co.barclays.barclayshomeowner", "com.mcafee.apps.emmagent", "com.virginmedia.tvanywhere", "com.amis.mobiatv", "it.telecomitalia.cubovision", "nl.ziggo.android.tv", "com.orange.fr.ocs", "com.adb.android.app.iti", "com.mobileiron"};
    public static final String[] DEFAULT_KEYWORD_LIST = {"supersu", "superuser", "Superuser", "noshufou", "xposed", "rootcloak", "chainfire", "titanium", "Titanium", "substrate", "greenify", "daemonsu", "root", "busybox", "titanium", ".tmpsu", "su", "rootcloak2"};
    public static final String[] DEFAULT_FILES_LIST = {"su", "daemonsu", "superuser.apk", "ZUPERFAKEFILE", "xposed"};
    public static final String[] DEFAULT_SYMBOL_LIST = {"riru_is_zygote_methods_replaced", "riru_get_version"};
    public static final String[] DEFAULT_CLASS_LIST = {"de.robv.android.xposed.XposedBridge", "de.robv.android.xposed.XposedHelpers"};
    public static final String[] DEFAULT_STACK_LIST = DEFAULT_CLASS_LIST;
    @SuppressWarnings("unchecked")
    public static final Pair<String, String>[] DEFAULT_SYSTEM_PROP_LIST = new Pair[]{new Pair<>("vxp", "")};
    @SuppressWarnings("unchecked")
    public static final Pair<String, String>[] DEFAULT_GLOBAL_PROPERTY_LIST = new Pair[]{new Pair<>("ro.build.selinux", "1")};
    public static final EnvBean[] DEFAULT_SYSTEM_ENV_LIST;
    @SuppressWarnings("unchecked")
    public static final Pair<String, String>[] DEFAULT_GLOBAL_VARIABLE_LIST = new Pair[]{new Pair<>("adb_enabled", "0"), new Pair<>("development_settings_enabled", "0")};
    @SuppressWarnings("unchecked")
    public static final Pair<String, String>[] DEFAULT_MAPS_RULE_LIST = new Pair[]{new Pair("XposedBridge", MapsMode.MM_REMOVE.key),
            new Pair("libmemtrack_real.so", MapsMode.MM_REMOVE.key)};

    public static final Pair<String, List<ExecBean>>[] DEFAULT_RUNTIME_LIST;

    public static final String EDXPOSED_PACKAGE = "org.meowcat.edxposed.manager";

    static {
        EnvBean bean = new EnvBean("CLASSPATH", "XposedBridge");
        DEFAULT_SYSTEM_ENV_LIST = new EnvBean[]{bean};
        ExecBean rbean = new ExecBean();
        rbean.oldCmd = "ls";
        rbean.oldArgv = "/system/lib";
        rbean.matchArgv = true;
        rbean.inputStream = "fake exec ls /system/lib";
        rbean.transform();
        List<ExecBean> list = new ArrayList<>();
        list.add(rbean);

        DEFAULT_RUNTIME_LIST = new Pair[]{new Pair<>(rbean.oldCmd, list)};
    }
}
