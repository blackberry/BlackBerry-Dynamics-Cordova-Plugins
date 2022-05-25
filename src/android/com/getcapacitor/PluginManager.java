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

import android.content.res.AssetManager;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class PluginManager {

    private final AssetManager assetManager;

    public PluginManager(AssetManager assetManager) {
        this.assetManager = assetManager;
    }

    public List<Class<? extends Plugin>> loadPluginClasses() throws PluginLoadException {
        JSONArray pluginsJSON = parsePluginsJSON();
        ArrayList<Class<? extends Plugin>> pluginList = new ArrayList<>();

        try {
            for (int i = 0, size = pluginsJSON.length(); i < size; i++) {
                JSONObject pluginJSON = pluginsJSON.getJSONObject(i);
                String classPath = pluginJSON.getString("classpath");
                Class<?> c = Class.forName(classPath);
                pluginList.add((Class<? extends Plugin>) c);
            }
        } catch (JSONException e) {
            throw new PluginLoadException("Could not parse capacitor.plugins.json as JSON");
        } catch (ClassNotFoundException e) {
            throw new PluginLoadException("Could not find class by class path: " + e.getMessage());
        }

        return pluginList;
    }

    private JSONArray parsePluginsJSON() throws PluginLoadException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(assetManager.open("capacitor.plugins.json")))) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            String jsonString = builder.toString();
            return new JSONArray(jsonString);
        } catch (IOException e) {
            throw new PluginLoadException("Could not load capacitor.plugins.json");
        } catch (JSONException e) {
            throw new PluginLoadException("Could not parse capacitor.plugins.json as JSON");
        }
    }
}
