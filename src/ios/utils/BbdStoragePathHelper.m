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

#import "BbdStoragePathHelper.h"
#import <BlackBerryDynamics/GD/GDFileManager.h>

@implementation BbdStoragePathHelper

+ (NSString*)secureStoragePath
{
    NSArray *paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);

    return [paths firstObject];
}

+ (NSString*)relativePathFromFullPath:(NSString*)fullPath
{
    NSRange storagePathRange = [fullPath rangeOfString:[self secureStoragePath]];

    if(storagePathRange.location != NSNotFound) {
        return [fullPath stringByReplacingCharactersInRange:storagePathRange withString:@""];
    } else {
        return fullPath;
    }

}

+ (BOOL)isPathContainsSecureStoragePath:(NSString*)path
{
    return [path rangeOfString:[self secureStoragePath]].location != NSNotFound;
}

+ (NSString*)fullPathWithStoragePath:(NSString*)path
{
    return [[self secureStoragePath] stringByAppendingPathComponent:path];
}

+ (NSString*)fullPathWithAppkineticsPath:(NSString*)path
{
    return [[[self secureStoragePath] stringByAppendingPathComponent:@"Inbox"] stringByAppendingPathComponent:path];
}

// This method takes inboxPath string in following format:
// "/Inbox/application_sender_id/timestamp/sender_secure_storage_path/received_file_name"
// Output string path has following format:
// "/application_sender_id/timestamp/received_file_name" - it is what we send back to JS
// Actual file location in secure storage is following:
// "receiver_secure_storage_path/Inbox/application_sender_id/timestamp/received_file_name"
+ (NSString*)relativePathFromInboxPath:(NSString*)inboxPath
{
    NSString *fileName = [inboxPath lastPathComponent];
    NSString *inboxFolderPath = nil;
    NSArray *inboxPathComponents = [inboxPath pathComponents];
    if ([inboxPathComponents count] > 3)
    {
        NSArray *inboxDirectoryPathComponents = [[inboxPath pathComponents] subarrayWithRange:NSMakeRange(2, 3)];
        inboxFolderPath = [inboxDirectoryPathComponents componentsJoinedByString:@"/"];
        inboxFolderPath = [inboxFolderPath hasPrefix:@"/"] ? inboxFolderPath : [@"/" stringByAppendingString:inboxFolderPath];
    }
    else
    {
        // imposible case. Just to be safe. The path always matchs the pattern: "/Inbox/application_sender_id/time_stamp/path/to/file/from/sender/file_name.extention"
        inboxFolderPath = @"/";
    }

    return [inboxFolderPath stringByAppendingPathComponent:fileName];
}

@end
