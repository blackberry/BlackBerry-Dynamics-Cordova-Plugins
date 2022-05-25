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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import com.good.gd.GDAndroid;
import com.good.gd.GDStateAction;
import com.good.gd.cordova.core.webview.engine.BBDCordovaWebChromeClient;
import com.good.gd.cordova.core.webview.engine.BBDCordovaWebViewEngine;

import java.util.List;
import java.util.Map;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPreferences;
import org.apache.cordova.CordovaResourceApi;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CordovaWebViewEngine;
import org.apache.cordova.ICordovaCookieManager;
import org.apache.cordova.NativeToJsMessageQueue;
import org.apache.cordova.PluginEntry;
import org.apache.cordova.PluginManager;
import org.apache.cordova.PluginResult;

public class MockCordovaWebViewImpl extends WebView implements CordovaWebView {

    private Context context;
    private PluginManager pluginManager;
    private CordovaPreferences preferences;
    private CordovaResourceApi resourceApi;
    private NativeToJsMessageQueue nativeToJsMessageQueue;
    private CordovaInterface cordova;
    private CapacitorCordovaCookieManager cookieManager;
    private WebView webView;
    private boolean hasPausedEver;

    private BBDCordovaWebViewEngine.BBDCordovaWebViewClient viewClient;
    BBDCordovaWebChromeClient chromeClient;

    public MockCordovaWebViewImpl(Context context) {
      this(context, null);
      this.context = context;
      // set BroadcastReceiver just to fire JS event
      GDAndroid.getInstance().registerReceiver(bbdStateReceiver, getIntentFilter());
    }

    public MockCordovaWebViewImpl(Context context, AttributeSet attrs) {
    super(context, attrs);
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

    private void fireJavaScriptDOMEvent(String eventName) {
      String codeToExecute = "(function(){\n\t" +
        "var __event__ = document.createEvent('HTMLEvents');\n\t" +
        "__event__.initEvent('" + eventName + "', true, true);\n\t" +
        "document.dispatchEvent(__event__);\n" +
        "})();";

      evaluateJavascript(codeToExecute, null);
    }

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

    @Override
    public void init(CordovaInterface cordova, List<PluginEntry> pluginEntries, CordovaPreferences preferences) {
        this.cordova = cordova;
        this.preferences = preferences;
        this.pluginManager = new PluginManager(this, this.cordova, pluginEntries);
        this.resourceApi = new CordovaResourceApi(this.context, this.pluginManager);
        this.pluginManager.init();
    }

    public void init(CordovaInterface cordova, List<PluginEntry> pluginEntries, CordovaPreferences preferences, WebView webView) {
        this.cordova = cordova;
        this.webView = webView;
        this.preferences = preferences;
        this.pluginManager = new PluginManager(this, this.cordova, pluginEntries);
        this.resourceApi = new CordovaResourceApi(this.context, this.pluginManager);
        nativeToJsMessageQueue = new NativeToJsMessageQueue();
        nativeToJsMessageQueue.addBridgeMode(new CapacitorEvalBridgeMode(webView, this.cordova));
        nativeToJsMessageQueue.setBridgeMode(0);
        this.cookieManager = new CapacitorCordovaCookieManager(webView);
        this.pluginManager.init();
    }

    public static class CapacitorEvalBridgeMode extends NativeToJsMessageQueue.BridgeMode {

        private final WebView webView;
        private final CordovaInterface cordova;

        public CapacitorEvalBridgeMode(WebView webView, CordovaInterface cordova) {
            this.webView = webView;
            this.cordova = cordova;
        }

        @Override
        public void onNativeToJsMessageAvailable(final NativeToJsMessageQueue queue) {
            cordova
                .getActivity()
                .runOnUiThread(
                    () -> {
                        String js = queue.popAndEncodeAsJs();
                        if (js != null) {
                            webView.evaluateJavascript(js, null);
                        }
                    }
                );
        }
    }

    @Override
    public boolean isInitialized() {
        return cordova != null;
    }

    @Override
    public View getView() {
        return this.webView;
    }

    @Override
    public void loadUrlIntoView(String url, boolean recreatePlugins) {
        if (url.equals("about:blank") || url.startsWith("javascript:")) {
            webView.loadUrl(url);
            return;
        }
    }

    @Override
    public void stopLoading() {}

    @Override
    public boolean canGoBack() {
        return false;
    }

    @Override
    public void clearCache() {}

    @Override
    public void clearCache(boolean b) {}

    @Override
    public void clearHistory() {}

    @Override
    public boolean backHistory() {
        return false;
    }

    @Override
    public void handlePause(boolean keepRunning) {
        if (!isInitialized()) {
            return;
        }
        hasPausedEver = true;
        pluginManager.onPause(keepRunning);
        triggerDocumentEvent("pause");
        // If app doesn't want to run in background
        if (!keepRunning) {
            // Pause JavaScript timers. This affects all webviews within the app!
            this.setPaused(true);
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        if (this.pluginManager != null) {
            this.pluginManager.onNewIntent(intent);
        }
    }

    @Override
    public void handleResume(boolean keepRunning) {
        if (!isInitialized()) {
            return;
        }
        this.setPaused(false);
        this.pluginManager.onResume(keepRunning);
        if (hasPausedEver) {
            triggerDocumentEvent("resume");
        }
    }

    @Override
    public void handleStart() {
        if (!isInitialized()) {
            return;
        }
        pluginManager.onStart();
    }

    @Override
    public void handleStop() {
        if (!isInitialized()) {
            return;
        }
        pluginManager.onStop();
    }

    @Override
    public void handleDestroy() {
        if (!isInitialized()) {
            return;
        }
        this.pluginManager.onDestroy();
    }

    @Override
    public void sendJavascript(String statememt) {
        nativeToJsMessageQueue.addJavaScript(statememt);
    }

    public void eval(final String js, final ValueCallback<String> callback) {
        Handler mainHandler = new Handler(context.getMainLooper());
        mainHandler.post(() -> webView.evaluateJavascript(js, callback));
    }

    public void triggerDocumentEvent(final String eventName) {
        eval("window.Capacitor.triggerEvent('" + eventName + "', 'document');", s -> {});
    }

    @Override
    public void showWebPage(String url, boolean openExternal, boolean clearHistory, Map<String, Object> params) {}

    @Override
    public boolean isCustomViewShowing() {
        return false;
    }

    @Override
    public void showCustomView(View view, WebChromeClient.CustomViewCallback callback) {}

    @Override
    public void hideCustomView() {}

    @Override
    public CordovaResourceApi getResourceApi() {
        return this.resourceApi;
    }

    @Override
    public void setButtonPlumbedToJs(int keyCode, boolean override) {}

    @Override
    public boolean isButtonPlumbedToJs(int keyCode) {
        return false;
    }

    @Override
    public void sendPluginResult(PluginResult cr, String callbackId) {
        nativeToJsMessageQueue.addPluginResult(cr, callbackId);
    }

    @Override
    public PluginManager getPluginManager() {
        return this.pluginManager;
    }

    @Override
    public CordovaWebViewEngine getEngine() {
        return null;
    }

    @Override
    public CordovaPreferences getPreferences() {
        return this.preferences;
    }

    @Override
    public ICordovaCookieManager getCookieManager() {
        return cookieManager;
    }

    @Override
    public String getUrl() {
        return webView.getUrl();
    }

    @Override
    public void loadUrl(String url) {
      super.loadUrl(url);
    }

    @Override
    public Object postMessage(String id, Object data) {
        return pluginManager.postMessage(id, data);
    }

    public void setPaused(boolean value) {
        if (value) {
            webView.onPause();
            webView.pauseTimers();
        } else {
            webView.onResume();
            webView.resumeTimers();
        }
    }
}
