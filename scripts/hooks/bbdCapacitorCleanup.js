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
import {
    replaceAndSave,
    removeAssertDeploymentTarget,
    cleanUpCAPBridgeViewController
} from '../node_modules/capacitor-plugin-bbd-base/scripts/hooks/helper.js';
import {
    BridgeJavaReplacementStrings,
    BridgeActivityJavaReplacementStrings,
    CapacitorWebViewJavaReplacementStrings,
    WebViewLocalServerJavaReplacementStrings,
    applyFromString,
    fileTreeString,
    implementationProjectCapacitorCordovaString
} from '../node_modules/capacitor-plugin-bbd-base/scripts/hooks/constants.js';

(function () {
    // We should run this script only if we uninstall capacitor-plugin-bbd-base plugin.
    // In other circumstances like 'npm i' or 'yarn' or 'npm uninstall' or 'npm i <other_module>' we should exit.
    // This is becasue sometimes other actions trigger running this script and we need to do setup process again.

    const projectRoot = process.env.INIT_CWD,
        androidProjectRoot = path.join(projectRoot, 'android'),
        iosProjectRoot = path.join(projectRoot, 'ios'),
        packageJsonObj = JSON.parse(fs.readFileSync(path.join(projectRoot, 'package.json'), 'utf-8'));

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

    // Remove integration hook from package.json
    const hooks = {
        afterCopy: 'afterCopy.js',
        afterUpdate: 'afterUpdate.js',
        cleanup: 'bbdCapacitorCleanup.js'
    };

    const hookScriptPath = (name, spaceBefore = false) => `${spaceBefore ? ' ' : ''}&& node ./hooks/${name}`;

    [
        'capacitor:copy:after',
        'capacitor:update:after',
        'cleanup'
    ].forEach(hookType => {
        let script = packageJsonObj.scripts[hookType];
        if (script && (script.includes(hookScriptPath(hooks.afterCopy)) || script.includes(hookScriptPath(hooks.afterUpdate)))) {
            packageJsonObj.scripts[hookType] = script
                .replace(hookScriptPath(hooks.afterCopy, true), '')
                .replace(hookScriptPath(hooks.afterUpdate, true), '')
                .replace(hookScriptPath(hooks.cleanup, true), '');
        } else {
            delete packageJsonObj.scripts[hookType];
        }
    });

    fs.writeFileSync(path.join(projectRoot, 'package.json'), JSON.stringify(packageJsonObj, null, 2), 'utf-8');

    // Cleanup configurations for Android platform in Capacitor project
    if (fs.existsSync(androidProjectRoot)) {
        // Remove settings
        const settingsJsonPath = path.join(
            androidProjectRoot, 'capacitor-cordova-android-plugins', 'src', 'main', 'assets', 'settings.json'
        ),
            dynamicsSettingsJsonPath = path.join(
                androidProjectRoot, 'capacitor-cordova-android-plugins', 'src', 'main', 'assets', 'com.blackberry.dynamics.settings.json'
            );

        if (fs.existsSync(settingsJsonPath)) {
            fs.unlinkSync(settingsJsonPath);
        }

        if (fs.existsSync(dynamicsSettingsJsonPath)) {
            fs.unlinkSync(dynamicsSettingsJsonPath);
        }

        // Cleanup AndroidManifest.xml
        const androidManifestPath = path.join(androidProjectRoot, 'app', 'src', 'main', 'AndroidManifest.xml');
        let androidManifestContent = fs.readFileSync(androidManifestPath, 'utf-8');
        const attributtesToRemoveFromAndroidManifest = [
            'xmlns:tools="http://schemas.android.com/tools"',
            'tools:replace="android:supportsRtl"',
            'android:supportsRtl="true"',
            'android:name="com.getcapacitor.core.BBDCordovaApp"'
        ];

        attributtesToRemoveFromAndroidManifest.forEach(function (attribute) {
            const attributeRegExp = new RegExp('\t*' + attribute + '\n')
            androidManifestContent = androidManifestContent.replace(attributeRegExp, '');
        });

        fs.writeFileSync(androidManifestPath, androidManifestContent, 'utf-8');

        // Restore minSdkVersion in variables.gradle
        const variablesGradlePath = path.join(androidProjectRoot, 'variables.gradle');
        if (fs.existsSync(variablesGradlePath)) {
            let variablesGradleContent = fs.readFileSync(variablesGradlePath, 'utf-8');

            variablesGradleContent = variablesGradleContent.replace(/minSdkVersion\s*=\s*\d+/, 'minSdkVersion = 22');
            variablesGradleContent = variablesGradleContent.replace(/cordovaAndroidVersion\s*=\s*'\d+.{1,}\d+'/, 'cordovaAndroidVersion = \'10.1.1\'');
            fs.writeFileSync(variablesGradlePath, variablesGradleContent, 'utf-8');
        }

        const nodeModulesCapacitorAndroidPath = path.join(
            projectRoot, 'node_modules', '@capacitor', 'android',
        );
        const capacitorAndroidPackagePath = path.join(
            nodeModulesCapacitorAndroidPath, 'capacitor', 'src', 'main', 'java', 'com', 'getcapacitor'
        );
        const capacitorAndroidBuildGradlePath = path.join(
            nodeModulesCapacitorAndroidPath, 'capacitor', 'build.gradle'
        );

        // restore build.gradle for capacitor-android project
        let capacitorAndroidBuildGradleContent = fs.readFileSync(capacitorAndroidBuildGradlePath, 'utf-8');
        capacitorAndroidBuildGradleContent = capacitorAndroidBuildGradleContent.replace(applyFromString, '');
        capacitorAndroidBuildGradleContent = capacitorAndroidBuildGradleContent.replace(implementationProjectCapacitorCordovaString, fileTreeString);
        fs.writeFileSync(capacitorAndroidBuildGradlePath, capacitorAndroidBuildGradleContent, 'utf-8');

        // restore Bridge.java
        replaceAndSave(
            path.join(capacitorAndroidPackagePath, 'Bridge.java'),
            BridgeJavaReplacementStrings,
            {
                replacementTextToCheck: '',
                revert: true
            }
        );

        // restore BridgeActivity.java
        replaceAndSave(
            path.join(capacitorAndroidPackagePath, 'BridgeActivity.java'),
            BridgeActivityJavaReplacementStrings,
            {
                replacementTextToCheck: '',
                revert: true
            }
        );

        // restore CapacitorWebView.java
        replaceAndSave(
            path.join(capacitorAndroidPackagePath, 'CapacitorWebView.java'),
            CapacitorWebViewJavaReplacementStrings,
            {
                replacementTextToCheck: '',
                revert: true
            }
        );

        // restore WebViewLocalServer.js
        replaceAndSave(
            path.join(capacitorAndroidPackagePath, 'WebViewLocalServer.java'),
            WebViewLocalServerJavaReplacementStrings,
            {
                replacementTextToCheck: '',
                revert: true
            }
        );

        // remove com.getcapcitor.core package from capacitor-android project
        fs.rmSync(path.join(capacitorAndroidPackagePath, 'core'), { recursive: true, force: true });
    }

    // Cleanup configurations for iOS platform in Capacitor project
    if (fs.existsSync(iosProjectRoot)) {
        try {
            const cordovaPluginsPodsSpecPath = path.join(
                projectRoot, 'ios', 'capacitor-cordova-ios-plugins', 'CordovaPlugins.podspec'),
                capacitorPodFilePath = path.join(projectRoot, 'ios', 'App', 'Podfile'),
                capacitorPodSpecFile = path.join(projectRoot, 'node_modules', '@capacitor', 'ios', 'Capacitor.podspec'),
                capacitorCLIPodsSpecPath = path.join(projectRoot, 'node_modules', '@capacitor', 'cli', 'dist', 'ios', 'update.js');

            replaceAndSave(cordovaPluginsPodsSpecPath, [
                [`s.dependency 'BlackBerryDynamics'`, ''],
                [
                    `s.xcconfig = {'GCC_PREPROCESSOR_DEFINITIONS' => '$(inherited) COCOAPODS=1 WK_WEB_VIEW_ONLY=1 BBD_CAPACITOR=1' }`,
                    `s.xcconfig = {'GCC_PREPROCESSOR_DEFINITIONS' => '$(inherited) COCOAPODS=1 WK_WEB_VIEW_ONLY=1' }`
                ]
            ]);
            replaceAndSave(capacitorPodFilePath, [
                [`platform :ios, '15.0'`, `platform :ios, '13.0'`],
                [/pod 'BlackBerryDynamics', (:podspec|:path) => '(.+)'/, '']
            ]);
            replaceAndSave(capacitorPodSpecFile, [
                [
                    `s.dependency 'CapacitorCordova'\n\ts.dependency 'BlackBerryDynamics'`,
                    `s.dependency 'CapacitorCordova'`
                ]
            ]);
            replaceAndSave(capacitorCLIPodsSpecPath, [
                [`s.dependency 'BlackBerryDynamics'`, '']
            ]);
            removeAssertDeploymentTarget(capacitorPodFilePath);
        } catch (error) {
            console.log(error);
        }

        cleanUpCAPBridgeViewController();

        // restore configs via ruby
        const uninstallPath = path.join(
            projectRoot, 'node_modules', 'capacitor-plugin-bbd-base', 'scripts', 'hooks', 'ios', 'uninstall.rb'
        );

        exec(
            `${uninstallPath} -i ${bundleId} -p "${iosProjectRoot}"`,
            (error) => {
                if (error) {
                    console.log('\nERROR: capacitor-plugin-bbd-base uninstall exited with error!')
                }
            }
        );
    }

    // Remove integration hook from the project
    for (const name of Object.values(hooks)) {
        const hookToRemovePath = path.join(projectRoot, 'hooks', name);
        if (fs.existsSync(hookToRemovePath)) {
            fs.unlinkSync(hookToRemovePath);
        }
    }

})();
