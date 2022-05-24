/**
 * Copyright (c) 2022 BlackBerry Limited. All Rights Reserved.
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

#import <Foundation/Foundation.h>

/**
 * Implementation of BbdStoragePathHelper.
 * Use to operate with paths.
 */

@interface BbdStoragePathHelper : NSObject

/**
 * @method secureStoragePath
 * Calculate the path to secure storage.
 * @return Full path to secure storage.
 */
+ (NSString*)secureStoragePath;

/**
 * @method relativePathFromFullPath
 * Calculate the path relatively secure storage path from full path passed.
 * @param fullPath Full path (contains the secure storage path)
 * @return The path relatively secure storage path.
 */
+ (NSString*)relativePathFromFullPath:(NSString*) fullPath;

/**
 * @method isPathContainsSecureStoragePath
 * This method used for processing the path and indicate the secure storage path in passed
 * @param path NSString object
 * @return YES if passed path contains secure storage path, otherwise NO.
 */
+ (BOOL)isPathContainsSecureStoragePath:(NSString*)path;

/**
 * @method fullPathWithStoragePath
 * This method is used for processing full path from relative path passed
 * @param path
 * @return the full path with secure storage path
 */
+ (NSString*)fullPathWithStoragePath:(NSString*)path;

/**
 * @method fullPathWithAppkineticsPath
 * This method is used for processing full path for AppKinetics from relative path passed
 * @param path
 * @return the full path for AppKinetics with secure storage path
 */
+ (NSString*)fullPathWithAppkineticsPath:(NSString*)path;

/**
 * @method relativePathFromInboxPath
 * This method is used for processing the path to file received from Sender via AppKinetics
 * @param inboxPath NSSString - represents the path to received file
 * @return the path to recieved file without sender secure storage path
 */
+ (NSString*)relativePathFromInboxPath:(NSString *)inboxPath;

@end
