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

package com.good.gd.cordova.core;

import android.app.Activity;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;

import com.good.gd.GDAndroid;
import com.good.gd.GDStateAction;
import com.good.gd.cordova.capacitor.BridgeActivity;
import com.good.gd.cordova.core.launcher.BBDLauncherInterfaceProvider;
import com.good.gd.cordova.core.launcher.BBDLauncherManager;
import com.good.gd.cordova.core.utils.AppUtils;
import com.good.gd.cordova.plugins.helpers.CordovaGDServiceHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BBDLifeCycle implements Application.ActivityLifecycleCallbacks {

  private static final String TAG = BBDLifeCycle.class.getSimpleName();

  private static BBDLifeCycle s_instance = null;
  private static GDAndroid bbdRuntime = null;

  private static List<Class> activities = new ArrayList<Class>();

  private static Application s_application = null;

  private boolean isAuthorized = false;

  private static boolean applicationAuthorizeCalled = false;

  private BBDLifeCycle() {

  }

  /**
   * Method to get singleton BBDLifeCycle instance
   *
   * @return BBDLifeCycle shared instance
   */
  public static BBDLifeCycle getInstance() {
    if (s_instance == null) {
      s_instance = new BBDLifeCycle();
    }

    if (bbdRuntime == null) {
      bbdRuntime = GDAndroid.getInstance();
    }

    return s_instance;
  }

  public boolean isAuthorized() {
    return isAuthorized;
  }

  /**
   * Method to initialize GDAndroid, make applicationInit and register ActivityLifeCycleCallbacks
   *
   * @param application   application to initialize with
   */
  public void initialize(Application application) {
    if (!applicationAuthorizeCalled) {
      bbdRuntime.applicationInit(application);

      s_application = application;

      applicationAuthorizeCalled = true;
    }

    Log.d(TAG, "BlackBerry Dynamics SDK for Cordova - version "
      + AppUtils.VERSION_NAME);

    application.registerActivityLifecycleCallbacks(s_instance);

    registerBroadcastReceiver();

    CordovaGDServiceHelper.getInstance().init();
  }

  /**
   * method is used to call GDAndroid.activity init on each of the received none BBD activities
   *
   * @param activity activity to init with
   */
  private void activityInit(Activity activity) {
    String activityName = activity.getClass().getCanonicalName();

    if (activity instanceof BridgeActivity) {
      activities.add(activity.getClass());

      return;
    }

    if (dynamicsActivities.contains(activityName)) {
      return;
    }

    activities.add(activity.getClass());

    bbdRuntime.activityInit(activity);
  }

  @Override
  public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    Log.d(TAG, "Activity created: " + activity.getClass().getCanonicalName());

    activityInit(activity);
  }

  @Override
  public void onActivityStarted(Activity activity) {
    Log.d(TAG, "Activity started: " + activity.getClass().getCanonicalName());
  }

  @Override
  public void onActivityResumed(Activity activity) {
    Log.d(TAG, "Activity resumed: " + activity.getClass().getCanonicalName());
  }

  @Override
  public void onActivityPaused(Activity activity) {
    Log.d(TAG, "Activity paused: " + activity.getClass().getCanonicalName());
  }

  @Override
  public void onActivityStopped(Activity activity) {
    Log.d(TAG, "Activity stopped: " + activity.getClass().getCanonicalName());
  }

  @Override
  public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
    Log.d(TAG, "Activity save instance state: " + activity.getClass().getCanonicalName());
  }

  @Override
  public void onActivityDestroyed(Activity activity) {
    Log.d(TAG, "Activity destroyed: " + activity.getClass().getCanonicalName());
  }

  public static void setLauncherProvider(BBDLauncherInterfaceProvider provider) {
    BBDLauncherManager.initWithProvider(provider);
  }

  public void initLauncher() {
    BBDLauncherManager.getInstance().initForApplication(s_application, activities);
  }

  /**
   * method is used to register BroadcastReceiver which updates singleton instance state
   * after GDStateAction.GD_STATE_AUTHORIZED_ACTION action received
   */
  private void registerBroadcastReceiver() {
    IntentFilter intentFilter = new IntentFilter();
    intentFilter.addAction(GDStateAction.GD_STATE_AUTHORIZED_ACTION);

    bbdRuntime.registerReceiver(new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {

        BBDLauncherManager launcherManager = BBDLauncherManager.getInstance();
        switch (intent.getAction()) {
          case GDStateAction.GD_STATE_AUTHORIZED_ACTION:
            isAuthorized = true;

            break;

          default:
            break;
        }
      }
    }, intentFilter);
  }

  private static List<String> dynamicsActivities = Arrays.asList(
    "com.good.gd.Activity",
    "com.good.gd.ListActivity",
    "com.good.gd.ExpandableListActivity",
    "com.good.gd.PreferenceActivity",
    "com.good.gd.FragmentActivity",
    "com.good.gd.GDIccReceivingActivity",
    "com.good.gd.ui.GDInternalActivity",
    "com.good.gt.ndkproxy.icc.IccActivity",
    "com.good.gd.ui.runtimepermissions_ui.GDRuntimePermissionsControlActivity"
  );
}
