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

import android.graphics.Bitmap;
import android.net.Uri;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import java.util.List;

public class BridgeWebViewClient extends WebViewClient {

    private Bridge bridge;

    public BridgeWebViewClient(Bridge bridge) {
        this.bridge = bridge;
    }

    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        return bridge.getLocalServer().shouldInterceptRequest(request);
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        Uri url = request.getUrl();
        return bridge.launchIntent(url);
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        return bridge.launchIntent(Uri.parse(url));
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
        List<WebViewListener> webViewListeners = bridge.getWebViewListeners();

        if (webViewListeners != null && view.getProgress() == 100) {
            for (WebViewListener listener : bridge.getWebViewListeners()) {
                listener.onPageLoaded(view);
            }
        }
    }

    @Override
    public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
        super.onReceivedError(view, request, error);

        List<WebViewListener> webViewListeners = bridge.getWebViewListeners();
        if (webViewListeners != null) {
            for (WebViewListener listener : bridge.getWebViewListeners()) {
                listener.onReceivedError(view);
            }
        }
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        super.onPageStarted(view, url, favicon);
        bridge.reset();
        List<WebViewListener> webViewListeners = bridge.getWebViewListeners();

        if (webViewListeners != null) {
            for (WebViewListener listener : bridge.getWebViewListeners()) {
                listener.onPageStarted(view);
            }
        }
    }

    @Override
    public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
        super.onReceivedHttpError(view, request, errorResponse);

        List<WebViewListener> webViewListeners = bridge.getWebViewListeners();
        if (webViewListeners != null) {
            for (WebViewListener listener : bridge.getWebViewListeners()) {
                listener.onReceivedHttpError(view);
            }
        }
    }
}
