#!/usr/bin/env node

/* Copyright (c) 2021 BlackBerry Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

module.exports = function(context) {

    var fs = require('fs'),
        path = require('path'),
        cmdPlatforms = context.opts.platforms,
        projectRoot = context.opts.projectRoot,
        platformAndroidRoot = path.join(projectRoot, 'platforms', 'android'),
        platformiOSRoot = path.join(projectRoot, 'platforms', 'ios');

    if (cmdPlatforms.includes('android') && fs.existsSync(platformAndroidRoot)) {
        handlePlatformCordovaPlugins(path.join(platformAndroidRoot, 'platform_www', 'cordova_plugins.js'));
        handlePlatformCordovaPlugins(
            path.join(platformAndroidRoot, 'app', 'src', 'main', 'assets', 'www', 'cordova_plugins.js')
        );
    }

    if (cmdPlatforms.includes('ios') && fs.existsSync(platformiOSRoot)) {
        handlePlatformCordovaPlugins(path.join(platformiOSRoot, 'platform_www', 'cordova_plugins.js'));
        handlePlatformCordovaPlugins(path.join(platformiOSRoot, 'www', 'cordova_plugins.js'));
    }

    // Removes Window "clobbers", "merges" from cordova-sqlite-storage to avoid conflicts with cordova-plugin-bbd-sqlite-storage
    // when both plugins are added
    // DEVNOTE: cordova-sqlite-storage API's can be accessed using cordova.require('cordova-sqlite-storage.someMethod')
    function handlePlatformCordovaPlugins(cordovaPluginsPath) {
        var cordovaPluginsContent = fs.readFileSync(cordovaPluginsPath, 'utf-8'),
            startString = 'module.exports = ',
            endString = '];',
            startStringIndex = cordovaPluginsContent.indexOf(startString),
            endStringIndex = cordovaPluginsContent.indexOf(endString);

        // Get plugins list from cordova_plugin.js
        var pluginsListString = cordovaPluginsContent.substring(
                startStringIndex + startString.length, endStringIndex + endString.length - 1
            ),
            pluginsList = JSON.parse(pluginsListString);

        // Update propertires for cordova-sqlite-storage entries
        var updatedPluginList = pluginsList.map(function(plugin) {
            if (plugin.pluginId === 'cordova-sqlite-storage') {
                var updatedPlugin = Object.assign({}, plugin);
                delete updatedPlugin.clobbers;
                delete updatedPlugin.merges;
                updatedPlugin.runs = 'true';

                return updatedPlugin;
            }

            return plugin;
        }),
            updatedPluginListString = JSON.stringify(updatedPluginList, null, 4);

        fs.writeFileSync(
            cordovaPluginsPath, cordovaPluginsContent.replace(pluginsListString, updatedPluginListString)
        );
    }

};
