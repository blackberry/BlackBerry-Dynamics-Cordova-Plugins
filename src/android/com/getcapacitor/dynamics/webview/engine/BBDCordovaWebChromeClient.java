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

package com.good.gd.cordova.core.webview.engine;

import android.webkit.JsPromptResult;
import android.webkit.WebView;

import com.blackberry.bbwebview.BBWebChromeClient;

public class BBDCordovaWebChromeClient extends BBWebChromeClient {
    protected final BBDCordovaWebViewEngine parentEngine;

    public BBDCordovaWebChromeClient(BBDCordovaWebViewEngine parentEngine) {
        this.parentEngine = parentEngine;
    }

    @Override
    public boolean onConsoleMessage(android.webkit.ConsoleMessage consoleMessage) {
        return super.onConsoleMessage(consoleMessage);
    }

    @Override
    public boolean onJsPrompt(WebView view, String url, String message, String defaultValue, JsPromptResult result) {
        String handledRet = parentEngine.bridge.promptOnJsPrompt(url, message, defaultValue);
        if (handledRet != null) {
            result.confirm(handledRet);
        } else {
            super.onJsPrompt(view, url, message, defaultValue, result);
        }

        return true;
    }

    @Override
    public void onProgressChanged(WebView view, int newProgress) {
        super.onProgressChanged(view, newProgress);
    }
}
