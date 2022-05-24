/*
       Copyright (c) 2022 BlackBerry Limited. All Rights Reserved.

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

package com.good.gd.cordova.plugins.helpers;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;
import org.json.JSONObject;

public class GDHttpRequestPluginHelper {

    public static void sendSuccessPluginResult(final String message,
                                               final CallbackContext callbackContext) {

        final PluginResult result = new PluginResult(PluginResult.Status.OK, message);
        callbackContext.sendPluginResult(result);
    }

    public static void sendErrorPluginResult(final String message,
                                             final CallbackContext callbackContext) {

        final PluginResult result = new PluginResult(PluginResult.Status.ERROR, message);
        callbackContext.sendPluginResult(result);
    }

    public static void sendSuccessPluginResult(final JSONObject object,
                                               final CallbackContext callbackContext) {
        final String response = object.toString();
        sendSuccessPluginResult(response, callbackContext);
    }

    public static void sendFinalSuccessPluginResult(final JSONObject jsonObject,
                                                    final CallbackContext callbackContext) {
        final String response = jsonObject.toString();
        final PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, response);
        pluginResult.setKeepCallback(false);
        callbackContext.sendPluginResult(pluginResult);
    }

    public static void sendFinalErrorPluginResult(final JSONObject jsonObject,
                                                  final CallbackContext callbackContext) {
        final String response = jsonObject.toString();
        final PluginResult pluginResult = new PluginResult(PluginResult.Status.ERROR, response);
        pluginResult.setKeepCallback(false);
        callbackContext.sendPluginResult(pluginResult);
    }

    public static void sendFinalErrorPluginResult(final String message,
                                                  final CallbackContext callbackContext) {
        final PluginResult pluginResult = new PluginResult(PluginResult.Status.ERROR, message);
        pluginResult.setKeepCallback(false);
        callbackContext.sendPluginResult(pluginResult);
    }

    public static void sendNonFinalSuccessPluginResult(final JSONObject jsonObject,
                                                       final CallbackContext callbackContext) {
        final String response = jsonObject.toString();
        final PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, response);
        pluginResult.setKeepCallback(true);
        callbackContext.sendPluginResult(pluginResult);
    }

    public static void sendNonFinalSuccessPluginResult(final String response,
                                                       final CallbackContext callbackContext) {
        final PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, response);
        pluginResult.setKeepCallback(true);
        callbackContext.sendPluginResult(pluginResult);
    }
}
