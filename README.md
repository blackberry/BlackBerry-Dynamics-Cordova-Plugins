# cordova-plugin-bbd-sqlite-storage

BlackBerry Dynamics Cordova SQLite Storage plugin. It is a fork of [cordova-plugin-sqlite-storage@4.0.0](https://github.com/litehelpers/Cordova-sqlite-storage). 

This plugin enables you to securely create and manage an SQLite database within the BlackBerry Dynamics secure container using a similar JavaScript SQLite API to the original plugin.

For more details please refer to the [com.good.gd.database](https://developer.blackberry.com/devzone/files/blackberry-dynamics/android/namespacecom_1_1good_1_1gd_1_1database.html) package on Android and the [sqlite3enc](https://developer.blackberry.com/devzone/files/blackberry-dynamics/ios/sqlite.html) Dynamics runtime feature on iOS.

## Preconditions
`cordova-plugin-bbd-sqlite-storage` is dependent on `cordova-plugin-bbd-base` and `cordova-plugin-bbd-file` plugins.

## Installation

`cordova plugin add git+https://github.com/blackberry/blackberry-dynamics-cordova-plugins#sqlite-storage`

## Uninstallation

`cordova plugin rm cordova-plugin-bbd-sqlite-storage`

## Supported Platforms

- Android
- iOS

## Usage

Please take a look at the original examples [here](https://github.com/xpbrew/cordova-sqlite-storage#Usage).

## Importing pre-populated Database
> Importing a pre-populated database is supported by using `window.sqlite3enc_import` API.

#### Example: File + SQLite Storage + AppKinetics
> NOTE: in the example below testDB.sqlite should be located in `www/data/sql/testDB.sqlite`.

For more details please take a look at [copyFilesToSecureFileSystem](https://developer.blackberry.com/devzone/files/blackberry-dynamics/cordova/GDAppKinetics.html#copyFilesToSecureFileSystem) and [sqlite3enc_import](https://developer.blackberry.com/devzone/files/blackberry-dynamics/cordova/GDSQLite3encImport.html) API reference.

```javascript
function success(dbFile) {
    console.log("Imported Database Path: " + dbFile.fullPath);
    var db = window.sqlitePlugin.openDatabase({ name: dbFile.name, iosDatabaseLocation: 'Documents' });
}

function fail(error) {
    alert(error.code);
}
requestFileSystem(LocalFileSystem.APPKINETICS, 0, function(gdFileSystem) {
   window.plugins.GDAppKineticsPlugin.copyFilesToSecureFileSystem(function(result) {
        var options = {create: false, exclusive: false};
        
        gdFileSystem.root.getFile("/data/sql/testDB.sqlite", options, function(fileEntry) {
            sqlite3enc_import(fileEntry.fullPath, "SQLite3enc.db", success, fail);
        }, fail);
    });
}, null);
```

## License

Apache 2.0 License

## Disclaimer

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
