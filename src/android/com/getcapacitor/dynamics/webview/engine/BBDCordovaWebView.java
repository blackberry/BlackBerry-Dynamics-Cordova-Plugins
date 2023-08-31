/*
       Copyright (c) 2023 BlackBerry Limited. All Rights Reserved.

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

package com.getcapacitor.core.webview.engine;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.webkit.WebChromeClient;
import android.webkit.WebViewClient;

import com.good.gd.GDAndroid;
import com.good.gd.GDStateAction;
import com.blackberry.bbwebview.BBWebView;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CordovaWebViewEngine;

public class BBDCordovaWebView extends BBWebView implements CordovaWebViewEngine.EngineView {
    private static final String TAG = "BBDCordovaWebView-" + BBDCordovaWebView.class.getSimpleName();

    private BBDCordovaWebViewEngine.BBDCordovaWebViewClient viewClient;
    BBDCordovaWebChromeClient chromeClient;
    private BBDCordovaWebViewEngine parentEngine;
    private CordovaInterface cordova;
    private Context context;

    public BBDCordovaWebView(Context context) {
        this(context, null);
        this.context = context;
        // set BroadcastReceiver just to fire JS event
        GDAndroid.getInstance().registerReceiver(bbdStateReceiver, getIntentFilter());
    }

    public BBDCordovaWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    // Package visibility to enforce that only SystemWebViewEngine should call this method.
    void init(BBDCordovaWebViewEngine parentEngine, CordovaInterface cordova) {
        this.cordova = cordova;
        this.parentEngine = parentEngine;
        if (this.viewClient == null) {
            setWebViewClient(new BBDCordovaWebViewEngine.BBDCordovaWebViewClient(getContext()));
        }

        if (this.chromeClient == null) {
            setWebChromeClient(new BBDCordovaWebChromeClient(parentEngine));
        }
    }

    @Override
    public CordovaWebView getCordovaWebView() {
        return parentEngine != null ? parentEngine.getCordovaWebView() : null;
    }

    @Override
    public void setWebViewClient(WebViewClient client) {
        viewClient = new BBDCordovaWebViewEngine.BBDCordovaWebViewClient(getContext());
        super.setWebViewClient(viewClient);
    }

    @Override
    public void loadUrl(String url) {

        super.loadUrl(url);
    }

    @Override
    public void setWebChromeClient(WebChromeClient client) {
        if (client instanceof BBDCordovaWebChromeClient) {
            chromeClient = (BBDCordovaWebChromeClient) client;
            super.setWebChromeClient(chromeClient);
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        Boolean ret = parentEngine != null ? parentEngine.client.onDispatchKeyEvent(event) : null;
        if (ret != null) {
            return ret.booleanValue();
        }
        return super.dispatchKeyEvent(event);
    }

    private BroadcastReceiver bbdStateReceiver = new BroadcastReceiver() {
        private String RECEIVER_TAG = "bbdStateReceiver";

        @Override
        /**
         * This method can be used to fire some custom JavaScript event on DOM objects
         * The same approach was used in spike GD-30095 for iOS. An implementation story GD-33311 will provide
         * a mechanism to trigger JavaScript events based on GDAppEvent on iOS. Please see GD-33243.
         *
         * A few additional stories for Android will be created.
         */
        public void onReceive(Context context, Intent intent) {
            String stateAction = intent.getAction();
            Log.d(RECEIVER_TAG, "Received intent action: " + stateAction);

            switch (stateAction) {
                case GDStateAction.GD_STATE_AUTHORIZED_ACTION:
                    Log.d(RECEIVER_TAG, GDStateAction.GD_STATE_AUTHORIZED_ACTION + " intent action received");

                    break;
                case GDStateAction.GD_STATE_LOCKED_ACTION:
                    Log.d(RECEIVER_TAG, GDStateAction.GD_STATE_LOCKED_ACTION + " intent action received");

                    break;

                case GDStateAction.GD_STATE_WIPED_ACTION:
                    Log.d(RECEIVER_TAG, GDStateAction.GD_STATE_WIPED_ACTION + " intent action received");

                    break;

                case GDStateAction.GD_STATE_UPDATE_POLICY_ACTION:
                    Log.d(RECEIVER_TAG, GDStateAction.GD_STATE_UPDATE_POLICY_ACTION + " intent action received");

                    fireJavaScriptDOMEvent(stateAction);

                    break;

                default:
                    fireJavaScriptDOMEvent(stateAction);

                    break;

            }
        }
    };

    private IntentFilter getIntentFilter() {
        IntentFilter intentFilter = new IntentFilter();

        intentFilter.addAction(GDStateAction.GD_STATE_AUTHORIZED_ACTION);
        intentFilter.addAction(GDStateAction.GD_STATE_LOCKED_ACTION);
        intentFilter.addAction(GDStateAction.GD_STATE_WIPED_ACTION);
        intentFilter.addAction(GDStateAction.GD_STATE_UPDATE_POLICY_ACTION);
        intentFilter.addAction(GDStateAction.GD_STATE_UPDATE_SERVICES_ACTION);
        intentFilter.addAction(GDStateAction.GD_STATE_UPDATE_CONFIG_ACTION);
        intentFilter.addAction(GDStateAction.GD_STATE_UPDATE_ENTITLEMENTS_ACTION);

        return intentFilter;
    }

    private void fireJavaScriptDOMEvent(String eventName) {
        String codeToExecute = "(function(){\n\t" +
                "var __event__ = document.createEvent('HTMLEvents');\n\t" +
                "__event__.initEvent('" + eventName + "', true, true);\n\t" +
                "document.dispatchEvent(__event__);\n" +
                "})();";

        evaluateJavascript(codeToExecute, null);
    }
}
