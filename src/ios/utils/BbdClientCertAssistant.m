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

#import "BbdClientCertAssistant.h"
#import <BlackBerryDynamics/GD/NSURLCredential+GDNET.h>

#import <objc/runtime.h>

@interface BbdClientCertAssistant()<WKNavigationDelegate>

@property (nonatomic, weak) WKWebView *webView;

@end

@implementation BbdClientCertAssistant

-(instancetype)initWithWebView:(WKWebView *)theWebView
{
    self = [super init];
    if (self) {
        _webView = theWebView;
        _webView.navigationDelegate = self;
    }

    return self;
}

-(void)dealloc
{
    _webView.navigationDelegate = nil;
}

#pragma mark - WKWebView delegate call

-(void)webView:(WKWebView *) webView
didReceiveAuthenticationChallenge:(NSURLAuthenticationChallenge *) challenge
completionHandler:(void (^)(NSURLSessionAuthChallengeDisposition disposition, NSURLCredential *credential)) completionHandler {

    NSString *authenticationMethod = challenge.protectionSpace.authenticationMethod;

    if ([authenticationMethod isEqualToString:NSURLAuthenticationMethodClientCertificate]){
        // TODO: Extend challenge with client certificate
        completionHandler(NSURLSessionAuthChallengeUseCredential, nil);
        return;
    }
};

@end
