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

package com.good.gd.cordova.plugins.helpers.delegates;

import android.webkit.WebView;

import com.good.gd.net.GDHttpClient;

import com.good.gd.apache.http.HttpEntity;
import com.good.gd.apache.http.client.HttpClient;
import com.good.gd.apache.http.util.EntityUtils;

import java.io.IOException;

public class GDFileTransferDelegate {

    public HttpClient getHttpClient() {
        return new GDHttpClient();
    }

    public String getUserAgent(WebView webView) {
        return webView.getSettings().getUserAgentString();
    }

    public String getHttpEntityAsString(HttpEntity httpEntity) throws IOException {
        return EntityUtils.toString(httpEntity);
    }
}
