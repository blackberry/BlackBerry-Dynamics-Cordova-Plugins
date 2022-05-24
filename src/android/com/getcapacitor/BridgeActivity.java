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
import android.content.res.Configuration;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.good.gd.GDAndroid;
import com.good.gd.GDStateAction;
import com.good.gd.cordova.core.BBDLifeCycle;
import com.good.gd.cordova.core.launcher.BBDLauncherManager;

import capacitor.android.plugins.R;
import java.util.ArrayList;
import java.util.List;

public class BridgeActivity extends AppCompatActivity {

    protected Bridge bridge;
    protected boolean keepRunning = true;
    private CapConfig config;

    private int activityDepth = 0;
    private List<Class<? extends Plugin>> initialPlugins = new ArrayList<>();
    private final Bridge.Builder bridgeBuilder = new Bridge.Builder(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        GDAndroid.getInstance().activityInit(this);

        super.onCreate(savedInstanceState);
        bridgeBuilder.setInstanceState(savedInstanceState);
        getApplication().setTheme(getResources().getIdentifier("AppTheme_NoActionBar", "style", getPackageName()));
        setTheme(getResources().getIdentifier("AppTheme_NoActionBar", "style", getPackageName()));
        setTheme(R.style.AppTheme_NoActionBar);
        setContentView(R.layout.bb_bridge_layout_main);
    }

    /**
     * Initializes the Capacitor Bridge with the Activity.
     * @deprecated It is preferred not to call this method. If it is not called, the bridge is
     * initialized automatically. If you need to add additional plugins during initialization,
     * use {@link #registerPlugin(Class)} or {@link #registerPlugins(List)}.
     *
     * @param plugins A list of plugins to initialize with Capacitor
     */
    @Deprecated
    protected void init(Bundle savedInstanceState, List<Class<? extends Plugin>> plugins) {
        this.init(savedInstanceState, plugins, null);
    }

    /**
     * Initializes the Capacitor Bridge with the Activity.
     * @deprecated It is preferred not to call this method. If it is not called, the bridge is
     * initialized automatically. If you need to add additional plugins during initialization,
     * use {@link #registerPlugin(Class)} or {@link #registerPlugins(List)}.
     *
     * @param plugins A list of plugins to initialize with Capacitor
     * @param config An instance of a Capacitor Configuration to use. If null, will load from file
     */
    @Deprecated
    protected void init(Bundle savedInstanceState, List<Class<? extends Plugin>> plugins, CapConfig config) {
        this.initialPlugins = plugins;
        this.config = config;

        this.load();
    }

    /**
     * @deprecated This method should not be called manually.
     */
    @Deprecated
    protected void load(Bundle savedInstanceState) {
        this.load();
    }

    private void load() {
        if (BBDLifeCycle.getInstance().isAuthorized()) {
            Logger.debug("Starting BridgeActivity");

            bridge = bridgeBuilder.addPlugins(initialPlugins).setConfig(config).create();

            this.keepRunning = bridge.shouldKeepRunning();
            this.onNewIntent(getIntent());

            BBDLifeCycle.getInstance().initLauncher();
            BBDLauncherManager.getInstance().setAppAuthorized();
        } else {
            registerDynamicsReceiver();
        }
    }

    private void registerDynamicsReceiver() {
      IntentFilter intentFilter = new IntentFilter();
      intentFilter.addAction(GDStateAction.GD_STATE_AUTHORIZED_ACTION);

      final GDAndroid bbdRuntime = GDAndroid.getInstance();

      bbdRuntime.registerReceiver(new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        switch (intent.getAction()) {
          case GDStateAction.GD_STATE_AUTHORIZED_ACTION:
            load();
            // unregister BroadcastReceiver to prevent View reload after application unlock
            bbdRuntime.unregisterReceiver(this);
            break;

          case GDStateAction.GD_STATE_LOCKED_ACTION:
            BBDLauncherManager.getInstance().setAppUnauthorized();
            break;

          case GDStateAction.GD_STATE_UPDATE_CONFIG_ACTION:
            BBDLauncherManager.getInstance().onUpdateConfig();
            break;

          case GDStateAction.GD_STATE_UPDATE_POLICY_ACTION:
            BBDLauncherManager.getInstance().onUpdatePolicy();
            break;

          default:
            break;
        }
        }
      }, intentFilter);
    }

    public void registerPlugin(Class<? extends Plugin> plugin) {
        bridgeBuilder.addPlugin(plugin);
    }

    public void registerPlugins(List<Class<? extends Plugin>> plugins) {
        bridgeBuilder.addPlugins(plugins);
    }

    public Bridge getBridge() {
        return this.bridge;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (BBDLifeCycle.getInstance().isAuthorized()) {
            bridge.saveInstanceState(outState);
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        // Preferred behavior: init() was not called, so we construct the bridge with auto-loaded plugins.
        if (bridge == null) {
            PluginManager loader = new PluginManager(getAssets());

            try {
                bridgeBuilder.addPlugins(loader.loadPluginClasses());
            } catch (PluginLoadException ex) {
                Logger.error("Error loading plugins.", ex);
            }

            this.load();
        }

        activityDepth++;

        if (BBDLifeCycle.getInstance().isAuthorized()) {
            this.bridge.onStart();
        }

        Logger.debug("App started");
    }

    @Override
    public void onRestart() {
        super.onRestart();
        
        if (BBDLifeCycle.getInstance().isAuthorized()) {
            this.bridge.onRestart();
        }
        
        Logger.debug("App restarted");
    }

    @Override
    public void onResume() {
        super.onResume();

        if (BBDLifeCycle.getInstance().isAuthorized()) {
            bridge.getApp().fireStatusChange(true);
            this.bridge.onResume();
        }

        Logger.debug("App resumed");
    }

    @Override
    public void onPause() {
        super.onPause();

        if (BBDLifeCycle.getInstance().isAuthorized()) {
            this.bridge.onPause();
        }

        Logger.debug("App paused");
    }

    @Override
    public void onStop() {
        super.onStop();

        if (BBDLifeCycle.getInstance().isAuthorized()) {
            activityDepth = Math.max(0, activityDepth - 1);
            if (activityDepth == 0) {
                bridge.getApp().fireStatusChange(false);
            }
            this.bridge.onStop();
        }

        Logger.debug("App stopped");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (BBDLifeCycle.getInstance().isAuthorized()) {
            this.bridge.onDestroy();
        }

        Logger.debug("App destroyed");
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (BBDLifeCycle.getInstance().isAuthorized()) {
            this.bridge.onDetachedFromWindow();
        }
    }

    /**
     * Handles permission request results.
     *
     * Capacitor is backwards compatible such that plugins using legacy permission request codes
     * may coexist with plugins using the AndroidX Activity v1.2 permission callback flow introduced
     * in Capacitor 3.0.
     *
     * In this method, plugins are checked first for ownership of the legacy permission request code.
     * If the {@link Bridge#onRequestPermissionsResult(int, String[], int[])} method indicates it has
     * handled the permission, then the permission callback will be considered complete. Otherwise,
     * the permission will be handled using the AndroidX Activity flow.
     *
     * @param requestCode the request code associated with the permission request
     * @param permissions the Android permission strings requested
     * @param grantResults the status result of the permission request
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (this.bridge == null) {
            return;
        }

        if (!bridge.onRequestPermissionsResult(requestCode, permissions, grantResults)) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    /**
     * Handles activity results.
     *
     * Capacitor is backwards compatible such that plugins using legacy activity result codes
     * may coexist with plugins using the AndroidX Activity v1.2 activity callback flow introduced
     * in Capacitor 3.0.
     *
     * In this method, plugins are checked first for ownership of the legacy request code. If the
     * {@link Bridge#onActivityResult(int, int, Intent)} method indicates it has handled the activity
     * result, then the callback will be considered complete. Otherwise, the result will be handled
     * using the AndroidX Activiy flow.
     *
     * @param requestCode the request code associated with the activity result
     * @param resultCode the result code
     * @param data any data included with the activity result
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (this.bridge == null) {
            return;
        }

        if (!bridge.onActivityResult(requestCode, resultCode, data)) {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (this.bridge == null || intent == null) {
            return;
        }

        this.bridge.onNewIntent(intent);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (this.bridge == null) {
            return;
        }

        this.bridge.onConfigurationChanged(newConfig);
    }
}
