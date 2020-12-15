# cordova-plugin-bbd-file

BlackBerry Dynamics Cordova File plugin. It is a fork of [cordova-plugin-file@6.0.1](https://github.com/apache/cordova-plugin-file). 

This plugin enables you to manage the FileSystem residing within the BlackBerry Dynamics secure container using a similar JavaScript API to the original plugin.

## Preconditions
`cordova-plugin-bbd-file` is dependent on `cordova-plugin-bbd-base` plugin.

Installation
============
To add this plugin to your application, run the following command in the project directory:
```
$ cd <path/to/package>/BlackBerry_Dynamics_SDK_for_Cordova_<version>/plugins/cordovaApp
$ cordova plugin add git+https://github.com/blackberry/BlackBerry-Dynamics-Cordova-Plugins#file
```

## Supported Platforms

- Android
- iOS

## File System Layouts
It can be very useful to know how the `cordova.file.*` properties map to physical paths on a real device.

##### iOS 
On iOS the `cordova-plugin-bbd-file` plugin supports all constants listed [here](https://cordova.apache.org/docs/en/latest/reference/cordova-plugin-file/#ios-file-system-layout).

##### Android
On Android the `cordova-plugin-bbd-file` plugin supports all constants listed [here](https://cordova.apache.org/docs/en/latest/reference/cordova-plugin-file/#android-file-system-layout) except the following:
- `externalRootDirectory`
- `externalApplicationStorageDirectory`
- `externalCacheDirectory`
- `externalDataDirectory`

## Usage

Please take a look at the original examples [here](https://github.com/apache/cordova-plugin-file#sample-create-files-and-directories-write-read-and-append-files-).

## File + AppKinetics

The BlackBerry Dynamics Cordova File plugin is compatible with other plugins. One great example is the AppKinetics plugin.

BlackBerry Dynamics Cordova AppKinetics plugin provides `storageLocation` constant that allows to resolve files that were transfered from other Dynamics apps using Dynamics app-based [services](https://marketplace.blackberry.com/services) like [transfer-file](https://marketplace.blackberry.com/services/273065) or were copied from application bundle to Dynamics secure container using `copyFilesToSecureFileSystem()` API.

Details about the AppKinetics module can be found [here](https://developer.blackberry.com/devzone/files/blackberry-dynamics/cordova/GDAppKinetics.html).

#### Example - resolving AppKinetics storage
```javascript
resolveLocalFileSystemURL(window.plugins.GDAppKineticsPlugin.storageLocation,
    function(directoryEntry) {
        var appKineticsDirectoryReader = directoryEntry.createReader();
            appKineticsDirectoryReader.readEntries(function (entries) {
            console.log("AppKinetics storage entries: ", entries);
        }, function(error) {
            console.log("Error: ", error);
        });
    }, function(error) {
        console.log("resolveLocalFileSystemURL error: ", error)
    }
 );
```

## Dynamics logs
> The BlackBerry Dynamics Cordova File plugin has API to work with Dynamics logs.

- `FileSystem.uploadLogs` API allows to upload BlackBerry Dynamics activity logs for support purposes. The logs will be uploaded to a server in the BlackBerry Technology Network Operation Center (NOC). Upload takes place in background and is retried as necessary.
- `FileSystem.exportLogFileToDocumentsFolder` API allows to create a dump of BlackBerry Dynamics activity logs. The logs will be dumped to a file that is outside the secure store, in the `Documents` folder. The file will not be encrypted.

#### Example - uploadLogs

```javascript
requestFileSystem(LocalFileSystem.PERSISTENT, 0, function (fileSystem) {
    fileSystem.uploadLogs(function() {
        console.log("Logs are uploaded to the NOC");
    }, null);
}, null);
```

#### Example - exportLogFileToDocumentsFolder

```javascript
requestFileSystem(LocalFileSystem.PERSISTENT, 0, function (fileSystem) {
    fileSystem.exportLogFileToDocumentsFolder(function() {
        console.log("Logs are exported to the Documents folder");
    }, null);
}, null);
```
