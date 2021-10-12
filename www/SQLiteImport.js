/**
 * Copyright (c) 2020 BlackBerry Limited. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

;
(function() {
    var cordovaExec = require('cordova/exec'),
        FileEntry = require('cordova-plugin-bbd-file.FileEntry'),
        FileError = require('cordova-plugin-bbd-file.FileError');

    //***************************** sqlite3enc_import ********************************//

    /**
     * @class GDSQLite3encImport
     *
     * @classdesc Wrapper for sqlite3enc_import method
     */

    /**
     * @function GDSQLite3encImport#sqlite3enc_import
     * @description This method is to import pre-populated SQLite DB from www/data folder.
     * It is used in combination with window.plugins.GDAppKineticsPlugin.copyFilesToSecureFileSystem
     * to copy DB file from www/data to Dynamics secure container and then import DB using Dynamics SQLite API.
     * Secure Database object will be returned. Use it to manipulate the data.
     * @property {string} srcFilename Full path, within the secure file system, of the plain SQLite database file to be imported.
     * @property {string} destFilename Name of the database to be created (iOS: in "Documents" directory).
     * If the database already exists, its contents will be overwritten.
     * @param {Function} successCallback Callback to be invoked when upload has completed
     * @param {Function} errorCallback Callback to be invoked upon error
     *
     *
     * @example
     * function success(dbFile) {
     *     console.log("Imported Database Path: " + dbFile.fullPath);
     *     var db = window.sqlitePlugin.openDatabase({ name: dbFile.name, iosDatabaseLocation: 'Documents' });
     * }
     *
     * function fail(error) {
     *     alert(error.code);
     * }
     *
     * requestFileSystem(LocalFileSystem.APPKINETICS, 0, function(gdFileSystem) {
     *     window.plugins.GDAppKineticsPlugin.copyFilesToSecureFileSystem(function(result) {
     *         var options = {create: false, exclusive: false};
     *
     *         gdFileSystem.root.getFile("/data/sql/testDB.sqlite", options, function(fileEntry) {
     *             sqlite3enc_import(fileEntry.fullPath, "SQLite3enc.db", success, fail);
     *         }, fail);
     *     });
     * }, null);
     */

    var gdSQLite3enc_import = function(srcFilename, destFilename, successCallback, errorCallback) {
        var win = typeof successCallback !== 'function' ? null : function(result) {
            var fs = new (require('cordova-plugin-bbd-file.FileSystem'))('persistent'),
                dbFileEntry = new FileEntry(result.name, result.fullPath, fs);
            successCallback(dbFileEntry);
        };
        var fail = typeof errorCallback !== 'function' ? null : function(code) {
            errorCallback(new FileError(code));
        };

        cordovaExec(win, fail, "BBDSQLitePlugin", "sqlite3enc_import", [srcFilename, destFilename]);
    };

    Object.defineProperty(gdSQLite3enc_import, 'toString', {
        value: function() {
            return 'function gdSQLite3enc_import() { [native code] }';
        }
    });

    sqlite3enc_import = gdSQLite3enc_import;

    module.exports = sqlite3enc_import;
})();
