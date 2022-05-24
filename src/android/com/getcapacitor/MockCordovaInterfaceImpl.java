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

import android.util.Pair;
import androidx.appcompat.app.AppCompatActivity;
import java.util.concurrent.Executors;
import org.apache.cordova.CordovaInterfaceImpl;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONException;

public class MockCordovaInterfaceImpl extends CordovaInterfaceImpl {

    public MockCordovaInterfaceImpl(AppCompatActivity activity) {
        super(activity, Executors.newCachedThreadPool());
    }

    public CordovaPlugin getActivityResultCallback() {
        return this.activityResultCallback;
    }

    /**
     * Checks Cordova permission callbacks to handle permissions defined by a Cordova plugin.
     * Returns true if Cordova is handling the permission request with a registered code.
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     * @return true if Cordova handled the permission request, false if not
     */
    public boolean handlePermissionResult(int requestCode, String[] permissions, int[] grantResults) throws JSONException {
        Pair<CordovaPlugin, Integer> callback = permissionResultCallbacks.getAndRemoveCallback(requestCode);
        if (callback != null) {
            callback.first.onRequestPermissionResult(callback.second, permissions, grantResults);
            return true;
        }

        return false;
    }
}
