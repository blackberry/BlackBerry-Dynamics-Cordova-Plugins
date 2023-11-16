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

// iOS constants
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
const notificationCenter = `NotificationCenter.default.addObserver(self, selector: #selector(registerGDStateChangeHandler(notification:)), name: NSNotification.Name.GDStateChange, object: nil)`;
const requireHelperPhrase = "require_relative '../../node_modules/" +
    "capacitor-plugin-bbd-base/scripts/hooks/ios/update_deployment_target.rb'" +
    "\n";
const capacitorPodsHelperPhrase = `require_relative '../../node_modules/@capacitor/ios/scripts/pods_helpers'`;
const targetVersion = '15.0';
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
const loadWebView = `loadWebView()`;
const blackBerryLauncherPodPhrase = `pod 'BlackBerryLauncher', :path => '../../node_modules/cordova-plugin-bbd-launcher'`;
const headers = {
    BlackBerry: `import BlackBerryDynamics.Runtime`,
    Cordova: `import Cordova`
};
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

// Android constants
const fileTreeString = `implementation fileTree(dir: 'libs', include: ['*.jar'])`;
const implementationProjectCapacitorCordovaString = `implementation project(':capacitor-cordova-android-plugins')
    ${fileTreeString}`;
const applyFromString = `
apply from: "../../../../android/capacitor-cordova-android-plugins/cordova.variables.gradle"
apply from: "../../../capacitor-plugin-bbd-base/scripts/gradle/bbd.gradle"
`;

const BridgeJavaReplacementStrings = [
    [
        'import org.json.JSONException;',
        `import org.json.JSONException;

import org.json.JSONException;
import androidx.activity.result.ActivityResultRegistry;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityOptionsCompat;
import com.getcapacitor.core.webview.engine.BBDCordovaWebChromeClient;
import com.getcapacitor.core.webview.engine.BBDCordovaWebViewEngine;
import java.util.UUID;`
    ],
    [
        `webView.setWebChromeClient(new BridgeWebChromeClient(this));
        webView.setWebViewClient(this.webViewClient);`,
        `BBDCordovaWebViewEngine bbdCordovaWebViewEngine = new BBDCordovaWebViewEngine(context, preferences, localServer, this);
        BBDCordovaWebViewEngine.BBDCordovaWebViewClient bbdCordovaWebViewClient = new BBDCordovaWebViewEngine.BBDCordovaWebViewClient(getContext());
        webView.setWebViewClient(bbdCordovaWebViewClient);
        webView.setWebChromeClient(new BBDCordovaWebChromeClient(bbdCordovaWebViewEngine));`
    ],
    [
        `if (fragment != null) {
            return fragment.registerForActivityResult(contract, callback);
        } else {
            return context.registerForActivityResult(contract, callback);
        }`,
        `return new ActivityResultRegistry() {
            @Override
            public <I, O> void onLaunch(int requestCode, @NonNull ActivityResultContract<I, O> contract, I input, @Nullable ActivityOptionsCompat options) {
            }
        }.register(UUID.randomUUID().toString(), contract, callback);`
    ],
];

const BridgeActivityJavaReplacementStrings = [
    [
        'import android.content.Intent;',
        `import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import com.getcapacitor.core.BBDLifeCycle;
import com.good.gd.GDAndroid;
import com.good.gd.GDStateAction;
import com.good.gd.cordova.core.launcher.BBDLauncherManager;`
    ],
    [
        'onCreate(Bundle savedInstanceState) {',
        `onCreate(Bundle savedInstanceState) {
        GDAndroid.getInstance().activityInit(this);`
    ],
    [
        'Logger.debug("Starting BridgeActivity");',
        `if (BBDLifeCycle.getInstance().isAuthorized()) {
            Logger.debug("Starting BridgeActivity");`
    ],
    [
        'this.onNewIntent(getIntent());',
        `this.onNewIntent(getIntent());

            BBDLifeCycle.getInstance().initLauncher();
            BBDLauncherManager.getInstance().setAppAuthorized();
        } else {
            registerDynamicsReceiver();
        }
    }

    private void registerDynamicsReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(GDStateAction.GD_STATE_AUTHORIZED_ACTION);

        final GDAndroid bbdRuntime = GDAndroid.getInstance();

        bbdRuntime.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (intent.getAction()) {
                    case GDStateAction.GD_STATE_AUTHORIZED_ACTION:
                        load();
                        // unregister BroadcastReceiver to prevent View reload after application unlock
                        bbdRuntime.unregisterReceiver(this);
                        break;

                    case GDStateAction.GD_STATE_LOCKED_ACTION:
                        BBDLauncherManager.getInstance().setAppUnauthorized();
                        break;

                    case GDStateAction.GD_STATE_UPDATE_CONFIG_ACTION:
                        BBDLauncherManager.getInstance().onUpdateConfig();
                        break;

                    case GDStateAction.GD_STATE_UPDATE_POLICY_ACTION:
                        BBDLauncherManager.getInstance().onUpdatePolicy();
                        break;

                    default:
                        break;
                }
            }
        }, intentFilter);`
    ],
    [
        'bridge.saveInstanceState(outState);',
        `if (BBDLifeCycle.getInstance().isAuthorized()) {
            bridge.saveInstanceState(outState);
        }`
    ],
    [
        'this.bridge.onStart();',
        `if (BBDLifeCycle.getInstance().isAuthorized()) {
            this.bridge.onStart();
        }`
    ],
    [
        'this.bridge.onRestart();',
        `if (BBDLifeCycle.getInstance().isAuthorized()) {
            this.bridge.onRestart();
        }`
    ],
    [
        'bridge.getApp().fireStatusChange(true);',
        `if (BBDLifeCycle.getInstance().isAuthorized()) {
            bridge.getApp().fireStatusChange(true);`
    ],
    [
        'this.bridge.onResume();',
        `this.bridge.onResume();
        }`
    ],
    [
        'this.bridge.onPause();',
        `if (BBDLifeCycle.getInstance().isAuthorized()) {
            this.bridge.onPause();
        }`
    ],
    [
        'activityDepth = Math.max(0, activityDepth - 1);',
        `if (BBDLifeCycle.getInstance().isAuthorized()) {
            activityDepth = Math.max(0, activityDepth - 1);`
    ],
    [
        'this.bridge.onStop();',
        `this.bridge.onStop();
        }`
    ],
    [
        'this.bridge.onDestroy();',
        `if (BBDLifeCycle.getInstance().isAuthorized()) {
            this.bridge.onDestroy();
        }`
    ],
    [
        'this.bridge.onDetachedFromWindow();',
        `if (BBDLifeCycle.getInstance().isAuthorized()) {
            this.bridge.onDetachedFromWindow();
        }`
    ]
];

const CapacitorWebViewJavaReplacementStrings = [
    [
        'public class CapacitorWebView extends WebView {',
        `import com.blackberry.bbwebview.BBWebView;

public class CapacitorWebView extends BBWebView {`
    ]
];

const WebViewLocalServerJavaReplacementStrings = [
    [
        'private boolean isLocalFile',
        'public static boolean isLocalFile'
    ]
];


export {
    // iOS
    registerGDStateChangeHandler,
    notificationCenter,
    requireHelperPhrase,
    capacitorPodsHelperPhrase,
    postInstallPhrase,
    assertDeploymentTargetReplacePhrase,
    headers,
    linkerFlags,
    loadWebView,
    blackBerryLauncherPodPhrase,
    // Android
    fileTreeString,
    implementationProjectCapacitorCordovaString,
    applyFromString,
    BridgeJavaReplacementStrings,
    BridgeActivityJavaReplacementStrings,
    CapacitorWebViewJavaReplacementStrings,
    WebViewLocalServerJavaReplacementStrings
};
