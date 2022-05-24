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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

/**
 * JSInject is responsible for returning Capacitor's core
 * runtime JS and any plugin JS back into HTML page responses
 * to the client.
 */
class JSInjector {

    private String globalJS;
    private String bridgeJS;
    private String pluginJS;
    private String cordovaJS;
    private String cordovaPluginsJS;
    private String cordovaPluginsFileJS;
    private String localUrlJS;

    public JSInjector(
        String globalJS,
        String bridgeJS,
        String pluginJS,
        String cordovaJS,
        String cordovaPluginsJS,
        String cordovaPluginsFileJS,
        String localUrlJS
    ) {
        this.globalJS = globalJS;
        this.bridgeJS = bridgeJS;
        this.pluginJS = pluginJS;
        this.cordovaJS = cordovaJS;
        this.cordovaPluginsJS = cordovaPluginsJS;
        this.cordovaPluginsFileJS = cordovaPluginsFileJS;
        this.localUrlJS = localUrlJS;
    }

    /**
     * Generates injectable JS content.
     * This may be used in other forms of injecting that aren't using an InputStream.
     * @return
     */
    public String getScriptString() {
        return (
            globalJS +
            "\n\n" +
            localUrlJS +
            "\n\n" +
            bridgeJS +
            "\n\n" +
            pluginJS +
            "\n\n" +
            cordovaJS +
            "\n\n" +
            cordovaPluginsFileJS +
            "\n\n" +
            cordovaPluginsJS
        );
    }

    /**
     * Given an InputStream from the web server, prepend it with
     * our JS stream
     * @param responseStream
     * @return
     */
    public InputStream getInjectedStream(InputStream responseStream) {
        String js = "<script type=\"text/javascript\">" + getScriptString() + "</script>";
        String html = this.readAssetStream(responseStream);
        if (html.contains("<head>")) {
            html = html.replace("<head>", "<head>\n" + js + "\n");
        } else if (html.contains("</head>")) {
            html = html.replace("</head>", js + "\n" + "</head>");
        } else {
            Logger.error("Unable to inject Capacitor, Plugins won't work");
        }
        return new ByteArrayInputStream(html.getBytes(StandardCharsets.UTF_8));
    }

    private String readAssetStream(InputStream stream) {
        try {
            final int bufferSize = 1024;
            final char[] buffer = new char[bufferSize];
            final StringBuilder out = new StringBuilder();
            Reader in = new InputStreamReader(stream, StandardCharsets.UTF_8);
            for (;;) {
                int rsz = in.read(buffer, 0, buffer.length);
                if (rsz < 0) break;
                out.append(buffer, 0, rsz);
            }
            return out.toString();
        } catch (Exception e) {
            Logger.error("Unable to process HTML asset file. This is a fatal error", e);
        }

        return "";
    }
}
