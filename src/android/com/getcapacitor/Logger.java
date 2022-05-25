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

import android.text.TextUtils;
import android.util.Log;

public class Logger {

    public static final String LOG_TAG_CORE = "Capacitor";
    public static CapConfig config;

    private static Logger instance;

    private static Logger getInstance() {
        if (instance == null) {
            instance = new Logger();
        }
        return instance;
    }

    public static void init(CapConfig config) {
        Logger.getInstance().loadConfig(config);
    }

    private void loadConfig(CapConfig config) {
        Logger.config = config;
    }

    public static String tags(String... subtags) {
        if (subtags != null && subtags.length > 0) {
            return LOG_TAG_CORE + "/" + TextUtils.join("/", subtags);
        }

        return LOG_TAG_CORE;
    }

    public static void verbose(String message) {
        verbose(LOG_TAG_CORE, message);
    }

    public static void verbose(String tag, String message) {
        if (!shouldLog()) {
            return;
        }

        Log.v(tag, message);
    }

    public static void debug(String message) {
        debug(LOG_TAG_CORE, message);
    }

    public static void debug(String tag, String message) {
        if (!shouldLog()) {
            return;
        }

        Log.d(tag, message);
    }

    public static void info(String message) {
        info(LOG_TAG_CORE, message);
    }

    public static void info(String tag, String message) {
        if (!shouldLog()) {
            return;
        }

        Log.i(tag, message);
    }

    public static void warn(String message) {
        warn(LOG_TAG_CORE, message);
    }

    public static void warn(String tag, String message) {
        if (!shouldLog()) {
            return;
        }

        Log.w(tag, message);
    }

    public static void error(String message) {
        error(LOG_TAG_CORE, message, null);
    }

    public static void error(String message, Throwable e) {
        error(LOG_TAG_CORE, message, e);
    }

    public static void error(String tag, String message, Throwable e) {
        if (!shouldLog()) {
            return;
        }

        Log.e(tag, message, e);
    }

    protected static boolean shouldLog() {
        return config == null || config.isLoggingEnabled();
    }
}
