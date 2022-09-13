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

const fs = require('fs'),
    path = require('path'),
    fse = require('fs-extra'),
    projectRoot = process.env.INIT_CWD,
    packageJson = require(path.join(projectRoot, 'package.json'));

const cAPBridgeViewControllerPath = path.join(
    projectRoot, 'node_modules', '@capacitor', 'ios', 'Capacitor', 'Capacitor', 'CAPBridgeViewController.swift'
);

const linkerFlags = {
    application: '-framework "BbdApplicationPlugin" ',
    appkinetics: '-framework "BbdAppKineticsPlugin" ',
    httprequest: '-framework "BbdHttpRequestPlugin" ',
    interappcommunication: '-framework "BbdInterAppCommunicationPlugin" ',
    mailto: '-framework "BbdMailToPlugin" ',
    push: '-framework "BbdPushPlugin" ',
    serversideservices: '-framework "BbdServerSideServicesPlugin" ',
    socket: '-framework "BbdSocketPlugin" ',
    specificpolicies: '-framework "BbdSpecificPoliciesPlugin" ',
    storage: '-framework "BbdStoragePlugin" ',
    tokenhelper: '-framework "BbdTokenHelperPlugin" ',
    websocket: '-framework "BbdWebSocketPlugin" ',
    launcher: '-framework "BbdLauncherPlugin" '
};

const podsPhrases = {
    BlackBerryLauncher: "pod 'BlackBerryLauncher', :path => '../../node_modules/cordova-plugin-bbd-launcher'",
};

const headers = {
    BlackBerry: `import BlackBerryDynamics.Runtime`,
    Cordova: `import Cordova`
};

const loadWebView = `loadWebView()`;
const notificationCenter = `NotificationCenter.default.addObserver(self, selector: #selector(registerGDStateChangeHandler(notification:)), name: NSNotification.Name.GDStateChange, object: nil)`;

const registerGDStateChangeHandler = [
    `@objc func registerGDStateChangeHandler(notification: NSNotification) {`,
    `        if (notification.name == NSNotification.Name.GDStateChange)`,
    `        {`,
    `            let userInfo: NSDictionary = notification.userInfo! as NSDictionary`,
    `            let propertyName = userInfo[GDStateChangeKeyProperty]`,
    ``,
    `            if (propertyName as! String == GDKeyIsAuthorized)`,
    `            {`,
    `                loadWebView()`,
    `            }`,
    `        }`,
    `    }`
];

exports.checkAndExitOrContinueOnInstall = () => {
    const originalNpmConfigArgv = JSON.parse(process.env.npm_config_argv).original,
        filteredOriginal = originalNpmConfigArgv.filter(function (val, i) {
            return !['--save', '--verbose', '--d'].includes(val);
        });

    if (!(filteredOriginal[1] && filteredOriginal[1].indexOf('capacitor-plugin-bbd-base') > -1 &&
        (filteredOriginal.includes('i') || filteredOriginal.includes('install') || filteredOriginal.includes('add')))) {
        process.exit(0);
    }
}

exports.checkAndExitOrContinueOnUninstall = () => {
    const originalNpmConfigArgv = JSON.parse(process.env.npm_config_argv).original,
        filteredOriginal = originalNpmConfigArgv.filter(function (val, i) {
            return !['--save', '--verbose', '--d'].includes(val);
        });

    if (!(filteredOriginal[1] && filteredOriginal[1].indexOf('capacitor-plugin-bbd-base') > -1 &&
        (filteredOriginal.includes('uninstall') || filteredOriginal.includes('remove')))) {
        process.exit(0);
    }
}

exports.getPackageNameFromAndroidManifest = (pathToAndroidManifest) => {
    const androidManifestContent = fs.readFileSync(pathToAndroidManifest, 'utf-8'),
        startIndexOfPackageString = androidManifestContent.indexOf(
            '"', androidManifestContent.indexOf('package=')
        ) + 1,
        endIndexOfPackageString = androidManifestContent.indexOf('"', startIndexOfPackageString);

    return androidManifestContent.substring(startIndexOfPackageString, endIndexOfPackageString);
}

exports.addAttributeToXmlElement = (element, attributeToAdd, xml) => {
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

exports.updateLinkerFlags = () => {
    for (const [key, value] of Object.entries(linkerFlags)) {
        if (('cordova-plugin-bbd-' + key) in packageJson.dependencies) {
            addLinkerForBuildType('debug', value);
            addLinkerForBuildType('release', value);
        }
    }
}

exports.updateLauncher = () => {
    const podFilePath = path.join(projectRoot, 'ios', 'App', 'Podfile');

    if ('cordova-plugin-bbd-launcher' in packageJson.dependencies) {
        let fileContext = fs.readFileSync(podFilePath).toString();
        if (fileContext.includes(podsPhrases.BlackBerryLauncher)) {
            return;
        }

        podsPhrases.BlackBerryDynamics = getBlackBerryDynamicsPodPhrase(fileContext);

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

exports.patchCAPBridgeViewController = () => {
    replaceAndSave(cAPBridgeViewControllerPath, [
        [headers.Cordova, `${headers.Cordova}\n${headers.BlackBerry}`],
        [loadWebView, notificationCenter],
        [`// MARK: - Initialization`, `${registerGDStateChangeHandler.join('\n')}\n\t// MARK: - Initialization`]
    ]);
}

exports.cleanUpCAPBridgeViewController = () => {
    replaceAndSave(cAPBridgeViewControllerPath, [
        [`${headers.Cordova}\n${headers.BlackBerry}`, headers.Cordova],
        [notificationCenter, loadWebView,],
        [registerGDStateChangeHandler.join('\n'), '']
    ]);
}

const targetVersion = '14.0';
const requireHelperPhrase = "require_relative '../../node_modules/" +
    "capacitor-plugin-bbd-base/scripts/hooks/ios/update_deployment_target.rb'" +
    "\n";
const postInstallPhrase = [
    `post_install do |installer|`,
    `   project = Xcodeproj::Project.open('App.xcodeproj')`,
    `   update_deployment_target project, ${targetVersion}`,
    `   project.save`,
    ``,
    `   update_deployment_target installer.pods_project, ${targetVersion}`,
    `end`
].join("\n");
const assertDeploymentTargetReplacePhrase = [
    `post_install do |installer|`,
    `  assertDeploymentTarget(installer)`,
    `end`
].join("\n");

exports.addAssertDeploymentTarget = (capacitorPodFile) => {
    let podFileContent = fse.readFileSync(
        capacitorPodFile,
        { encoding: "utf-8" }
    ).toString();

    if (podFileContent.includes("assertDeploymentTarget(installer)")) {
        podFileContent = podFileContent.replace(assertDeploymentTargetReplacePhrase, "");
    }

    podFileContent = requireHelperPhrase + podFileContent + postInstallPhrase;
    fse.writeFileSync(capacitorPodFile, podFileContent);
}

exports.removeAssertDeploymentTarget = (capacitorPodFile) => {
    replaceAndSave(capacitorPodFile, [
        [requireHelperPhrase, ""],
        [postInstallPhrase, assertDeploymentTargetReplacePhrase]
    ]);
}

function replaceAndSave(filePath, collection) {
    if (!fs.existsSync(filePath)) {
        throw new Error(`File not exists at path ${filePath}`)
    }
    const encoding = { encoding: 'utf8' };

    let fileContext = fs.readFileSync(filePath, encoding).toString();

    for (const [search, replace] of collection) {
        fileContext = fileContext.replace(search, replace);
    }

    fs.writeFileSync(filePath, fileContext, encoding);
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

    if (fse.existsSync(xcconfigPath)) {
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

exports.replaceAndSave = replaceAndSave;
