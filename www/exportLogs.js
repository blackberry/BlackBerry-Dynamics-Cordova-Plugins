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
        FileError = require('cordova-plugin-bbd-file.FileError'),
        FileSystem = require('cordova-plugin-bbd-file.FileSystem');

    /**
     * @function GDFileSystem#exportLogFileToDocumentsFolder
     * @description Call this function to create a dump of BlackBerry Dynamics activity logs.
     * The logs will be dumped to a file that is outside the secure store, in the Documents folder.
     * The file will not be encrypted.
     *
     * @example
     * requestFileSystem(LocalFileSystem.PERSISTENT, 0, function (fileSystem) {
     *     fileSystem.exportLogFileToDocumentsFolder(function() {
     *         console.log("Logs are exported to the Documents folder");
     *     }, null);
     * }, null);
     */
    FileSystem.prototype.exportLogFileToDocumentsFolder = function(successCallback, errorCallback) {
        var win = typeof successCallback !== 'function' ? null : function(result) {
            successCallback();
        };
        var fail = typeof errorCallback !== 'function' ? null : function(code) {
            errorCallback(new FileError(code));
        };
        cordovaExec(win, fail, "File", "exportLogFileToDocumentsFolder", []);
    };

    /**
     * @function FileSystem#uploadLogs
     * @description Call this function to upload BlackBerry Dynamics activity logs for support purposes.
     * The logs will be uploaded to a server in the BlackBerry Technology Network Operation Center (NOC).
     * Upload takes place in background and is retried as necessary.
     *
     * @example
     * requestFileSystem(LocalFileSystem.PERSISTENT, 0, function (fileSystem) {
     *     fileSystem.uploadLogs(function() {
     *         console.log("Logs are uploaded to the NOC");
     *     }, null);
     * }, null);
     */
    FileSystem.prototype.uploadLogs = function(successCallback, errorCallback) {
        var win = typeof successCallback !== 'function' ? null : function(result) {
            successCallback();
        };
        var fail = typeof errorCallback !== 'function' ? null : function(code) {
            errorCallback(new FileError(code));
        };
        cordovaExec(win, fail, "File", "uploadLogs", []);
    };

})();
