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

#import "GDCBasePlugin.h"
#import <objc/runtime.h>
#import "GDCordovaLogger.h"

#ifdef CORDOVA_BUILD_DEBUG
#import <Cordova/CDVCommandQueue.h>
#import <Cordova/CDVInvokedUrlCommand.h>

@interface CDVCommandQueue (CordovaDebug)
+(void)patch;
@end

#endif // CORDOVA_BUILD_DEBUG

@implementation GDCBasePlugin

NSString* const kMailToScheme = @"mailto";
NSString* const kMailToPlugin = @"GDMailToPlugin";

static NSMutableDictionary *pluginRegistry = nil;

+(void)load
{
    // replace openURL and openURL:options:completionHandler with openURLReplacement:options:completionHandler:
    // the last is used as an antry point to handle mailto: scheme
    [self replaceOpenUrlMethod];

#ifdef CORDOVA_BUILD_DEBUG
    [CDVCommandQueue patch];
#endif // CORDOVA_BUILD_DEBUG
}

+(void) registerPlugin:(NSObject *)plugin
{
    if(!pluginRegistry)
    {
        pluginRegistry = [NSMutableDictionary new];
    }

    [pluginRegistry setObject:plugin forKey:(id)[plugin class]];
}

+(void) swizzleInstanceMethodForClass:(Class)originalClass // original
                  andReplacementClass:(Class)replacementClass // new
                   withTargetSelector:(SEL)originalSelector // original
               andReplacementSelector:(SEL)replacementSelector // new
                     andStashSelector:(SEL)stashSelector // store the original so we can access it
{
    Method originalMethod = class_getInstanceMethod(originalClass, originalSelector); // original
    Method replacementMethod = class_getInstanceMethod(replacementClass, replacementSelector); // new

    method_exchangeImplementations(originalMethod, replacementMethod);

    if (stashSelector) {
        Method stashMethod = class_getInstanceMethod(replacementClass, stashSelector);

        // Here we operate on the 'replacementMethod' as that is now the 'original' (due to the
        // method swap we did above).
        method_exchangeImplementations(replacementMethod, stashMethod);
    }
}

-(void) writePluginResultForId:(NSString*)acallbackID asDictionary:(NSDictionary*)dictionary isError:(BOOL)isError asKeepAlive:(BOOL)keepAlive
{
    NSError *error = nil;
    NSData *jsonData = [NSJSONSerialization dataWithJSONObject:dictionary options:NSJSONWritingPrettyPrinted error:&error];
    NSString *jsonString = [[NSString alloc] initWithData:jsonData encoding:NSUTF8StringEncoding];
#ifdef CORDOVA_BUILD_DEBUG
    [self logPluginResult:jsonString withCallbackId:acallbackID];
#endif // CORDOVA_BUILD_DEBUG
    [self writePluginResultForId:acallbackID asString:jsonString isError:isError asKeepAlive:keepAlive];
}

-(void) writePluginResultForId:(NSString*)acallbackID asDictionary:(NSDictionary*)dictionary isError:(BOOL)isError
{
#ifdef CORDOVA_BUILD_DEBUG
    [self logPluginResult:[dictionary debugDescription] withCallbackId:acallbackID];
#endif // CORDOVA_BUILD_DEBUG
    [self writePluginResultForId:acallbackID asDictionary:dictionary isError:isError asKeepAlive:NO];
}

-(void) writePluginResultForId:(NSString*)acallbackID asString:(NSString*)resultString isError:(BOOL)isError asKeepAlive:(BOOL)keepAlive
{
	CDVPluginResult *successResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:resultString];
    CDVPluginResult *errorResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:resultString];
#ifdef CORDOVA_BUILD_DEBUG
    [self logPluginResult:resultString withCallbackId:acallbackID];
#endif // CORDOVA_BUILD_DEBUG
	if(!isError) {
        if(keepAlive)
            [successResult setKeepCallbackAsBool:YES];

        [self.commandDelegate sendPluginResult:successResult callbackId:acallbackID];
	} else {
        if(keepAlive)
            [errorResult setKeepCallbackAsBool:YES];

		[self.commandDelegate sendPluginResult:errorResult callbackId:acallbackID];
	}
}

-(void) writePluginResultForId:(NSString*)acallbackID asString:(NSString*)resultString isError:(BOOL)isError
{
#ifdef CORDOVA_BUILD_DEBUG
    [self logPluginResult:resultString withCallbackId:acallbackID];
#endif // CORDOVA_BUILD_DEBUG
	[self writePluginResultForId:acallbackID asString:resultString isError:isError asKeepAlive:NO];
}

-(void) writePluginResultForId:(NSString*)acallbackID asString:(NSString*)resultString
{
#ifdef CORDOVA_BUILD_DEBUG
    [self logPluginResult:resultString withCallbackId:acallbackID];
#endif // CORDOVA_BUILD_DEBUG
	[self writePluginResultForId:acallbackID asString:resultString isError:NO];
}

#ifdef CORDOVA_BUILD_DEBUG
-(void)logPluginResult:(NSString *)pluginResult withCallbackId:(NSString *)callbackId
{
    [GDCordovaLogger info:@"IIICCRRR: CallbackId: %@", callbackId];
    [GDCordovaLogger info:@"IIICCRRR: Result: %@", pluginResult];
}
#endif // CORDOVA_BUILD_DEBUG

/**
 * getInputArgAsChar - helper function to retrieve input argument as a const char*.
 */
-(const char*) getInputArgAsChar:(id)arg
{
	if(arg != [NSNull null])
		return [(NSString*)arg UTF8String];
	else
		return nil;
}

/**
 * getInputArgAsNSString - helper function to retrieve input argument as an NSString*.
 */
-(NSString*) getInputArgAsNSString:(id)arg
{
	if(arg != [NSNull null])
		return [NSString stringWithString:((NSString*)arg)];
	else
		return nil;
}

/**
 * getInputArgAsBOOL - helper function to retrieve input argument as a BOOL.
 */
-(BOOL) getInputArgAsBOOL:(id)arg withDefault:(BOOL)defaultValue
{
	if(arg != [NSNull null]) {
        if ( [arg isKindOfClass:[NSString class]])
            return ([[NSString stringWithString:((NSString*)arg)] isEqualToString:@"true"]) ? YES : NO;
        else if ( [arg isKindOfClass:[NSNumber class]])
            return ([(NSNumber*)arg integerValue] > 0 );
        else
            return false;
    }
	else
		return defaultValue;
}

+(void) replaceOpenUrlMethod
{
    static dispatch_once_t once;
    dispatch_once(&once, ^{

#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wundeclared-selector"

        [self swizzleInstanceMethodForClass:[UIApplication class]
                        andReplacementClass:[UIApplication class]
                         withTargetSelector:@selector(openURL:)
                     andReplacementSelector:@selector(gdcOpenURLReplacement:)
                           andStashSelector:@selector(defaultOpenURL:)];

        [self swizzleInstanceMethodForClass:[UIApplication class]
                        andReplacementClass:[UIApplication class]
                         withTargetSelector:@selector(openURL:options:completionHandler:)
                     andReplacementSelector:@selector(gdcOpenURLReplacement:options:completionHandler:)
                           andStashSelector:@selector(defaultOpenURL:options:completionHandler:)];
#pragma clang diagnostic pop
    });
}

@end

#pragma mark - UIApplication(GDCordovaMailToProxy)

@interface UIApplication(GDCordovaMailToProxy)

-(void) gdcOpenURLReplacement:(NSURL*)url options:(NSDictionary<NSString *, id> *)options completionHandler:(void (^ __nullable)(BOOL success))completion;
-(void) gdcOpenURLReplacement:(NSURL*)url;
-(void) defaultOpenURL:(NSURL *)url;
-(void) defaultOpenURL:(NSURL*)url options:(NSDictionary<NSString *, id> *)options completionHandler:(void (^ __nullable)(BOOL success))completion;

@end

@implementation UIApplication(GDCordovaMailToProxy)


/**
 * @method tryUseMailToPluginWithURL
 * Tries to perform mailto url by MailTo plugin
 * @param mailto url
 * @return YES in a case of url performed by MailTo plugin. NO if there is no MailTo plugin installed
 */
-(BOOL) tryUseMailToPluginWithURL:(NSURL*)url
{
    Class MailToPlugin = NSClassFromString(kMailToPlugin);
    NSObject *mailToPluginObject = [pluginRegistry objectForKey:MailToPlugin];
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wundeclared-selector"
    // check for the MailTo plugin
    if(mailToPluginObject &&
       [mailToPluginObject isKindOfClass:MailToPlugin] &&
       [mailToPluginObject respondsToSelector:@selector(presentMailComposerFromURL:)])
    {
        [GDCordovaLogger detail:@"Perform mailto using BBDMailToPlugin"];
        // handle the mailto: scheme by the plugin.
        [mailToPluginObject performSelector:@selector(presentMailComposerFromURL:) withObject:url];

        return YES;
    }
#pragma clang diagnostic pop
    return NO;
}

-(void) defaultOpenURL:(NSURL *)url
{
    // stash selector, never called
}
-(void) defaultOpenURL:(NSURL*)url options:(NSDictionary<NSString *, id> *)options completionHandler:(void (^ __nullable)(BOOL success))completion
{
    // stash selector, never called
}

-(void) gdcOpenURLReplacement:(NSURL*)url
{
    BOOL isHandledByMailToPlugin = NO;
    if ([[url scheme] isEqualToString:kMailToScheme])
    {
        isHandledByMailToPlugin = [self tryUseMailToPluginWithURL:url];
    }

    if (!isHandledByMailToPlugin)
    {
        // handle mailto: scheme by openURL method
        // this approach is used if there is no GDMailToPlugin installed
        [self defaultOpenURL:url];
    }
}

-(void) gdcOpenURLReplacement:(NSURL*)url options:(NSDictionary<NSString *, id> *)options completionHandler:(void (^ __nullable)(BOOL success))completion
{
    if ([[url scheme] isEqualToString:kMailToScheme])
    {
        BOOL isHandledByMailToPlugin = [self tryUseMailToPluginWithURL:url];

        if(!isHandledByMailToPlugin)
        {
            [GDCordovaLogger detail:@"Handle mailto: scheme with openURL"];
            // handle mailto: scheme by openURL method
            // this approach is used if there is no GDMailToPlugin installed
            [self defaultOpenURL:url];
        }
    }
    else
    {
        // call origin openURL:options:completionHandler method
        // this method is used in BBDOpenUrlChannel so we need to call the original method for handling the ICC services
        [self defaultOpenURL:url options:options completionHandler:completion];
    }
}

@end

#ifdef CORDOVA_BUILD_DEBUG
@implementation CDVCommandQueue (CordovaDebugPrivate)

-(BOOL)executeReplacement:(CDVInvokedUrlCommand *)command
{
    @try
    {
        [GDCordovaLogger info:@"IIICCMMM: Executing command:"];
        [GDCordovaLogger info:@"IIICCMMM: Callback ID: %@", command.callbackId];
        [GDCordovaLogger info:@"IIICCMMM: Method: [%@ %@:]", command.className, command.methodName];
        if (command.arguments.count > 0)
        {
            for (int i = 0; i < command.arguments.count; i++) {
                [GDCordovaLogger info:@"IIICCMMM: Argument at index: %d: %@", i, (NSString *)[command.arguments objectAtIndex:i]];
            }
        }
    }
    @catch(NSException *exception)
    {
        [GDCordovaLogger info:@"Incorrect data? never mind."];
    }
    @finally
    {
        return [self executeReplacement:command];
    }
}

+(void)patch
{
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        Method origin = class_getInstanceMethod(self, @selector(execute:));
        Method replacement = class_getInstanceMethod(self, @selector(executeReplacement:));

        method_exchangeImplementations(origin, replacement);
    });
}

@end
#endif // CORDOVA_BUILD_DEBUG
