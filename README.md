# capacitor-plugin-bbd-base

BlackBerry Dynamics Capacitor Base plugin. 
> Adds all needed configurations to be able to use `BlackBerry Dynamics` in your Ionic Capacitor application.
It is similar to `cordova-plugin-bbd-base` that adds all needed configurations to be able to use `BlackBerry Dynamics` in Cordova-based or Ionic-Cordova-based projects.
All the other BlackBerry Dynamics Cordova plugins require the Capacitor Base plugin to be installed as dependency.

## Supportability

#### Platforms
- Android
- iOS

#### Node.js
- We recommend to use the latest stable version of Node.js 12.x (LTS).

#### Ionic
- Ionic 6
- `--type=ionic-angular`
- `--type=ionic-react`
- `--type=ionic-vue`
- `--capacitor` integration

## Preconditions

- Install `xcodeproj` and `plist` Ruby gems:
    `$ sudo gem install xcodeproj`
    `$ sudo gem install plist`
    NOTE: required Ruby version >= 2.0.0

## Dynamics SDK Dependancy
Dynamics SDK for iOS and Android are installed as part of the `capacitor-plugin-bbd-base` plugin using CocoaPods & Gradle.
### BlackBerry Dynamics SDK for iOS integration
The integration uses the iOS "Dynamic Framework" version of BlackBerry Dynamics as the static library is no longer supported.
There are a few options to integrate BlackBerry Dynamics SDK for iOS.
#### Using latest released version - default
By default, `capacitor-plugin-bbd-base` plugin will integrate **latest** available BlackBerry Dynamics SDK for iOS using following podspec: `https://software.download.blackberry.com/repository/framework/dynamics/ios/10.2.0.83/BlackBerryDynamics-10.2.0.83.podspec`.
> NOTE: If one of the below integration methods was used there is an option to reset **default** configuration by running following command:
`$ npx set-dynamics-podspec --default`
`$ ionic cap build ios`

#### Using other released version
There is possibility to integrate other released build of BlackBerry Dynamics SDK for iOS.
Following command should be run:
```
$ npx set-dynamics-podspec --url "https://software.download.blackberry.com/repository/framework/dynamics/ios/10.1.0.36/BlackBerryDynamics-10.1.0.36.podspec"
$ ionic cap build ios
```
#### Using locally downloaded version
Also, it is possible to integrate manually downloaded BlackBerry Dynamics SDK for iOS from local place.
Following command should be run:
```
$ npx set-dynamics-podspec --path "/Users/<user>/Downloads/gdsdk-release-dylib-X.X.X.X/BlackBerry_Dynamics_SDK_for_iOS_vX.X.X.X_dylib"
$ ionic cap build ios
```

## BBWebView integration on Android

`BBWebView` has been integrated into the BlackBerry Dynamics Capacitor Base plugin.
It becomes the default webview for Dynamics Ionic-Capacitor applications on Android. This enables the following features:
 - Dynamics Ionic-Capacitor application on Android is loaded via `BBWebView`
 - `XMLHttpRequest` and `fetch` ajax requests are intercepted and routed through Dynamics infrastructure
 - HTML form submissions are intercepted and routed through Dynamics infrastructure
 - `document.cookie` are stored in secure container

## Installation

`$ npm install git+https://github.com/blackberry/blackberry-dynamics-cordova-plugins#capacitor-base`

## Uninstallation

`$ npm uninstall capacitor-plugin-bbd-base`

## Supported Dynamics Cordova plugins

 - **[cordova-plugin-bbd-file](https://github.com/blackberry/BlackBerry-Dynamics-Cordova-Plugins/tree/file)**
 *Installation*: `$ npm install git+https://github.com/blackberry/BlackBerry-Dynamics-Cordova-Plugins#file`
 *Uninstallation*: `$ npm uninstall cordova-plugin-bbd-file`
 - **[cordova-plugin-bbd-file-transfer](https://github.com/blackberry/BlackBerry-Dynamics-Cordova-Plugins/tree/file-transfer)**
 *Installation*: `$ npm install git+https://github.com/blackberry/BlackBerry-Dynamics-Cordova-Plugins#file-transfer`
 *Uninstallation*: `$ npm uninstall cordova-plugin-bbd-file-transfer`
 - **[cordova-plugin-bbd-sqlite-storage](https://github.com/blackberry/BlackBerry-Dynamics-Cordova-Plugins/tree/sqlite-storage)**
 *Installation*: `$ npm install git+https://github.com/blackberry/BlackBerry-Dynamics-Cordova-Plugins#sqlite-storage`
 *Uninstallation*: `$ npm uninstall cordova-plugin-bbd-sqlite-storage`
 - **[cordova-plugin-bbd-inappbrowser](https://github.com/blackberry/BlackBerry-Dynamics-Cordova-Plugins/tree/inappbrowser)**
 *Installation*: `$ npm install git+https://github.com/blackberry/BlackBerry-Dynamics-Cordova-Plugins#inappbrowser`
 *Uninstallation*: `$ npm uninstall cordova-plugin-bbd-inappbrowser`
 - **[cordova-plugin-bbd-media-capture](https://github.com/blackberry/BlackBerry-Dynamics-Cordova-Plugins/tree/media-capture)**
 *Installation*: `$ npm install git+https://github.com/blackberry/BlackBerry-Dynamics-Cordova-Plugins#media-capture`
 *Uninstallation*: `$ npm uninstall cordova-plugin-bbd-media-capture`
 - **cordova-plugin-bbd-appkinetics**
 *Installation*: `$ npm install cordova-plugin-bbd-appkinetics`
 *Uninstallation*: `$ npm uninstall cordova-plugin-bbd-appkinetics`
 - **cordova-plugin-bbd-application**
 *Installation*: `$ npm install cordova-plugin-bbd-application`
 *Uninstallation*: `$ npm uninstall cordova-plugin-bbd-application`
 - **cordova-plugin-bbd-httprequest**
 *Installation*: `$ npm install cordova-plugin-bbd-httprequest`
 *Uninstallation*: `$ npm uninstall cordova-plugin-bbd-httprequest`
 - **cordova-plugin-bbd-interappcommunication**
 *Installation*: `$ npm install cordova-plugin-bbd-interappcommunication`
 *Uninstallation*: `$ npm uninstall cordova-plugin-bbd-interappcommunication`
 - **cordova-plugin-bbd-launcher**
 *Installation*: `$ npm install cordova-plugin-bbd-launcher`
 *Uninstallation*: `$ npm uninstall cordova-plugin-bbd-launcher`
 - **cordova-plugin-bbd-mailto**
 *Installation*: `$ npm install cordova-plugin-bbd-mailto`
 *Uninstallation*: `$ npm uninstall cordova-plugin-bbd-mailto`
 - **cordova-plugin-bbd-push**
 *Installation*: `$ npm install cordova-plugin-bbd-push`
 *Uninstallation*: `$ npm uninstall cordova-plugin-bbd-push`
 - **cordova-plugin-bbd-serversideservices**
 *Installation*: `$ npm install cordova-plugin-bbd-serversideservices`
 *Uninstallation*: `$ npm uninstall cordova-plugin-bbd-serversideservices`
 - **cordova-plugin-bbd-socket**
 *Installation*: `$ npm install cordova-plugin-bbd-socket`
 *Uninstallation*: `$ npm uninstall cordova-plugin-bbd-socket`
 - **cordova-plugin-bbd-specificpolicies**
 *Installation*: `$ npm install cordova-plugin-bbd-specificpolicies`
 *Uninstallation*: `$ npm uninstall cordova-plugin-bbd-specificpolicies`
 - **cordova-plugin-bbd-storage**
 *Installation*: `$ npm install cordova-plugin-bbd-storage`
 *Uninstallation*: `$ npm uninstall cordova-plugin-bbd-storage`
 - **cordova-plugin-bbd-tokenhelper**
 *Installation*: `$ npm install cordova-plugin-bbd-tokenhelper`
 *Uninstallation*: `$ npm uninstall cordova-plugin-bbd-tokenhelper`
 - **cordova-plugin-bbd-websocket**
 *Installation*: `$ npm install cordova-plugin-bbd-websocket`
 *Uninstallation*: `$ npm uninstall cordova-plugin-bbd-websocket`

## Dynamics Ionic-Capacitor samples

 - [Secure-ICC-Ionic-Capacitor-Angular](https://github.com/blackberry/BlackBerry-Dynamics-Cordova-Samples/tree/master/Secure-ICC-Ionic-Capacitor-Angular)

## Examples of usage

#### New Ionic-Capacitor project
Create project:
`$ ionic start DynamicsCapacitorApp <app-template> --capacitor --package-id=<app-id>`
`$ cd <path>/DynamicsCapacitorApp`
Add platforms:
`$ ionic cap add ios`
OR/AND
`$ ionic cap add android`
Sync project:
`$ ionic cap sync`
Add Capacitor Base plugin:
`$ npm install git+https://github.com/blackberry/blackberry-dynamics-cordova-plugins#capacitor-base`

**Add other supported Dynamics Cordova plugins here ...**

Build project:
`$ ionic cap build ios`
OR/AND
`$ ionic cap build android`
Run the app via IDE (Xcode or Android Studio) or use following command:
`$ ionic cap run ios`
OR/AND
`$ ionic cap run android`

> More details about Ionic CLI can be found [here](https://ionicframework.com/docs/cli/commands/start).

#### Existing Ionic-Capacitor project
`$ cd <app>`
Add platforms if applicable:
`$ ionic cap add ios`
OR/AND
`$ ionic cap add android`
Sync project:
`$ ionic cap sync`
Add Capacitor Base plugin:
`$ npm install git+https://github.com/blackberry/blackberry-dynamics-cordova-plugins#capacitor-base`

**Add other supported Dynamics Cordova plugins here ...**

Build project:
`$ ionic cap build ios`
OR/AND
`$ ionic cap build android`
Run the app via IDE (Xcode or Android Studio) or use following command:
`$ ionic cap run ios`
OR/AND
`$ ionic cap run android`

## License

Apache 2.0 License

## Disclaimer

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
