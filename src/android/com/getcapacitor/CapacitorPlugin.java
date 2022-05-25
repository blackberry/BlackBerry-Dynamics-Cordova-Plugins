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

import android.content.Intent;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Base annotation for all Plugins
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface CapacitorPlugin {
    /**
     * A custom name for the plugin, otherwise uses the
     * simple class name.
     */
    String name() default "";

    /**
     * Request codes this plugin uses and responds to, in order to tie
     * Android events back the plugin to handle.
     *
     * NOTE: This is a legacy option provided to support third party libraries
     * not currently implementing the new AndroidX Activity Results API. Plugins
     * without this limitation should use a registered callback with
     * {@link com.good.gd.cordova.capacitor.Plugin#startActivityForResult(PluginCall, Intent, String)}
     */
    int[] requestCodes() default {};

    /**
     * Permissions this plugin needs, in order to make permission requests
     * easy if the plugin only needs basic permission prompting
     */
    Permission[] permissions() default {};
}
