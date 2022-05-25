/*
       Copyright (c) 2022 BlackBerry Limited. All Rights Reserved.
       Some modifications to the original capacitor-android project:
       https://github.com/ionic-team/capacitor/tree/main/android/capacitor/src/main/java/com/getcapacitor

       Licensed to the Apache Software Foundation (ASF) under one
       or more contributor license agreements.  See the NOTICE file
       distributed with this work for additional information
       regarding copyright ownership.  The ASF licenses this file
       to you under the Apache License, Version 2.0 (the
       "License"); you may not use this file except in compliance
       with the License.  You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

       Unless required by applicable law or agreed to in writing,
       software distributed under the License is distributed on an
       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
       KIND, either express or implied.  See the License for the
       specific language governing permissions and limitations
       under the License.
 */

package com.good.gd.cordova.capacitor;

import android.app.Activity;
import android.content.SharedPreferences;

@CapacitorPlugin
public class WebView extends Plugin {

    public static final String WEBVIEW_PREFS_NAME = "CapWebViewSettings";
    public static final String CAP_SERVER_PATH = "serverBasePath";

    @PluginMethod
    public void setServerBasePath(PluginCall call) {
        String path = call.getString("path");
        bridge.setServerBasePath(path);
        call.resolve();
    }

    @PluginMethod
    public void getServerBasePath(PluginCall call) {
        String path = bridge.getServerBasePath();
        JSObject ret = new JSObject();
        ret.put("path", path);
        call.resolve(ret);
    }

    @PluginMethod
    public void persistServerBasePath(PluginCall call) {
        String path = bridge.getServerBasePath();
        SharedPreferences prefs = getContext().getSharedPreferences(WEBVIEW_PREFS_NAME, Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(CAP_SERVER_PATH, path);
        editor.apply();
        call.resolve();
    }
}
