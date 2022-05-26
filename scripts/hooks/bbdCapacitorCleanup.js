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
    const { checkAndExitOrContinueOnUninstall, replaceAndSave, readBundleIdFromCapacitorConfig } = require('./helper');

    // We should run this script only if we uninstall capacitor-plugin-bbd-base plugin.
    // In other circumstances like 'npm i' or 'yarn' or 'npm uninstall' or 'npm i <other_module>' we should exit.
    // This is becasue sometimes other actions trigger running this script and we need to do setup process again.
    checkAndExitOrContinueOnUninstall();

    const path = require('path'),
        fs = require('fs'),
        fse = require('fs-extra'),
        { exec } = require('child_process'),
        projectRoot = process.env.INIT_CWD,
        bbdBasePath = process.cwd(),
        androidProjectRoot = path.join(projectRoot, 'android'),
        iosProjectRoot = path.join(projectRoot, 'ios'),
        packageJsonObj = require(path.join(projectRoot, 'package.json')),
        bundleId = readBundleIdFromCapacitorConfig(projectRoot),
        { getPackageNameFromAndroidManifest, cleanUpCAPBridgeViewController } = require('./helper');

    // Remove integration hook from package.json
    const hooks = {
        afterCopy: 'afterCopy.js',
        afterUpdate: 'afterUpdate.js'
    };

    const hookScriptPath = (name, spaceBefore = false) => `${spaceBefore ? ' ' : ''}&& node ./hooks/${name}`;

    [
        'capacitor:copy:after',
        'capacitor:update:after'
    ].forEach(hookType => {
        let script = packageJsonObj.scripts[hookType];
        if (script.includes(hookScriptPath(hooks.afterCopy)) || script.includes(hookScriptPath(hooks.afterUpdate))) {
            packageJsonObj.scripts[hookType] = script
                .replace(hookScriptPath(hooks.afterCopy, true), '')
                .replace(hookScriptPath(hooks.afterUpdate, true), '');
        } else {
            delete packageJsonObj.scripts[hookType];
        }
    });

    fse.outputJsonSync(path.join(projectRoot, 'package.json'), packageJsonObj, 'utf-8');

    // Remove integration hook from the project
    for (const name of Object.values(hooks)) {
        const hookToRemovePath = path.join(projectRoot, 'hooks', name);
        if (fs.existsSync(hookToRemovePath)) {
            fs.unlinkSync(hookToRemovePath);
        }
    }

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
        const bridgeActivityRegExp = /([a-zA-Z0-9]+\.)+(BridgeActivity")/;

        androidManifestContent = androidManifestContent.replace(
            bridgeActivityRegExp, bundleId + '.MainActivity"'
        );

        const attributtesToRemoveFromAndroidManifest = [
            'xmlns:tools="http://schemas.android.com/tools"',
            'tools:replace="android:supportsRtl"',
            'android:supportsRtl="true"',
            'android:name="com.good.gd.cordova.core.BBDCordovaApp"'
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

            variablesGradleContent = variablesGradleContent.replace(/minSdkVersion\s*=\s*\d+/, 'minSdkVersion = 21');
            variablesGradleContent = variablesGradleContent.replace(/cordovaAndroidVersion\s*=\s*'\d+.{1,}\d+'/, 'cordovaAndroidVersion = \'7.0.0\'');
            fs.writeFileSync(variablesGradlePath, variablesGradleContent, 'utf-8');
        }

        // Restore import in MainActivity.java
        const projectPackageName = getPackageNameFromAndroidManifest(androidManifestPath),
            mainActivityPath = path.join(androidProjectRoot, 'app', 'src', 'main', 'java', ...projectPackageName.split('.'), 'MainActivity.java');

        if (fs.existsSync(mainActivityPath)) {
            let mainActivityContent = fs.readFileSync(mainActivityPath, 'utf-8');

            mainActivityContent = mainActivityContent.replace(
                'import com.good.gd.cordova.capacitor.BridgeActivity;',
                'import com.getcapacitor.BridgeActivity;'
            );

            fs.writeFileSync(mainActivityPath, mainActivityContent, 'utf-8');
        }

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
                [`platform :ios, '14.0'`, `platform :ios, '12.0'`],
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

})();
