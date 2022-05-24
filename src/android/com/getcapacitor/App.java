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

import androidx.annotation.Nullable;

public class App {

    /**
     * Interface for callbacks when app status changes.
     */
    public interface AppStatusChangeListener {
        void onAppStatusChanged(Boolean isActive);
    }

    /**
     * Interface for callbacks when app is restored with pending plugin call.
     */
    public interface AppRestoredListener {
        void onAppRestored(PluginResult result);
    }

    @Nullable
    private AppStatusChangeListener statusChangeListener;

    @Nullable
    private AppRestoredListener appRestoredListener;

    private boolean isActive = false;

    public boolean isActive() {
        return isActive;
    }

    /**
     * Set the object to receive callbacks.
     * @param listener
     */
    public void setStatusChangeListener(@Nullable AppStatusChangeListener listener) {
        this.statusChangeListener = listener;
    }

    /**
     * Set the object to receive callbacks.
     * @param listener
     */
    public void setAppRestoredListener(@Nullable AppRestoredListener listener) {
        this.appRestoredListener = listener;
    }

    protected void fireRestoredResult(PluginResult result) {
        if (appRestoredListener != null) {
            appRestoredListener.onAppRestored(result);
        }
    }

    public void fireStatusChange(boolean isActive) {
        this.isActive = isActive;
        if (statusChangeListener != null) {
            statusChangeListener.onAppStatusChanged(isActive);
        }
    }
}
