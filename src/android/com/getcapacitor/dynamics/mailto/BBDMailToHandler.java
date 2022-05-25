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

package com.good.gd.cordova.core.mailto;

import android.net.MailTo;

import org.apache.cordova.CordovaPlugin;

/**
 * This specific CordovaPlugin subclass is used fo intercepting the mailto: urls
 *
 * It is provided as a separate feature to get rid of the onOverrideUrlLoading method from the Base plugin,
 * to prevent calling the method by each of the Base plugin subclasses.
 */
public class BBDMailToHandler extends CordovaPlugin {
    /**
     * Called when the URL of the webview changes.
     * Used for mailto: urls intercepting
     *
     * @param url               The URL that is being changed to.
     * @return                  Return false to allow the URL to load, return true to prevent the URL from loading.
     */
    @Override
    public boolean onOverrideUrlLoading(String url) {
        if (MailTo.isMailTo(url)) {
            MailToInterceptor.getInstance().interceptMailTo(url, cordova.getContext());

            return true;
        }

        return super.onOverrideUrlLoading(url);
    }
}
