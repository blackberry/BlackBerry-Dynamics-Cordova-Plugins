/**
 * Copyright (c) 2023 BlackBerry Limited. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import path from 'path';
import fs from 'fs';
import { exec } from 'child_process';
import { readdir } from 'node:fs/promises';
import {
    replaceAndSave,
    addAttributeToXmlElement,
    updateLinkerFlags
} from '../node_modules/capacitor-plugin-bbd-base/scripts/hooks/helper.js';
import {
    BridgeJavaReplacementStrings,
    BridgeActivityJavaReplacementStrings,
    CapacitorWebViewJavaReplacementStrings,
    WebViewLocalServerJavaReplacementStrings,
    fileTreeString,
    implementationProjectCapacitorCordovaString,
    applyFromString
} from '../node_modules/capacitor-plugin-bbd-base/scripts/hooks/constants.js';

const projectRoot = process.env.INIT_CWD,
    packageJsonPath = path.join(projectRoot, 'package.json'),
    packageJson = JSON.parse(fs.readFileSync(packageJsonPath, 'utf-8')),
    androidProjectRoot = path.join(projectRoot, 'android'),
    iosProjectRoot = path.join(projectRoot, 'ios'),
    isWindows = process.platform === 'win32',
    currentPlatformName = process.env.CAPACITOR_PLATFORM_NAME,
    capacitorBasePluginPath = path.join(projectRoot, 'node_modules', 'capacitor-plugin-bbd-base'),
    nodeModulesCapacitorAndroidPath = path.join(
        projectRoot, 'node_modules', '@capacitor', 'android',
    );

(async function () {
    let bundleId = '';

    if (fs.existsSync(path.join(projectRoot, 'capacitor.config.ts'))) {
        // For Capacitor 5 project
        const capacitorConfig = fs.readFileSync(path.join(projectRoot, 'capacitor.config.ts'), 'utf-8'),
            searchTerm = 'appId:',
            bundleIdStart = capacitorConfig.substring(capacitorConfig.indexOf(searchTerm) + searchTerm.length + 2, capacitorConfig.length);

        bundleId = bundleIdStart.substring(0, bundleIdStart.indexOf('\n') - 2);
    } else {
        // For Capacitor 4 project
        const capacitorConfigJson = JSON.parse(fs.readFileSync(path.join(projectRoot, 'capacitor.config.json'), 'utf-8'));
        bundleId = capacitorConfigJson['appId'];
    }

    // Configure Capacitor project for Android platform
    if (fs.existsSync(androidProjectRoot) && currentPlatformName == 'android') {
        // Set GDApplicationID in settings.json
        const capacitorCordovaAndroidPluginsPackagePath = path.join(androidProjectRoot, 'capacitor-cordova-android-plugins'),
            settingsJsonPath = path.join(
                capacitorCordovaAndroidPluginsPackagePath, 'src', 'main', 'assets', 'settings.json'
            ),
            settingsJson = JSON.parse(fs.readFileSync(settingsJsonPath, 'utf-8'));

        if (settingsJson.GDApplicationID !== bundleId) {
            settingsJson.GDApplicationID = bundleId;
            fs.writeFileSync(settingsJsonPath, JSON.stringify(settingsJson, null, 2), 'utf-8');
        }

        // Update AndroidManifest.xml
        const androidManifestPath = path.join(androidProjectRoot, 'app', 'src', 'main', 'AndroidManifest.xml');
        let androidManifestContent = fs.readFileSync(androidManifestPath, 'utf-8');

        const manifestTagName = 'manifest',
            xmlnsToolsString = 'xmlns:tools="http://schemas.android.com/tools"';

        androidManifestContent = addAttributeToXmlElement(manifestTagName, xmlnsToolsString, androidManifestContent);

        const applicationTagName = 'application',
            replaceSupportsRtlString = 'tools:replace="android:supportsRtl"',
            supportsRtlString = 'android:supportsRtl="true"',
            applicationAndroidNameString = 'android:name="com.getcapacitor.core.BBDCordovaApp"';

        androidManifestContent = addAttributeToXmlElement(applicationTagName, replaceSupportsRtlString, androidManifestContent);
        androidManifestContent = addAttributeToXmlElement(applicationTagName, supportsRtlString, androidManifestContent);
        androidManifestContent = addAttributeToXmlElement(applicationTagName, applicationAndroidNameString, androidManifestContent);

        fs.writeFileSync(androidManifestPath, androidManifestContent, 'utf-8');

        // Update minSdkVersion and cordovaAndroidVersion in variables.gradle
        const variablesGradlePath = path.join(androidProjectRoot, 'variables.gradle');
        if (fs.existsSync(variablesGradlePath)) {
            let variablesGradleContent = fs.readFileSync(variablesGradlePath, 'utf-8');

            variablesGradleContent = variablesGradleContent.replace(/minSdkVersion\s*=\s*\d+/, 'minSdkVersion = 29');
            variablesGradleContent = variablesGradleContent.replace(/cordovaAndroidVersion\s*=\s*'\d+.{1,}\d+'/, 'cordovaAndroidVersion = \'10.1.1\'');
            fs.writeFileSync(variablesGradlePath, variablesGradleContent, 'utf-8');
        }

        // Update build.gradle from "capacitor-android" project:
        // - add dependency on "capacitor-cordova-android-plugins" project
        // - add "apply from" dependencies to necessary Gradle files
        const capAndroidBuildGradlePath = path.join(
            nodeModulesCapacitorAndroidPath, 'capacitor', 'build.gradle'
        );
        let capAndroidBuildGradleContent = fs.readFileSync(capAndroidBuildGradlePath, 'utf-8');

        if (!capAndroidBuildGradleContent.includes(implementationProjectCapacitorCordovaString)) {
            capAndroidBuildGradleContent = capAndroidBuildGradleContent.replace(
                fileTreeString,
                implementationProjectCapacitorCordovaString
            );

            capAndroidBuildGradleContent += applyFromString;

            fs.writeFileSync(capAndroidBuildGradlePath, capAndroidBuildGradleContent, 'utf-8');
        }

        // Update "capacitor-android" sources
        const capacitorAndroidPackagePath = path.join(
                nodeModulesCapacitorAndroidPath, 'capacitor', 'src', 'main', 'java', 'com', 'getcapacitor'
            ),
            capacitorAndroidCorePackagePath = path.join(
                capacitorAndroidPackagePath, 'core'
            ),
            gdCordovaCapacitorPackagePath = path.join(
                capacitorCordovaAndroidPluginsPackagePath, 'src', 'main', 'java', 'com', 'good', 'gd', 'cordova', 'getcapacitor'
            );

        replaceAndSave(
            path.join(capacitorAndroidPackagePath, 'Bridge.java'),
            BridgeJavaReplacementStrings,
            { replacementTextToCheck: 'import com.getcapacitor.core.webview.engine.BBDCordovaWebChromeClient'}
        );

        replaceAndSave(
            path.join(capacitorAndroidPackagePath, 'BridgeActivity.java'),
            BridgeActivityJavaReplacementStrings,
            { replacementTextToCheck: 'import com.getcapacitor.core.BBDLifeCycle' }
        );

        replaceAndSave(
            path.join(capacitorAndroidPackagePath, 'CapacitorWebView.java'),
            CapacitorWebViewJavaReplacementStrings,
            { replacementTextToCheck: 'import com.blackberry.bbwebview.BBWebView' }
        );

        replaceAndSave(
            path.join(capacitorAndroidPackagePath, 'WebViewLocalServer.java'),
            WebViewLocalServerJavaReplacementStrings,
            { replacementTextToCheck: 'public static boolean isLocalFile' }
        );

        const capacitorBaseDynamicsCorePath = path.join(
            capacitorBasePluginPath, 'src', 'android', 'com', 'getcapacitor', 'dynamics',
        );

        // Copy BBDCordovaApp.java, BBDLifeCycle.java and webview/ to "capacito-android" package
        // src/android/com/getcapacitor/dynamics/BBDLifeCycle.java
        if (!fs.existsSync(path.join(capacitorAndroidCorePackagePath, 'BBDCordovaApp.java'))) {
            fs.cpSync(
                path.join(capacitorBaseDynamicsCorePath, 'BBDCordovaApp.java'),
                path.join(capacitorAndroidCorePackagePath, 'BBDCordovaApp.java')
            );
            fs.cpSync(
                path.join(capacitorBaseDynamicsCorePath, 'BBDLifeCycle.java'),
                path.join(capacitorAndroidCorePackagePath, 'BBDLifeCycle.java')
            );

            fs.cpSync(
                path.join(capacitorBaseDynamicsCorePath, 'webview'),
                path.join(capacitorAndroidCorePackagePath, 'webview'),
                { recursive: true }
            );
        }

        const getFilesInDirectory = async (dirPath) => Promise.all(
            await readdir(dirPath, { withFileTypes: true }).then((entries) => entries.map((entry) => {
                const childPath = path.join(dirPath, entry.name)
                return entry.isDirectory() ? getFilesInDirectory(childPath) : childPath
            })),
        );

        // For InAppBrowser and Launcher: copy whole getcapacitor folder from "capacitor-android" module to com.good.gd.cordova package
        // in "capacitor-cordova-android-plugins" module
        const gdCordovaCorePackagePath = path.join(gdCordovaCapacitorPackagePath, '..', 'core');

        if (
            fs.existsSync(path.join(projectRoot, 'node_modules', 'cordova-plugin-bbd-inappbrowser')) ||
            fs.existsSync(path.join(projectRoot, 'node_modules', 'cordova-plugin-bbd-launcher')) &&
            !fs.existsSync(gdCordovaCapacitorPackagePath)
        ) {
            fs.cpSync(capacitorAndroidPackagePath, gdCordovaCapacitorPackagePath, { recursive: true });

            fs.cpSync(path.join(gdCordovaCapacitorPackagePath, 'core'), path.join(gdCordovaCapacitorPackagePath, 'core', '..', '..', '..', 'cordova', 'core'), { recursive: true });
            fs.rmSync(path.join(gdCordovaCapacitorPackagePath, 'core'), { recursive: true, force: true });

            const getCapacitorFiles = await getFilesInDirectory(gdCordovaCapacitorPackagePath),
                getCapacitorJavaFiles = getCapacitorFiles.flat(Number.POSITIVE_INFINITY).filter(f => f.includes('.java'));

            const gdCordovaCoreFiles = await getFilesInDirectory(gdCordovaCorePackagePath),
                gdCordovaCoreJavaFiles = gdCordovaCoreFiles.flat(Number.POSITIVE_INFINITY).filter(f => f.includes('.java'));

            // In com.good.gd.cordova.getcapacitor package:
            // - update "com.getcapacitor.android.R" to "capacitor.cordova.android.plugins.R"
            // - update package names in imports
            let androidRReplacementString = 'capacitor.cordova.android.plugins.R';
            // Capacitor 4: import should be updated to "capacitor.android.plugins.R"
            const version4RegExp = /^(\~|\^)?(4\.)/;
            if (version4RegExp.test(packageJson.dependencies['@capacitor/android'])) {
                androidRReplacementString = 'capacitor.android.plugins.R';
            }

            getCapacitorJavaFiles.forEach(file => {
                let fileContent = fs.readFileSync(file, 'utf-8');
                fileContent = fileContent
                    .replaceAll('com.getcapacitor.android.R', androidRReplacementString)
                    .replaceAll('com.getcapacitor.core', 'com.good.gd.cordova.core')
                    .replaceAll('com.getcapacitor', 'com.good.gd.cordova.getcapacitor');
                fs.writeFileSync(file, fileContent, 'utf-8');
            });

            gdCordovaCoreJavaFiles.forEach(file => {
                let fileContent = fs.readFileSync(file, 'utf-8');
                fileContent = fileContent
                    .replaceAll('com.getcapacitor.core', 'com.good.gd.cordova.core')
                    .replaceAll('com.getcapacitor', 'com.good.gd.cordova.getcapacitor');
                fs.writeFileSync(file, fileContent, 'utf-8');
            });

            // Copy resources from "cordova-android" to "capacitor-cordova-android-plugins" module and update package names
            const resFromCapacitorCordovaAndroidPluginsPath = path.join(capacitorCordovaAndroidPluginsPackagePath, 'src', 'main', 'res');

            fs.cpSync(
                path.join(nodeModulesCapacitorAndroidPath, 'capacitor', 'src', 'main', 'res'),
                resFromCapacitorCordovaAndroidPluginsPath,
                { recursive: true }
            );

            const layoutResFromCapacitorCordovaAndroidPluginsPath = await getFilesInDirectory(
                    path.join(resFromCapacitorCordovaAndroidPluginsPath, 'layout')
                ),
                layoutResFiles = layoutResFromCapacitorCordovaAndroidPluginsPath.flat(
                    Number.POSITIVE_INFINITY).filter(f => f.includes('.xml')
                );

            const valuesResFromCapacitorCordovaAndroidPluginsPath = await getFilesInDirectory(
                    path.join(resFromCapacitorCordovaAndroidPluginsPath, 'values')
                ),
                valuestResFiles = valuesResFromCapacitorCordovaAndroidPluginsPath.flat(
                    Number.POSITIVE_INFINITY).filter(f => f.includes('.xml')
                );

            [...layoutResFiles, ...valuestResFiles].forEach(file => {
                let fileContent = fs.readFileSync(file, 'utf-8');
                fileContent = fileContent.replaceAll('com.getcapacitor', 'com.good.gd.cordova.getcapacitor');
                fs.writeFileSync(file, fileContent, 'utf-8');
            });

        }
    }

    // Configure Capacitor project for iOS platform
    if (fs.existsSync(iosProjectRoot) && currentPlatformName == 'ios') {
        if (isWindows) { return; }

        const installPath = path.join(
            projectRoot, 'node_modules', 'capacitor-plugin-bbd-base', 'scripts', 'hooks', 'ios', 'install.rb'
        );

        exec(
            `${installPath} -i ${bundleId} -p "${iosProjectRoot}"`,
            (error) => {
                if (error) {
                    console.log('\nERROR: capacitor-plugin-bbd-base install exited with error!')
                }
            }
        );

        updateLinkerFlags();
    }

})();
