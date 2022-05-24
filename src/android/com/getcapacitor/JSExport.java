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

import static com.good.gd.cordova.capacitor.FileUtils.readFile;

import android.content.Context;
import android.text.TextUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JSExport {

    private static String CATCHALL_OPTIONS_PARAM = "_options";
    private static String CALLBACK_PARAM = "_callback";

    public static String getGlobalJS(Context context, boolean loggingEnabled, boolean isDebug) {
        return "window.Capacitor = { DEBUG: " + isDebug + ", isLoggingEnabled: " + loggingEnabled + ", Plugins: {} };";
    }

    public static String getCordovaJS(Context context) {
        String fileContent = "";
        try {
            fileContent = readFile(context.getAssets(), "public/cordova.js");
        } catch (IOException ex) {
            Logger.error("Unable to read public/cordova.js file, Cordova plugins will not work");
        }
        return fileContent;
    }

    public static String getCordovaPluginsFileJS(Context context) {
        String fileContent = "";
        try {
            fileContent = readFile(context.getAssets(), "public/cordova_plugins.js");
        } catch (IOException ex) {
            Logger.error("Unable to read public/cordova_plugins.js file, Cordova plugins will not work");
        }
        return fileContent;
    }

    public static String getPluginJS(Collection<PluginHandle> plugins) {
        List<String> lines = new ArrayList<>();
        JSONArray pluginArray = new JSONArray();

        lines.add("// Begin: Capacitor Plugin JS");
        for (PluginHandle plugin : plugins) {
            lines.add(
                "(function(w) {\n" +
                "var a = (w.Capacitor = w.Capacitor || {});\n" +
                "var p = (a.Plugins = a.Plugins || {});\n" +
                "var t = (p['" +
                plugin.getId() +
                "'] = {});\n" +
                "t.addListener = function(eventName, callback) {\n" +
                "  return w.Capacitor.addListener('" +
                plugin.getId() +
                "', eventName, callback);\n" +
                "}"
            );
            Collection<PluginMethodHandle> methods = plugin.getMethods();
            for (PluginMethodHandle method : methods) {
                if (method.getName().equals("addListener") || method.getName().equals("removeListener")) {
                    // Don't export add/remove listener, we do that automatically above as they are "special snowflakes"
                    continue;
                }
                lines.add(generateMethodJS(plugin, method));
            }

            lines.add("})(window);\n");
            pluginArray.put(createPluginHeader(plugin));
        }

        return TextUtils.join("\n", lines) + "\nwindow.Capacitor.PluginHeaders = " + pluginArray.toString() + ";";
    }

    public static String getCordovaPluginJS(Context context) {
        return getFilesContent(context, "public/plugins");
    }

    public static String getFilesContent(Context context, String path) {
        StringBuilder builder = new StringBuilder();
        try {
            String[] content = context.getAssets().list(path);
            if (content.length > 0) {
                for (String file : content) {
                    if (!file.endsWith(".map")) {
                        builder.append(getFilesContent(context, path + "/" + file));
                    }
                }
            } else {
                return readFile(context.getAssets(), path);
            }
        } catch (IOException ex) {
            Logger.error("Unable to read file at path " + path);
        }
        return builder.toString();
    }

    private static JSONObject createPluginHeader(PluginHandle plugin) {
        JSONObject pluginObj = new JSONObject();
        Collection<PluginMethodHandle> methods = plugin.getMethods();
        try {
            String id = plugin.getId();
            JSONArray methodArray = new JSONArray();
            pluginObj.put("name", id);

            for (PluginMethodHandle method : methods) {
                methodArray.put(createPluginMethodHeader(method));
            }

            pluginObj.put("methods", methodArray);
        } catch (JSONException e) {
            // ignore
        }
        return pluginObj;
    }

    private static JSONObject createPluginMethodHeader(PluginMethodHandle method) {
        JSONObject methodObj = new JSONObject();

        try {
            methodObj.put("name", method.getName());
            if (!method.getReturnType().equals(PluginMethod.RETURN_NONE)) {
                methodObj.put("rtype", method.getReturnType());
            }
        } catch (JSONException e) {
            // ignore
        }

        return methodObj;
    }

    public static String getBridgeJS(Context context) throws JSExportException {
        return getFilesContent(context, "native-bridge.js");
    }

    private static String generateMethodJS(PluginHandle plugin, PluginMethodHandle method) {
        List<String> lines = new ArrayList<>();

        List<String> args = new ArrayList<>();
        // Add the catch all param that will take a full javascript object to pass to the plugin
        args.add(CATCHALL_OPTIONS_PARAM);

        String returnType = method.getReturnType();
        if (returnType.equals(PluginMethod.RETURN_CALLBACK)) {
            args.add(CALLBACK_PARAM);
        }

        // Create the method function declaration
        lines.add("t['" + method.getName() + "'] = function(" + TextUtils.join(", ", args) + ") {");

        switch (returnType) {
            case PluginMethod.RETURN_NONE:
                lines.add(
                    "return w.Capacitor.nativeCallback('" +
                    plugin.getId() +
                    "', '" +
                    method.getName() +
                    "', " +
                    CATCHALL_OPTIONS_PARAM +
                    ")"
                );
                break;
            case PluginMethod.RETURN_PROMISE:
                lines.add(
                    "return w.Capacitor.nativePromise('" + plugin.getId() + "', '" + method.getName() + "', " + CATCHALL_OPTIONS_PARAM + ")"
                );
                break;
            case PluginMethod.RETURN_CALLBACK:
                lines.add(
                    "return w.Capacitor.nativeCallback('" +
                    plugin.getId() +
                    "', '" +
                    method.getName() +
                    "', " +
                    CATCHALL_OPTIONS_PARAM +
                    ", " +
                    CALLBACK_PARAM +
                    ")"
                );
                break;
            default:
            // TODO: Do something here?
        }

        lines.add("}");

        return TextUtils.join("\n", lines);
    }
}
