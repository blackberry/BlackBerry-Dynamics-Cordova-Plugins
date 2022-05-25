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

import android.util.Log;

import com.good.gd.icc.GDICCForegroundOptions;
import com.good.gd.icc.GDService;
import com.good.gd.icc.GDServiceClient;
import com.good.gd.icc.GDServiceClientListener;
import com.good.gd.icc.GDServiceException;
import com.good.gd.icc.GDServiceListener;
import com.good.gd.cordova.plugins.GDBasePlugin;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Helper class to interact with GDService and GDServiceClient.
 */
public class CordovaGDServiceHelper implements GDServiceListener, GDServiceClientListener {

  private static final String TAG = CordovaGDServiceHelper.class.getSimpleName();

  private static final String APP_KINETICS_APPLICATION_NAME_KEY = "applicationName";
  private static final String APP_KINETICS_SERVICE_NAME_KEY = "serviceName";
  private static final String APP_KINETICS_VERSION_KEY = "version";
  private static final String APP_KINETICS_METHOD_KEY = "method";
  private static final String APP_KINETICS_PARAMETERS_KEY = "parameters";
  private static final String APP_KINETICS_ATTACHMENT_KEY = "attachments";

  private final ArrayList<String> receivedFiles = new ArrayList<String>();
  private final HashMap<String, JSONObject> servicesWithoutHandlers
    = new HashMap<String, JSONObject>();

  private final HashMap<String, CallbackContext> servicesProviders
    = new HashMap<String, CallbackContext>();

  private CallbackContext callbackContext;

  private static CordovaGDServiceHelper instance = null;

  public static CordovaGDServiceHelper getInstance() {
    if(instance == null) {
      instance = new CordovaGDServiceHelper();
    }

    return instance;
  }

  /**
   * Constructs CordovaGDServiceHelper.
   */
  public void init() {
    try {
      GDService.setServiceListener(this);
      GDServiceClient.setServiceClientListener(this);
    } catch (final GDServiceException exception) {
      throw new Error("couldn't initialize GDService");
    }
  }

  /**
   * Sets the callback context to proceed with.
   *
   * @param callbackContext callback context to proceed with.
   */
  public void setCallbackContext(final CallbackContext callbackContext) {
    this.callbackContext = callbackContext;
  }

  /**
   * Add service provider with callback context.
   *
   * @param serviceName     service name.
   * @param serviceVersion  service version.
   * @param callbackContext callback context to proceed with.
   */
  public void addServiceProvider(final String serviceName, final String serviceVersion,
                                 final CallbackContext callbackContext) {
    servicesProviders.put(createKey(serviceName, serviceVersion),
      callbackContext);
  }

  /**
   * Sends files to another application.
   *
   * @param address     application address.
   * @param serviceId   application service id.
   * @param version     application service version.
   * @param method      service method.
   * @param parameters  parameters to send.
   * @param attachments attachments to message.
   * @throws GDServiceException in case of unsuccessful sending.
   */
  public void sendTo(final String address,
                     final String serviceId,
                     final String version,
                     final String method,
                     final Object parameters,
                     final String[] attachments) throws GDServiceException {
    GDServiceClient.sendTo(address,
      serviceId,
      version,
      method,
      parameters,
      attachments,
      GDICCForegroundOptions.PreferPeerInForeground);
  }

  /**
   * Bring given for address app to front.
   *
   * @param address address of app to be brought.
   * @throws GDServiceException in case of unsuccessful bringing.
   */
  public void bringToFront(final String address) throws GDServiceException {
    GDServiceClient.bringToFront(address);
  }

  @Override
  public void onReceivingAttachments(String s, int i, String s1) {

  }

  @Override
  public void onReceivingAttachmentFile(String s, String s1, long l, String s2) {

  }

  @Override
  public void onReceiveMessage(final String application,
                               final String service,
                               final String version,
                               final String method,
                               final Object params,
                               final String[] attachments,
                               final String requestID) {
    if (GDBasePlugin.FILE_TRANSFER_SERVICE_NAME.equals(service) &&
      GDBasePlugin.FILE_TRANSFER_SERVICE_VERSION.equals(version) &&
      GDBasePlugin.FILE_TRANSFER_METHOD.equals(method)) {
      proceedSimpleReceive(attachments);
      return;
    }

    final JSONObject resultObject = new JSONObject();

    try {
      resultObject.put(APP_KINETICS_APPLICATION_NAME_KEY, application);
      resultObject.put(APP_KINETICS_SERVICE_NAME_KEY, service);
      resultObject.put(APP_KINETICS_VERSION_KEY, version);
      resultObject.put(APP_KINETICS_METHOD_KEY, method);

      if (attachments != null) {
        resultObject.put(APP_KINETICS_ATTACHMENT_KEY, attachments);
      }

      if (params != null) {
        resultObject.put(APP_KINETICS_PARAMETERS_KEY, params);
      }
    } catch (final JSONException exception) {
      /**
       * DESNOTE ovovch: Actually json object should be valid here.
       */
      throw new Error("This case is impossible");
    }

    final CallbackContext callbackContext =
      servicesProviders.get(createKey(service, version));

    if (callbackContext == null) {
      servicesWithoutHandlers.put(createKey(service, version),
        resultObject);
      return;
    }

    sendPluginResult(resultObject, callbackContext);
  }

  @Override
  public void onReceiveMessage(final String application,
                               final Object params,
                               final String[] attachments,
                               final String requestID) {
    proceedSimpleReceive(attachments);

    try {
      GDService.replyTo(application, params,
        GDICCForegroundOptions.NoForegroundPreference,
        null,
        requestID);
    } catch (final GDServiceException exception) {
      /**
       * DESNOTE ovovch: according to iOS implementation we should do nothing here.
       */
      Log.w(TAG, exception.getMessage(), exception);
    }
  }

  /**
   * Check if given service was previously unhandled.
   *
   * @param serviceName service name.
   * @param version     service version.
   * @return true if service needs to be proceeded, false otherwise.
   */
  public boolean needToProceedWithService(final String serviceName,
                                          final String version) {
    final String key = createKey(serviceName, version);

    if (servicesWithoutHandlers.isEmpty()) {
      return false;
    }

    return servicesWithoutHandlers.containsKey(key);
  }

  /**
   * Proceeds with given service provider.
   *
   * @param serviceName service name.
   * @param version     service version.
   */
  public void proceedWithService(final String serviceName,
                                 final String version) {
    final String key = createKey(serviceName, version);

    final CallbackContext callbackContext = servicesProviders.get(key);

    if (callbackContext == null) {
      return;
    }

    final JSONObject item = servicesWithoutHandlers.get(key);

    sendPluginResult(item, callbackContext);

    servicesWithoutHandlers.remove(key);
  }

  /**
   * Check if need to proceed with received files.
   *
   * @return true if need to proceed with received files, false otherwise.
   */
  public boolean needToProceedReceivedFiles() {
    return !receivedFiles.isEmpty();
  }

  /**
   * Proceeds with received file.
   *
   * @param callbackContext callback context needed to proceed.
   */
  public void proceedReceivedFiles(final CallbackContext callbackContext) {
    final JSONArray resultArray = new JSONArray();
    final String pathPrefix = "/Inbox/";

    for (String attachment : receivedFiles) {
      if (attachment.startsWith(pathPrefix)) {
        // remove prefix
        attachment = attachment.substring(pathPrefix.length() - 1);
      }
      resultArray.put(attachment);
    }

    sendPluginResult(resultArray, callbackContext);

    receivedFiles.clear();
  }

  private void proceedSimpleReceive(final String[] attachments) {
    if (attachments == null) {
      return;
    }

    if (callbackContext != null) {
      final JSONArray resultArray = new JSONArray();

      for (final String attachment : attachments) {
        resultArray.put(attachment);
      }

      sendPluginResult(resultArray, callbackContext);
    } else {
      for (final String attachment : attachments) {
        receivedFiles.add(attachment);
      }
    }
  }

  private void sendPluginResult(final JSONObject item,
                                final CallbackContext callbackContext) {
    final PluginResult pluginResult =
      new PluginResult(PluginResult.Status.OK, item);
    pluginResult.setKeepCallback(true);

    callbackContext.sendPluginResult(pluginResult);
  }

  private void sendPluginResult(final JSONArray resultArray,
                                final CallbackContext callbackContext) {
    final PluginResult pluginResult =
      new PluginResult(PluginResult.Status.OK, resultArray);
    pluginResult.setKeepCallback(true);

    callbackContext.sendPluginResult(pluginResult);
  }

  @Override
  public void onMessageSent(final String application,
                            final String requestID,
                            final String[] attachments) {
    /**
     * DESNOTE ovocvh: just skip it
     */
  }

  private String createKey(final String serviceName, final String version) {
    return serviceName + "-" + version;
  }
}
