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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Wraps a result for web from calling a native plugin.
 */
public class PluginResult {

    private final JSObject json;

    public PluginResult() {
        this(new JSObject());
    }

    public PluginResult(JSObject json) {
        this.json = json;
    }

    public PluginResult put(String name, boolean value) {
        return this.jsonPut(name, value);
    }

    public PluginResult put(String name, double value) {
        return this.jsonPut(name, value);
    }

    public PluginResult put(String name, int value) {
        return this.jsonPut(name, value);
    }

    public PluginResult put(String name, long value) {
        return this.jsonPut(name, value);
    }

    /**
     * Format a date as an ISO string
     */
    public PluginResult put(String name, Date value) {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
        df.setTimeZone(tz);
        return this.jsonPut(name, df.format(value));
    }

    public PluginResult put(String name, Object value) {
        return this.jsonPut(name, value);
    }

    public PluginResult put(String name, PluginResult value) {
        return this.jsonPut(name, value.json);
    }

    PluginResult jsonPut(String name, Object value) {
        try {
            this.json.put(name, value);
        } catch (Exception ex) {
            Logger.error(Logger.tags("Plugin"), "", ex);
        }
        return this;
    }

    public String toString() {
        return this.json.toString();
    }

    /**
     * Return plugin metadata and information about the result, if it succeeded the data, or error information if it didn't.
     * This is used for appRestoredResult, as it's technically a raw data response from a plugin.
     * @return the raw data response from the plugin.
     */
    public JSObject getWrappedResult() {
        JSObject ret = new JSObject();
        ret.put("pluginId", this.json.getString("pluginId"));
        ret.put("methodName", this.json.getString("methodName"));
        ret.put("success", this.json.getBoolean("success", false));
        ret.put("data", this.json.getJSObject("data"));
        ret.put("error", this.json.getJSObject("error"));
        return ret;
    }
}
