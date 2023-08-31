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
import {
    checkAndExitOrContinueOnInstall,
    patchCAPBridgeViewController,
    replaceAndSave,
    addAssertDeploymentTarget
} from './helper.js';

(function () {
    // We should run this script only if we install capacitor-plugin-bbd-base plugin.
    // In other circumstances like 'npm i' or 'yarn' or 'npm uninstall' or 'npm i <other_module>' we should exit.
    // This is becasue sometimes other actions trigger running this script and we need to do setup process again.
    checkAndExitOrContinueOnInstall();

    const projectRoot = process.env.INIT_CWD,
        packageJson = JSON.parse(fs.readFileSync(path.join(projectRoot, 'package.json'), 'utf-8')),
        bbdBasePath = process.cwd(),
        isWindows = process.platform === 'win32';

    // Copy and link integration hook in Capacitor project
    const hooks = {
        'capacitor:copy:after': 'afterCopy.js',
        'capacitor:update:after': 'afterUpdate.js',
        'cleanup': 'bbdCapacitorCleanup.js'
    };

    for (const [script, integrationHookFileName] of Object.entries(hooks)) {
        let hook = `node ./hooks/${integrationHookFileName}`;

        if (packageJson.scripts[script] && packageJson.scripts[script].indexOf(hook) > 0) {
            break;
        }

        fs.cpSync(
            path.join(bbdBasePath, 'scripts', 'hooks', integrationHookFileName),
            path.join(projectRoot, 'hooks', integrationHookFileName)
        );

        if (packageJson.scripts[script] && !packageJson.scripts[script].includes(hook)) {
            hook = `${packageJson.scripts[script]} && ${hook}`;
        }

        packageJson.scripts[script] = hook;
        // Add "type": "module" for projects created from Ionic CLI < 7
        packageJson.type = 'module';
    }

    fs.writeFileSync(path.join(projectRoot, 'package.json'), JSON.stringify(packageJson, null, 2), 'utf-8');

    if (!isWindows) {
        if (!fs.existsSync(path.join(projectRoot, 'ios'))) {
            return;
        }
        const cordovaPluginsPodsSpecPath = path.join(projectRoot, 'node_modules', '@capacitor', 'cli', 'dist', 'ios', 'update.js'),
            capacitorPodFile = path.join(projectRoot, 'ios', 'App', 'Podfile'),
            BlackBerryDependencyPhrase = `s.dependency 'BlackBerryDynamics'`,
            SwiftVersionPhrase = `s.swift_version  = '5.1'`,
            addYourPodsHerePhrase = '# Add your Pods here';

        if (!fs.existsSync(cordovaPluginsPodsSpecPath)) {
            console.log('File not found at path: ', cordovaPluginsPodsSpecPath);
            return;
        }

        const fileContext = fs.readFileSync(cordovaPluginsPodsSpecPath, { encoding: 'utf8' });

        if (fileContext.indexOf(BlackBerryDependencyPhrase) > 0) {
            return;
        }

        replaceAndSave(cordovaPluginsPodsSpecPath, [
            [
                SwiftVersionPhrase,
                `${BlackBerryDependencyPhrase}\n\t${SwiftVersionPhrase}`
            ],
            [
                `s.xcconfig = {'GCC_PREPROCESSOR_DEFINITIONS' => '$(inherited) COCOAPODS=1 WK_WEB_VIEW_ONLY=1' }`,
                `s.xcconfig = {'GCC_PREPROCESSOR_DEFINITIONS' => '$(inherited) COCOAPODS=1 WK_WEB_VIEW_ONLY=1 BBD_CAPACITOR=1' }`
            ]
        ], {
            replacementTextToCheck: 'BBD_CAPACITOR=1'
        });

        // Add bbd dependency to Root pod file
        const { dynamicsPodSpec } = JSON.parse(fs.readFileSync(path.join(bbdBasePath, 'package.json'), 'utf-8'));
        const podsSpecPhrase = `pod 'BlackBerryDynamics', :podspec => '${dynamicsPodSpec}'`;

        replaceAndSave(capacitorPodFile, [
            [/platform :ios, \'([\d\.\d]+)\'/, "platform :ios, '15.0'"],
            [
                addYourPodsHerePhrase,
                `${addYourPodsHerePhrase}\n\t${podsSpecPhrase}`
            ]
        ], {
            replacementTextToCheck: 'BlackBerryDynamics'
        });

        addAssertDeploymentTarget(capacitorPodFile);

        // add dependency to Capacitor pod file
        const capacitorPodSpecFile = path.join(projectRoot, 'node_modules', '@capacitor', 'ios', 'Capacitor.podspec');
        replaceAndSave(capacitorPodSpecFile, [
            [
                `s.dependency 'CapacitorCordova'`,
                `s.dependency 'CapacitorCordova'\n\ts.dependency 'BlackBerryDynamics'`
            ]
        ], {
            replacementTextToCheck: 'BlackBerryDynamics'
        });

        patchCAPBridgeViewController();
    }

})();
