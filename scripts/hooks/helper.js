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
    registerGDStateChangeHandler,
    notificationCenter,
    requireHelperPhrase,
    postInstallPhrase,
    assertDeploymentTargetReplacePhrase,
    headers,
    linkerFlags,
    loadWebView,
    blackBerryLauncherPodPhrase
} from './constants.js';

const projectRoot = process.env.INIT_CWD,
    packageJson = JSON.parse(fs.readFileSync(path.join(projectRoot, 'package.json'), 'utf-8'));

const cAPBridgeViewControllerPath = path.join(
    projectRoot, 'node_modules', '@capacitor', 'ios', 'Capacitor', 'Capacitor', 'CAPBridgeViewController.swift'
);

const podsPhrases = {
    BlackBerryLauncher: blackBerryLauncherPodPhrase,
};

export const checkAndExitOrContinueOnInstall = () => {
    const processArgv = process.argv;
    if (!(processArgv[1] && processArgv[1].indexOf('capacitor-plugin-bbd-base') > -1 &&
        process.env.npm_command === 'install')) {
        process.exit(0);
    }
}
export const checkAndExitOrContinueOnUninstall = () => {
    const processArgv = process.argv;
    if (!(processArgv[1] && processArgv[1].indexOf('capacitor-plugin-bbd-base') > -1 &&
        process.env.npm_command === 'uninstall')) {
        process.exit(0);
    }
}

export const getPackageNameFromAndroidManifest = (pathToAndroidManifest) => {
    const androidManifestContent = fs.readFileSync(pathToAndroidManifest, 'utf-8'),
        startIndexOfPackageString = androidManifestContent.indexOf(
            '"', androidManifestContent.indexOf('package=')
        ) + 1,
        endIndexOfPackageString = androidManifestContent.indexOf('"', startIndexOfPackageString);

    return androidManifestContent.substring(startIndexOfPackageString, endIndexOfPackageString);
}

export const addAttributeToXmlElement = (element, attributeToAdd, xml) => {
    if (!xml.includes(attributeToAdd)) {
        const startIndexOfElementTag = xml.indexOf('<' + element),
            endIndexOfElementStartLine = xml.indexOf('\n', startIndexOfElementTag),
            nextInlineAttribute = xml.substring(startIndexOfElementTag + 1 + element.length, endIndexOfElementStartLine),
            elementIdentationsNumber = (startIndexOfElementTag - xml.lastIndexOf('\n', startIndexOfElementTag)) / 4,
            attributeIndentation = '\t\t'.repeat(elementIdentationsNumber + 1);

        xml = xml.replace(element, element + '\n' + attributeIndentation + attributeToAdd);

        if (nextInlineAttribute.trim()) {
            xml = xml.replace(nextInlineAttribute, '\n' + attributeIndentation + nextInlineAttribute.trim());
        }

        return xml;
    }

    return xml;
}

export const updateLinkerFlags = () => {
    for (const [key, value] of Object.entries(linkerFlags)) {
        if (('cordova-plugin-bbd-' + key) in packageJson.dependencies) {
            addLinkerForBuildType('debug', value);
            addLinkerForBuildType('release', value);
        }
    }
}

export const updateLauncher = () => {
    const podFilePath = path.join(projectRoot, 'ios', 'App', 'Podfile');

    if ('cordova-plugin-bbd-launcher' in packageJson.dependencies) {
        let fileContent = fs.readFileSync(podFilePath, 'utf-8');
        if (fileContent.includes(podsPhrases.BlackBerryLauncher)) {
            return;
        }

        podsPhrases.BlackBerryDynamics = getBlackBerryDynamicsPodPhrase(fileContent);

        replaceAndSave(podFilePath, [
            [
                podsPhrases.BlackBerryDynamics,
                addAfter(podsPhrases.BlackBerryDynamics, podsPhrases.BlackBerryLauncher)
            ]
        ]);
    } else {
        replaceAndSave(podFilePath, [
            [podsPhrases.BlackBerryLauncher, '']
        ]);
    }
}

export const patchCAPBridgeViewController = () => {
    replaceAndSave(cAPBridgeViewControllerPath, [
        [headers.Cordova, `${headers.Cordova}\n${headers.BlackBerry}`],
        [loadWebView, notificationCenter],
        [`// MARK: - Initialization`, `${registerGDStateChangeHandler.join('\n')}\n\t// MARK: - Initialization`]
    ]);
}

export const cleanUpCAPBridgeViewController = () => {
    replaceAndSave(cAPBridgeViewControllerPath, [
        [`${headers.Cordova}\n${headers.BlackBerry}`, headers.Cordova],
        [notificationCenter, loadWebView,],
        [registerGDStateChangeHandler.join('\n'), '']
    ]);
}

export const addAssertDeploymentTarget = (capacitorPodFile) => {
    let podFileContent = fs.readFileSync(
        capacitorPodFile,
        { encoding: "utf-8" }
    ).toString();

    if (podFileContent.includes("assertDeploymentTarget(installer)")) {
        podFileContent = podFileContent.replace(assertDeploymentTargetReplacePhrase, "");
    }

    if (!podFileContent.includes(requireHelperPhrase) && !podFileContent.includes(postInstallPhrase)) {
        podFileContent = requireHelperPhrase + podFileContent + postInstallPhrase;
        fs.writeFileSync(capacitorPodFile, podFileContent, 'utf-8');
    }
}

export const removeAssertDeploymentTarget = (capacitorPodFile) => {
    replaceAndSave(capacitorPodFile, [
        [requireHelperPhrase, ""],
        [postInstallPhrase, assertDeploymentTargetReplacePhrase]
    ]);
}

export const replaceAndSave = (filePath, collection, { replacementTextToCheck = '', revert = false } = {}) => {
    if (!fs.existsSync(filePath)) {
        throw new Error(`File not exists at path ${filePath}`)
    }
    const encoding = { encoding: 'utf8' };

    let fileContent = fs.readFileSync(filePath, encoding);

    if (!replacementTextToCheck || replacementTextToCheck && !fileContent.includes(replacementTextToCheck) || revert ) {
        for (const [target, replacement] of collection) {
            fileContent = revert ? fileContent.replace(replacement, target) : fileContent.replace(target, replacement);
        }

        fs.writeFileSync(filePath, fileContent, encoding);
    }
}

function addLinkerForBuildType(buildType, linker) {
    const xcconfigPath = path.join(
        projectRoot,
        'ios',
        'App',
        'Pods',
        'Target\ Support\ Files',
        'Pods-App',
        'Pods-App.' + buildType + '.xcconfig'
    );

    if (fs.existsSync(xcconfigPath)) {
        replaceAndSave(xcconfigPath, [
            ['-framework "BlackBerryDynamics" ', '-framework "BlackBerryDynamics" ' + linker]
        ]);
    }
}

function getBlackBerryDynamicsPodPhrase(context) {
    const [match] = context.match(/pod 'BlackBerryDynamics', (:podspec|:path) => '(.+)'/);
    return match;
}

function addAfter(phrase, newPhrase) {
    return `${phrase}\n\t${newPhrase}`
}
