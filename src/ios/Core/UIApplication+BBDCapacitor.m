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

#import "UIApplication+BbdCapacitor.h"

#import "GDCordovaLogger.h"
#import <BlackBerryDynamics/GD/GDState.h>
#import <BlackBerryDynamics/GD/GDiOS.h>
#import "BbdCapacitorRuntime.h"

#import <objc/runtime.h>

#pragma mark BBDStateChangesHandler

@interface BBDStateChangesHandler : NSObject

@property (nonatomic, strong) UIApplication *application;
@property (nonatomic, strong) id<UIApplicationDelegate> delegate;
@property (nonatomic, strong) NSDictionary *options;

-(instancetype)initWithApplication:(UIApplication *)application andLaunchOptions:(NSDictionary *)options;

@end

@implementation BBDStateChangesHandler

-(instancetype)initWithApplication:(UIApplication *)application andLaunchOptions:(NSDictionary *)options
{
    self = [super init];
    if (self)
    {
        self.application = application;
        self.delegate = application.delegate;
        self.options = options;
    }

    return self;
}

-(void)performStateChangeNotification:(NSNotification *)notification
{
    if ([[notification name] isEqualToString:GDStateChangeNotification])
    {
        NSDictionary *userInfo = [notification userInfo];
        NSString *propertyName = [userInfo objectForKey:GDStateChangeKeyProperty];
        GDState *state = [userInfo objectForKey:GDStateChangeKeyCopy];

        if ([propertyName isEqualToString:GDKeyIsAuthorized])
        {
            [GDCordovaLogger info:@"receiveStateChangeNotification - isAuthorized: %@", state.isAuthorized ? @"true" : @"false"];
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wundeclared-selector"
            SEL didFinishLaunchingSelector = @selector(bbd_cap_stashApplication:didFinishLaunchingWithOptions:);
#pragma clang diagnostic pop
            if ([self.delegate respondsToSelector:didFinishLaunchingSelector])
            {
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Warc-performSelector-leaks"
                [self.delegate performSelector:didFinishLaunchingSelector
                                    withObject:self.application
                                    withObject:self.options];
#pragma clang diagnostic pop
                [[NSNotificationCenter defaultCenter] removeObserver:self];
                objc_removeAssociatedObjects(self);
            }
        }
        else if ([propertyName isEqualToString:GDKeyReasonNotAuthorized])
        {
            [GDCordovaLogger info:@"receiveStateChangeNotification - reasonNotAuthorized: %ld", (long) state.reasonNotAuthorized];

        }
        else if ([propertyName isEqualToString:GDKeyUserInterfaceState])
        {
            [GDCordovaLogger info:@"receiveStateChangeNotification - userInterfaceState: %ld", (long) state.userInterfaceState];

        }
        else if ([propertyName isEqualToString:GDKeyCurrentScreen])
        {
            [GDCordovaLogger info:@"receiveStateChangeNotification - currentScreen: %ld", (long) state.currentScreen];
        }
    }
}

@end
#pragma mark -

#pragma mark UIApplication+BBDCapacitor
@implementation UIApplication (BBDCapacitor)

+(void)load
{
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        [BbdCapacitorRuntime swizzleInstanceMethodForClass:self
                                 withTargetSelector:@selector(setDelegate:)
                             andReplacementSelector:@selector(setDelegateCapacitorReplacement:)
                                   andStashSelector:@selector(setDelegateCapacitorStash:)];
    });
}

-(void)setDelegateCapacitorReplacement:(id<UIApplicationDelegate>)delegate
{
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{

        Method originMethod = class_getInstanceMethod([delegate class], @selector(application:didFinishLaunchingWithOptions:));
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wundeclared-selector"
        class_addMethod([delegate class],
                        @selector(bbd_cap_application:didFinishLaunchingWithOptions:),
                        (IMP)bbd_applicationDidFinishlaunchingWithOptions, method_getTypeEncoding(originMethod));

        class_addMethod([delegate class],
                        @selector(bbd_cap_stashApplication:didFinishLaunchingWithOptions:),
                        (IMP)bbd_stashApplicationDidFinishlaunchingWithOptions, method_getTypeEncoding(originMethod));

        [BbdCapacitorRuntime swizzleInstanceMethodForClass:[delegate class]
                                 withTargetSelector:@selector(application:didFinishLaunchingWithOptions:)
                             andReplacementSelector:@selector(bbd_cap_application:didFinishLaunchingWithOptions:)
                                   andStashSelector:@selector(bbd_cap_stashApplication:didFinishLaunchingWithOptions:)];
#pragma clang diagnostic pop

    });
    [self setDelegateCapacitorStash:delegate];
}

-(void)setDelegateCapacitorStash:(id<UIApplicationDelegate>)delegate
{
    return;
}

bool bbd_stashApplicationDidFinishlaunchingWithOptions(id self, SEL cmd, UIApplication *application, NSDictionary *options)
{
    return YES;
}

bool bbd_applicationDidFinishlaunchingWithOptions(id self, SEL cmd, UIApplication *application, NSDictionary *options)
{
    BBDStateChangesHandler *notificationHandler = [[BBDStateChangesHandler alloc] initWithApplication:application
                                                                                     andLaunchOptions:options];

    [[NSNotificationCenter defaultCenter] addObserver:notificationHandler
                                             selector:@selector(performStateChangeNotification:)
                                                 name:GDStateChangeNotification
                                               object:nil];
    // keep strong reference on notificationHandler in application object
    objc_setAssociatedObject(application, "_bbd_cap_notification_receiver", notificationHandler, OBJC_ASSOCIATION_RETAIN);

    [[GDiOS sharedInstance] authorize];

    return YES;
}

@end
