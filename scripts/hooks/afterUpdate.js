/**
 * Copyright (c) 2022 BlackBerry Limited. All Rights Reserved.
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

(function () {
    const path = require('path'),
        fs = require('fs'),
        { exec } = require('child_process'),
        projectRoot = process.env.INIT_CWD,
        androidProjectRoot = path.join(projectRoot, 'android'),
        iosProjectRoot = path.join(projectRoot, 'ios'),
        bundleId = readBundleIdFromCapacitorConfig(projectRoot),
        isWindows = process.platform === 'win32',
        { getPackageNameFromAndroidManifest, addAttributeToXmlElement, updateLinkerFlags, updateLauncher } = require(
            path.join(projectRoot, 'node_modules', 'capacitor-plugin-bbd-base', 'scripts', 'hooks', 'helper')
        ),
        currentPlatformName = process.env.CAPACITOR_PLATFORM_NAME;

    // Configure Capacitor project for Android platform
    if (fs.existsSync(androidProjectRoot) && currentPlatformName == 'android') {
        // Set GDApplicationID in settings.json
        const settingsJsonPath = path.join(
            androidProjectRoot, 'capacitor-cordova-android-plugins', 'src', 'main', 'assets', 'settings.json'
        );
        let settingsJsonObj = require(settingsJsonPath);

        if (settingsJsonObj.GDApplicationID !== bundleId) {
            settingsJsonObj.GDApplicationID = bundleId;
            fs.writeFileSync(settingsJsonPath, JSON.stringify(settingsJsonObj, null, 2), 'utf-8');
        }

        // Update AndroidManifest.xml
        const androidManifestPath = path.join(androidProjectRoot, 'app', 'src', 'main', 'AndroidManifest.xml');
        let androidManifestContent = fs.readFileSync(androidManifestPath, 'utf-8');
        const mainActivityRegexp = /([a-zA-Z0-9]+\.)+(MainActivity")/;

        androidManifestContent = androidManifestContent.replace(
            mainActivityRegexp, 'com.good.gd.cordova.capacitor.BridgeActivity"'
        );

        const manifestTagName = 'manifest',
            xmlnsToolsString = 'xmlns:tools="http://schemas.android.com/tools"';

        androidManifestContent = addAttributeToXmlElement(manifestTagName, xmlnsToolsString, androidManifestContent);

        const applicationTagName = 'application',
            replaceSupportsRtlString = 'tools:replace="android:supportsRtl"',
            supportsRtlString = 'android:supportsRtl="true"',
            applicationAndroidNameString = 'android:name="com.good.gd.cordova.core.BBDCordovaApp"';

        androidManifestContent = addAttributeToXmlElement(applicationTagName, replaceSupportsRtlString, androidManifestContent);
        androidManifestContent = addAttributeToXmlElement(applicationTagName, supportsRtlString, androidManifestContent);
        androidManifestContent = addAttributeToXmlElement(applicationTagName, applicationAndroidNameString, androidManifestContent);

        fs.writeFileSync(androidManifestPath, androidManifestContent, 'utf-8');

        // Update minSdkVersion and cordovaAndroidVersion in variables.gradle
        const variablesGradlePath = path.join(androidProjectRoot, 'variables.gradle');
        if (fs.existsSync(variablesGradlePath)) {
            let variablesGradleContent = fs.readFileSync(variablesGradlePath, 'utf-8');

            variablesGradleContent = variablesGradleContent.replace(/minSdkVersion\s*=\s*\d+/, 'minSdkVersion = 28');
            variablesGradleContent = variablesGradleContent.replace(/cordovaAndroidVersion\s*=\s*'\d+.{1,}\d+'/, 'cordovaAndroidVersion = \'10.1.1\'');
            fs.writeFileSync(variablesGradlePath, variablesGradleContent, 'utf-8');
        }

        // Update import in MainActivity.java
        const projectPackageName = getPackageNameFromAndroidManifest(androidManifestPath),
            mainActivityPath = path.join(androidProjectRoot, 'app', 'src', 'main', 'java', ...projectPackageName.split('.'), 'MainActivity.java');

        if (fs.existsSync(mainActivityPath)) {
            let mainActivityContent = fs.readFileSync(mainActivityPath, 'utf-8');

            mainActivityContent = mainActivityContent.replace(
                'import com.getcapacitor.BridgeActivity;',
                'import com.good.gd.cordova.capacitor.BridgeActivity;'
            );

            fs.writeFileSync(mainActivityPath, mainActivityContent, 'utf-8');
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

    function readBundleIdFromCapacitorConfig(projectRoot) {
        const configJson = path.join(projectRoot, 'capacitor.config.json');
        const configTs = path.join(projectRoot, 'capacitor.config.ts');

        if (fs.existsSync(configJson)) {
            return require(configJson)['appId'];
        }

        if (fs.existsSync(configTs)) {
            const bundleIdRegExp = /appId\: '(([a-zA-Z0-9]+\.)+([a-zA-Z0-9]+))'/;
            const configTsContent = fs.readFileSync(configTs, 'utf-8');
            const bundleId = bundleIdRegExp.exec(configTsContent);

            return bundleId ? bundleId[1] : null;
        }
    }

})();
