/*
 Copyright (c) 2020 BlackBerry Limited. All Rights Reserved.
 Some modifications to the original Cordova FileTransfer plugin
 from https://github.com/apache/cordova-plugin-file-transfer/
 
 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.
 */

#import <Foundation/Foundation.h>
#import <Cordova/CDVPlugin.h>
#import "CDVFileBD.h"
#import <GD/GDFileHandle.h>

enum CDVFileTransferErrorBD {
    FILE_NOT_FOUND_ERR = 1,
    INVALID_URL_ERR = 2,
    CONNECTION_ERR = 3,
    CONNECTION_ABORTED = 4,
    NOT_MODIFIED = 5
};
typedef int CDVFileTransferErrorBD;

enum CDVFileTransferDirectionBD {
    CDV_TRANSFER_UPLOAD = 1,
    CDV_TRANSFER_DOWNLOAD = 2,
};
typedef int CDVFileTransferDirectionBD;

// Magic value within the options dict used to set a cookie.
extern NSString* const kOptionsKeyCookie;

@interface CDVFileTransferBD : CDVPlugin <NSURLSessionDelegate, NSURLSessionDownloadDelegate, NSURLSessionDataDelegate, NSURLSessionTaskDelegate> {}

- (void)upload:(CDVInvokedUrlCommand*)command;
- (void)download:(CDVInvokedUrlCommand*)command;
- (NSString*)escapePathComponentForUrlString:(NSString*)urlString;

// Visible for testing.
- (NSURLRequest*)requestForUploadCommand:(CDVInvokedUrlCommand*)command fileData:(NSData*)fileData;
- (NSMutableDictionary*)createFileTransferError:(int)code AndSource:(NSString*)source AndTarget:(NSString*)target;

- (NSMutableDictionary*)createFileTransferError:(int)code
                                      AndSource:(NSString*)source
                                      AndTarget:(NSString*)target
                                  AndHttpStatus:(int)httpStatus
                                        AndBody:(NSString*)body;
@property (nonatomic, strong) NSOperationQueue* queue;
@property (readonly) NSMutableDictionary* activeTransfers;
@end

@class CDVFileTransferEntityLengthRequestBD;

@interface CDVFileTransferDelegateBD : NSObject <NSURLSessionDelegate, NSURLSessionDownloadDelegate, NSURLSessionDataDelegate, NSURLSessionTaskDelegate> {}

- (void)updateBytesExpected:(long long)newBytesExpected;
- (void)cancelTransfer:(NSURLConnection*)connection;


- (void) cancelSessionTask:(NSURLSessionDataTask*) dataTask;

@property (strong) NSMutableData* responseData; // atomic
@property (nonatomic, strong) NSDictionary* responseHeaders;
@property (nonatomic, assign) UIBackgroundTaskIdentifier backgroundTaskID;
@property (nonatomic, strong) CDVFileTransferBD* command;
@property (nonatomic, assign) CDVFileTransferDirectionBD direction; 
@property (nonatomic, copy) NSString* callbackId;
@property (nonatomic, copy) NSString* objectId;
@property (nonatomic, copy) NSString* source;
@property (nonatomic, copy) NSString* target;
@property (nonatomic, copy) NSURL* targetURL;
@property (nonatomic, copy) NSString* mimeType;
@property (assign) int responseCode; // atomic
@property (nonatomic, assign) long long bytesTransfered;
@property (nonatomic, assign) long long bytesExpected;
@property (nonatomic, assign) BOOL trustAllHosts;
@property (strong) GDFileHandle* targetFileHandle;
@property (nonatomic, strong) CDVFileTransferEntityLengthRequestBD* entityLengthRequest;
@property (nonatomic, strong) CDVFileBD *filePlugin;
@property (nonatomic, assign) BOOL chunkedMode;

//sesion
@property (nonatomic, strong) NSURLSessionDataTask* sessionTaskConnection;
@property (nonatomic, strong) NSURLSession* session;

@end
