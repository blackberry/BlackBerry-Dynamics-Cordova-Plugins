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
    BlackBerryDynamics: "pod 'BlackBerryDynamics', :podspec => " +
        "'https://software.download.blackberry.com/repository/framework/dynamics/ios/10.2.0.83/BlackBerryDynamics-10.2.0.83.podspec'",
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

    if (!(filteredOriginal[1] && 
        (filteredOriginal[1].indexOf('capacitor-plugin-bbd-base') > -1 || filteredOriginal[1].indexOf('capacitor-base') > -1) &&
        (filteredOriginal.includes('i') || filteredOriginal.includes('install') || filteredOriginal.includes('add')))) {
        process.exit(0);
    }
}

exports.checkAndExitOrContinueOnUninstall = () => {
    const originalNpmConfigArgv = JSON.parse(process.env.npm_config_argv).original,
        filteredOriginal = originalNpmConfigArgv.filter(function (val, i) {
            return !['--save', '--verbose', '--d'].includes(val);
        });

    if (!(filteredOriginal[1] && 
        (filteredOriginal[1].indexOf('capacitor-plugin-bbd-base') > -1 || filteredOriginal[1].indexOf('capacitor-base') > -1) &&
        (filteredOriginal.includes('uninstall') || filteredOriginal.includes('remove')))) {
        process.exit(0);
    }
}

exports.readBundleIdFromCapacitorConfig = (projectRoot) => {
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
        let fileContext = fs.readFileSync(podFilePath);
        if (fileContext.includes(podsPhrases.BlackBerryLauncher))
            return;

        replaceAndSave(podFilePath, [
            [
                podsPhrases.BlackBerryDynamics,
                `${podsPhrases.BlackBerryDynamics}\n\t${podsPhrases.BlackBerryLauncher}`
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

function replaceAndSave(filePath, collection) {
    if (!fs.existsSync(filePath)) {
        throw new Error(`File not exists at path ${filePath}`)
    }
    const encoding = { encoding: 'utf8' };

    let fileContext = fs.readFileSync(filePath, encoding);

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

exports.replaceAndSave = replaceAndSave;
