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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.graphics.Bitmap;
import android.net.MailTo;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;

import androidx.annotation.RequiresApi;
import androidx.webkit.WebViewAssetLoader;

import com.good.gd.cordova.capacitor.Bridge;
import com.good.gd.cordova.capacitor.WebViewListener;
import com.good.gd.cordova.core.mailto.MailToInterceptor;
import com.good.gd.cordova.capacitor.WebViewLocalServer;
import com.blackberry.bbwebview.BBWebViewClient;

import org.apache.cordova.ConfigXmlParser;
import org.apache.cordova.CordovaBridge;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPreferences;
import org.apache.cordova.CordovaResourceApi;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CordovaWebViewEngine;
import org.apache.cordova.ICordovaCookieManager;
import org.apache.cordova.LOG;
import org.apache.cordova.NativeToJsMessageQueue;
import org.apache.cordova.PluginManager;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.good.gd.apachehttp.entity.ContentType.TEXT_HTML;

public class BBDCordovaWebViewEngine implements CordovaWebViewEngine {
    public static final String TAG = "BBDCordovaWebViewEngine";

    private static WebViewLocalServer localServer;
    private static String CDV_LOCAL_SERVER;
    private static final String LAST_BINARY_VERSION_CODE = "lastBinaryVersionCode";
    private static final String LAST_BINARY_VERSION_NAME = "lastBinaryVersionName";
    public static final String WEBVIEW_PREFS_NAME = "WebViewSettings";
    public static final String CDV_SERVER_PATH = "serverBasePath";

    protected WebView webView;
    protected static CordovaPreferences preferences;
    protected CordovaBridge bridge;
    protected CordovaWebViewEngine.Client client;
    protected CordovaWebView parentWebView;
    protected CordovaInterface cordova;
    protected PluginManager pluginManager;
    protected CordovaResourceApi resourceApi;
    protected NativeToJsMessageQueue nativeToJsMessageQueue;
    private BroadcastReceiver receiver;
    private static Bridge capacitorBridge;

    /** Used when created via reflection. */
    public BBDCordovaWebViewEngine(Context context, CordovaPreferences preferences, WebViewLocalServer localServer, Bridge bridge) {
        this(new BBDCordovaWebView(context), preferences, localServer, bridge);
        this.localServer = localServer;
    }

    public BBDCordovaWebViewEngine(BBDCordovaWebView webView, CordovaPreferences preferences, WebViewLocalServer localServer, Bridge bridge) {
        this.preferences = preferences;
        this.webView = webView;
        this.localServer = localServer;
        this.capacitorBridge = bridge;
        CDV_LOCAL_SERVER = "http://localhost/";
    }

    @Override
    public void init(CordovaWebView parentWebView, CordovaInterface cordova, final CordovaWebViewEngine.Client client,
                     CordovaResourceApi resourceApi, PluginManager pluginManager,
                     NativeToJsMessageQueue nativeToJsMessageQueue) {

        initWebView(parentWebView, cordova, client, resourceApi, pluginManager, nativeToJsMessageQueue);
        SharedPreferences prefs = cordova.getActivity().getApplicationContext().getSharedPreferences(WEBVIEW_PREFS_NAME, Activity.MODE_PRIVATE);
        String path = prefs.getString(CDV_SERVER_PATH, null);
        if (!isDeployDisabled() && !isNewBinary() && path != null && !path.isEmpty()) {
            setServerBasePath(path);
        }
    }

    private void initWebView(CordovaWebView parentWebView, CordovaInterface cordova, CordovaWebViewEngine.Client client,
                     CordovaResourceApi resourceApi, PluginManager pluginManager,
                     NativeToJsMessageQueue nativeToJsMessageQueue) {
        if (this.cordova != null) {
            throw new IllegalStateException();
        }
        // Needed when prefs are not passed by the constructor
        if (preferences == null) {
            preferences = parentWebView.getPreferences();
        }
        this.parentWebView = parentWebView;
        this.cordova = cordova;
        this.client = client;
        this.resourceApi = resourceApi;
        this.pluginManager = pluginManager;
        this.nativeToJsMessageQueue = nativeToJsMessageQueue;

        initWebViewSettings();

        nativeToJsMessageQueue.addBridgeMode(new NativeToJsMessageQueue.OnlineEventsBridgeMode(new NativeToJsMessageQueue.OnlineEventsBridgeMode.OnlineEventsBridgeModeDelegate() {
            @Override
            public void setNetworkAvailable(boolean value) {
                //sometimes this can be called after calling webview.destroy() on destroy()
                //thus resulting in a NullPointerException
                if(webView!=null) {
                    webView.setNetworkAvailable(value);
                }
            }
            @Override
            public void runOnUiThread(Runnable r) {
                BBDCordovaWebViewEngine.this.cordova.getActivity().runOnUiThread(r);
            }
        }));
        nativeToJsMessageQueue.addBridgeMode(new NativeToJsMessageQueue.EvalBridgeMode(this, cordova));
        bridge = new CordovaBridge(pluginManager, nativeToJsMessageQueue);
        exposeJsInterface(webView, bridge);
    }

    @SuppressLint({"NewApi", "SetJavaScriptEnabled"})
    @SuppressWarnings("deprecation")
    private void initWebViewSettings() {
        webView.setInitialScale(0);
        webView.setVerticalScrollBarEnabled(false);
        // Enable JavaScript
        final WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NORMAL);

        String manufacturer = android.os.Build.MANUFACTURER;
        LOG.d(TAG, "CordovaWebView is running on device made by: " + manufacturer);

        //We don't save any form data in the application
        settings.setSaveFormData(false);
        settings.setSavePassword(false);

        // Jellybean rightfully tried to lock this down. Too bad they didn't give us a whitelist
        // while we do this
        settings.setAllowUniversalAccessFromFileURLs(true);
        settings.setMediaPlaybackRequiresUserGesture(false);

        // Enable database
        // We keep this disabled because we use or shim to get around DOM_EXCEPTION_ERROR_16
        String databasePath = webView.getContext().getApplicationContext().getDir("database", Context.MODE_PRIVATE).getPath();
        settings.setDatabaseEnabled(true);
        settings.setDatabasePath(databasePath);


        //Determine whether we're in debug or release mode, and turn on Debugging!
        ApplicationInfo appInfo = webView.getContext().getApplicationContext().getApplicationInfo();
        if ((appInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            enableRemoteDebugging();
        }

        settings.setGeolocationDatabasePath(databasePath);

        // Enable DOM storage
        settings.setDomStorageEnabled(true);

        // Enable built-in geolocation
        settings.setGeolocationEnabled(true);

        // Enable AppCache
        // Fix for CB-2282
        settings.setAppCacheMaxSize(5 * 1048576);
        settings.setAppCachePath(databasePath);
        settings.setAppCacheEnabled(true);

        // enabling "http://" scheme after moving form "file://android_asset/" to "https://appassets.androidplatform.net/assets"
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        // Fix for CB-1405
        // Google issue 4641
        String defaultUserAgent = settings.getUserAgentString();

        // Fix for CB-3360
        String overrideUserAgent = preferences.getString("OverrideUserAgent", null);
        if (overrideUserAgent != null) {
            settings.setUserAgentString(overrideUserAgent);
        } else {
            String appendUserAgent = preferences.getString("AppendUserAgent", null);
            if (appendUserAgent != null) {
                settings.setUserAgentString(defaultUserAgent + " " + appendUserAgent);
            }
        }
        // End CB-3360

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_CONFIGURATION_CHANGED);
        if (this.receiver == null) {
            this.receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    settings.getUserAgentString();
                }
            };
            webView.getContext().registerReceiver(this.receiver, intentFilter);
        }
        // end CB-1405
    }

    private boolean isNewBinary() {
        String versionCode = "";
        String versionName = "";
        SharedPreferences prefs = cordova.getActivity().getApplicationContext().getSharedPreferences(WEBVIEW_PREFS_NAME, Activity.MODE_PRIVATE);
        String lastVersionCode = prefs.getString(LAST_BINARY_VERSION_CODE, null);
        String lastVersionName = prefs.getString(LAST_BINARY_VERSION_NAME, null);

        try {
            PackageInfo pInfo = this.cordova.getActivity().getPackageManager().getPackageInfo(this.cordova.getActivity().getPackageName(), 0);
            versionCode = Integer.toString(pInfo.versionCode);
            versionName = pInfo.versionName;
        } catch(Exception ex) {
            Log.e(TAG, "Unable to get package info", ex);
        }

        if (!versionCode.equals(lastVersionCode) || !versionName.equals(lastVersionName)) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(LAST_BINARY_VERSION_CODE, versionCode);
            editor.putString(LAST_BINARY_VERSION_NAME, versionName);
            editor.putString(CDV_SERVER_PATH, "");
            editor.apply();
            return true;
        }
        return false;
    }

    // normalize WebResourceResponse for Ajax
    private static WebResourceResponse normalizeWebResourceResponse(WebResourceResponse response) {
        String mimeType = response.getMimeType();
        WebResourceResponse normalizeResponse = new WebResourceResponse(
                mimeType,
                response.getEncoding(),
                response.getData()
        );
        int statusCode = response.getStatusCode();
        String reasonPhrase = response.getReasonPhrase();
        if (reasonPhrase == null || reasonPhrase.trim().isEmpty()) {
            if (statusCode == 200) {
                reasonPhrase = "OK";
            }
            else {
                reasonPhrase = "phrase is empty";
            }
        }

        try {
            normalizeResponse.setStatusCodeAndReasonPhrase(statusCode, reasonPhrase);
        }
        catch (IllegalArgumentException e) {
            Log.d(TAG, "setStatusCodeAndReasonPhrase: " + e.toString());
        }

        int AccessControlAllowOrigin = 0;
        String location = null;
        String exposeHeaders = null;
        Map<String, String> responseHeaders = response.getResponseHeaders();

        if (responseHeaders != null) {
            HashMap<String, String> headers = new LinkedHashMap<>();
            for (Map.Entry<String, String> entry : responseHeaders.entrySet()) {

                if (entry.getKey().equalsIgnoreCase("Access-Control-Allow-Origin")) {
                    if (AccessControlAllowOrigin++ > 0) {
                        Log.d(TAG, "Access-Control-Allow-Origin' header contains multiple values");
                        // enable first one only
                        // prevent that the 'Access-Control-Allow-Origin' header contains multiple values 'Server Name, *'.
                        continue;
                    }
                }
                else if (entry.getKey().equalsIgnoreCase("Access-Control-Expose-Headers")) {
                    // Access-Control-Expose-Headers from BBWebView
                    exposeHeaders = entry.getValue();
                    continue;
                }
                else if (entry.getKey().equalsIgnoreCase("location")) {
                    // BBWebView Library will set html web page with "<html><head><meta http-equiv=\"Refresh\" content=\"0; URL='" as a response body if needs auto redirection.
                    // in that case, response status code is "200" and headers has "location".
                    if (statusCode == 200 && mimeType.equalsIgnoreCase(TEXT_HTML.getMimeType())) {
                        location = entry.getValue();
                        Log.d(TAG, "redirection will be automatically by Ajax, Location = '" + location + "'");
                    }
                }

                headers.put(entry.getKey(), entry.getValue());
            }

            if (AccessControlAllowOrigin > 0) {
                StringBuilder AccessControlExposeHeaders = new StringBuilder();

                if (exposeHeaders == null || exposeHeaders.trim().isEmpty()) {
                    // expose CORS-safelisted response headers to Ajax
                    exposeHeaders = "Cache-Control, Content-Language, Content-Length, Content-Type, Expires, Last-Modified, Pragma";
                }
                AccessControlExposeHeaders.append(exposeHeaders);
                if (location != null) {
                    // expose "Location" response header to Ajax
                    // this will allow that Ajax can check if it's autoredirection
                    AccessControlExposeHeaders.append(", location");
                }
                headers.put("Access-Control-Expose-Headers", AccessControlExposeHeaders.toString());
            }

            normalizeResponse.setResponseHeaders(headers);
        }

        return normalizeResponse;
    }

    @Override
    public CordovaWebView getCordovaWebView() { return parentWebView; }

    @Override
    public ICordovaCookieManager getCookieManager() { return null; }

    @Override
    public View getView() { return webView; }

    private void enableRemoteDebugging() {
        try {
            WebView.setWebContentsDebuggingEnabled(true);
        } catch (IllegalArgumentException e) {
            LOG.d(TAG, "You have one job! To turn on Remote Web Debugging! YOU HAVE FAILED! ");
            e.printStackTrace();
        }
    }

    // Yeah, we know, which is why we makes ure that we don't do this if the bridge is
    // below JELLYBEAN_MR1.  It'd be great if lint was just a little smarter.
    @SuppressLint("AddJavascriptInterface")
    private static void exposeJsInterface(WebView webView, CordovaBridge bridge) {
        BBDExposedJsApi exposedJsApi = new BBDExposedJsApi(bridge);
        webView.addJavascriptInterface(exposedJsApi, "_cordovaNative");
    }

    /**
     * Load the url into the webview.
     */
    @Override
    public void loadUrl(final String url, boolean clearNavigationStack) {
        webView.loadUrl(url);
    }

    @Override
    public String getUrl() {
        return webView.getUrl();
    }

    @Override
    public void stopLoading() {
        webView.stopLoading();
    }

    @Override
    public void clearCache() {
        webView.clearCache(true);
    }

    @Override
    public void clearHistory() {
        webView.clearHistory();
    }

    @Override
    public boolean canGoBack() {
        return webView.canGoBack();
    }

    @Override
    public boolean goBack() {
        // Check webview first to see if there is a history
        // This is needed to support curPage#diffLink, since they are added to parentEngine's history, but not our history url array (JQMobile behavior)
        if (webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return false;
    }

    @Override
    public void setPaused(boolean value) {
        if (value) {
            webView.onPause();
            webView.pauseTimers();
        } else {
            webView.onResume();
            webView.resumeTimers();
        }
    }

    @Override
    public void destroy() {
        webView.destroy();
        // unregister the receiver
        if (receiver != null) {
            try {
                webView.getContext().unregisterReceiver(receiver);
            } catch (Exception e) {
                LOG.e(TAG, "Error unregistering configuration receiver: " + e.getMessage(), e);
            }
        }
    }

    @Override
    public void evaluateJavascript(String js, ValueCallback<String> callback) {
        webView.evaluateJavascript(js, callback);
    }

    private boolean isDeployDisabled() {
        return preferences.getBoolean("DisableDeploy", false);
    }
    public static class BBDCordovaWebViewClient extends BBWebViewClient {
        private final ConfigXmlParser parser = null;
        private String redirectLoadUrl = null;
        private String originalLoadUrl = null;
        private WebViewLocalServer localServer = BBDCordovaWebViewEngine.localServer;
        private Bridge bridge = BBDCordovaWebViewEngine.capacitorBridge;

        public BBDCordovaWebViewClient(Context context) {
            final WebViewAssetLoader webViewAssetLoader = new WebViewAssetLoader.Builder()
                    .addPathHandler(WEBVIEW_ASSET_DIRECTORY, new WebViewAssetLoader.AssetsPathHandler(context))
                    .build();

            super.assetLoader = webViewAssetLoader;
        }

        @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            final String authority = "localhost";
            if (WebViewLocalServer.isLocalFile(request.getUrl()) || (request.getUrl().getAuthority() != null && request.getUrl().getAuthority().equals(authority))) {
                WebResourceResponse response = localServer.shouldInterceptRequest(request);
                return response;
            } else if (!request.isForMainFrame() && request.getUrl().toString().equals("https://appassets.androidplatform.net/favicon.ico")) {
                try {
                    // webview request the favicon automatically even if there are no references to it in the index.html file.
                    // this solution prevent error "net::ERR_CACHE_MISS" with empty PNG file.
                    return new WebResourceResponse("image/png", "base64", new ByteArrayInputStream("iVBORw0KGgo=".getBytes()));
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            } else {
                WebResourceResponse response = super.shouldInterceptRequest(view, request);
                return normalizeWebResourceResponse(response);
            }
        }

        @Override
        public boolean shouldOverrideUrlLoading (WebView view, WebResourceRequest request) {
            if (MailTo.isMailTo(request.getUrl().toString())) {
                MailToInterceptor.getInstance().interceptMailTo(request.getUrl().toString(), view.getContext());
                return true;
            }
            return super.shouldOverrideUrlLoading(view, request);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
          return bridge.launchIntent(Uri.parse(url));
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
          super.onPageStarted(view, url, favicon);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
        }

        @Override
        public void onReceivedError(WebView webView,  WebResourceRequest request,  WebResourceError error) {
            Log.i(TAG, "onReceivedError " + request.getUrl() + " code: " + error.getErrorCode() + " desc: " + error.getDescription());
            List<WebViewListener> webViewListeners = bridge.getWebViewListeners();
            if (webViewListeners != null) {
              for (WebViewListener listener : bridge.getWebViewListeners()) {
                listener.onReceivedError(webView);
              }
            }
        }
    }

    public void setServerBasePath(String path) {
        localServer.hostFiles(path);
        webView.loadUrl(CDV_LOCAL_SERVER);
    }
}
