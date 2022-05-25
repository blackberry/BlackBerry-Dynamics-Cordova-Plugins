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

import android.webkit.WebView;

/**
 * Provides callbacks associated with the {@link BridgeWebViewClient}
 */
public abstract class WebViewListener {

    /**
     * Callback for page load event.
     *
     * @param webView The WebView that loaded
     */
    public void onPageLoaded(WebView webView) {
        // Override me to add behavior to the page loaded event
    }

    /**
     * Callback for onReceivedError event.
     *
     * @param webView The WebView that loaded
     */
    public void onReceivedError(WebView webView) {
        // Override me to add behavior to handle the onReceivedError event
    }

    /**
     * Callback for onReceivedHttpError event.
     *
     * @param webView The WebView that loaded
     */
    public void onReceivedHttpError(WebView webView) {
        // Override me to add behavior to handle the onReceivedHttpError event
    }

    /**
     * Callback for page start event.
     *
     * @param webView The WebView that loaded
     */
    public void onPageStarted(WebView webView) {
        // Override me to add behavior to the page started event
    }
}
