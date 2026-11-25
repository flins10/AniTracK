#import <Foundation/Foundation.h>
#import <UserNotifications/UserNotifications.h>
#import <AppKit/AppKit.h>

static BOOL au_has_valid_bundle(void) {
    @autoreleasepool {
        NSBundle *bundle = [NSBundle mainBundle];
        if (!bundle) return NO;
        NSString *bid = [bundle bundleIdentifier];
        if (!bid || [bid length] == 0) return NO;
        NSString *path = [bundle bundlePath];
        return path != nil && [path hasSuffix:@".app"];
    }
}

@interface AnimeMacNotifDelegate : NSObject <UNUserNotificationCenterDelegate>
@property (nonatomic, copy) NSString *pendingClickPath;
@end

@implementation AnimeMacNotifDelegate

- (void)userNotificationCenter:(UNUserNotificationCenter *)center
       willPresentNotification:(UNNotification *)notification
         withCompletionHandler:(void (^)(UNNotificationPresentationOptions options))completionHandler {
    if (@available(macOS 11.0, *)) {
        completionHandler(UNNotificationPresentationOptionBanner |
                          UNNotificationPresentationOptionList |
                          UNNotificationPresentationOptionSound |
                          UNNotificationPresentationOptionBadge);
    } else {
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wdeprecated-declarations"
        completionHandler(UNNotificationPresentationOptionAlert |
                          UNNotificationPresentationOptionSound |
                          UNNotificationPresentationOptionBadge);
#pragma clang diagnostic pop
    }
}

- (void)userNotificationCenter:(UNUserNotificationCenter *)center
didReceiveNotificationResponse:(UNNotificationResponse *)response
         withCompletionHandler:(void (^)(void))completionHandler {
    @try {
        NSDictionary *userInfo = response.notification.request.content.userInfo;
        if (self.pendingClickPath && [self.pendingClickPath length] > 0) {
            NSMutableDictionary *payload = [NSMutableDictionary dictionary];
            payload[@"notification_id"] = userInfo[@"notification_id"] ?: response.notification.request.identifier ?: @"";
            payload[@"target_screen"] = userInfo[@"target_screen"] ?: @"notifications";
            payload[@"type"] = userInfo[@"type"] ?: @"message";
            payload[@"clicked_at"] = @((long long)([[NSDate date] timeIntervalSince1970] * 1000.0));
            NSData *jsonData = [NSJSONSerialization dataWithJSONObject:payload options:0 error:nil];
            if (jsonData) {
                [jsonData writeToFile:self.pendingClickPath atomically:YES];
            }
        }
        dispatch_async(dispatch_get_main_queue(), ^{
            [[NSApplication sharedApplication] activateIgnoringOtherApps:YES];
        });
    } @catch (NSException *ex) {
        (void)ex;
    }
    completionHandler();
}

@end

static AnimeMacNotifDelegate *g_au_delegate = nil;

void au_macos_init_notification_delegate(const char *pending_click_path) {
    @autoreleasepool {
        if (!au_has_valid_bundle()) return;
        @try {
            if (!g_au_delegate) {
                g_au_delegate = [[AnimeMacNotifDelegate alloc] init];
            }
            if (pending_click_path && pending_click_path[0] != '\0') {
                g_au_delegate.pendingClickPath = [NSString stringWithUTF8String:pending_click_path];
            }
            UNUserNotificationCenter *center = [UNUserNotificationCenter currentNotificationCenter];
            center.delegate = g_au_delegate;
        } @catch (NSException *ex) {
            (void)ex;
        }
    }
}

// Returns: 0 = prompt (notDetermined), 1 = granted (authorized/provisional), 2 = denied, -1 = unavailable/unbundled
int au_macos_check_permission(void) {
    @autoreleasepool {
        if (!au_has_valid_bundle()) return -1;
        __block int result = 0;
        @try {
            dispatch_semaphore_t sem = dispatch_semaphore_create(0);
            UNUserNotificationCenter *center = [UNUserNotificationCenter currentNotificationCenter];
            [center getNotificationSettingsWithCompletionHandler:^(UNNotificationSettings *settings) {
                switch (settings.authorizationStatus) {
                    case UNAuthorizationStatusAuthorized:
                    case UNAuthorizationStatusProvisional:
                        result = 1;
                        break;
                    case UNAuthorizationStatusDenied:
                        result = 2;
                        break;
                    case UNAuthorizationStatusNotDetermined:
                    default:
                        result = 0;
                        break;
                }
                dispatch_semaphore_signal(sem);
            }];
            dispatch_time_t timeout = dispatch_time(DISPATCH_TIME_NOW, (int64_t)(3 * NSEC_PER_SEC));
            if (dispatch_semaphore_wait(sem, timeout) != 0) {
                return -1;
            }
        } @catch (NSException *ex) {
            (void)ex;
            return -1;
        }
        return result;
    }
}

// Triggers the real macOS UNUserNotificationCenter permission dialog.
// Returns: 1 = granted, 2 = denied, 0 = prompt, -1 = unavailable/unbundled
int au_macos_request_permission(void) {
    @autoreleasepool {
        if (!au_has_valid_bundle()) return -1;
        __block int result = 2;
        @try {
            UNUserNotificationCenter *center = [UNUserNotificationCenter currentNotificationCenter];
            if (g_au_delegate && center.delegate == nil) {
                center.delegate = g_au_delegate;
            }

            // If already denied, macOS will not re-prompt; return 2 immediately so UI shows Open System Settings
            int current = au_macos_check_permission();
            if (current == 1 || current == 2) {
                return current;
            }

            dispatch_semaphore_t sem = dispatch_semaphore_create(0);
            UNAuthorizationOptions opts = UNAuthorizationOptionAlert |
                                          UNAuthorizationOptionSound |
                                          UNAuthorizationOptionBadge;
            [center requestAuthorizationWithOptions:opts completionHandler:^(BOOL granted, NSError * _Nullable error) {
                (void)error;
                result = granted ? 1 : 2;
                dispatch_semaphore_signal(sem);
            }];
            dispatch_time_t timeout = dispatch_time(DISPATCH_TIME_NOW, (int64_t)(60 * NSEC_PER_SEC));
            if (dispatch_semaphore_wait(sem, timeout) != 0) {
                return au_macos_check_permission();
            }
        } @catch (NSException *ex) {
            (void)ex;
            return -1;
        }
        return result;
    }
}

// Delivers a real native notification to macOS Notification Center.
// Returns: 1 on success, 0 on failure.
int au_macos_send_notification(const char *notif_id, const char *title, const char *body) {
    @autoreleasepool {
        if (!au_has_valid_bundle()) return 0;
        if (au_macos_check_permission() != 1) return 0;
        __block int ok = 0;
        @try {
            UNUserNotificationCenter *center = [UNUserNotificationCenter currentNotificationCenter];
            if (g_au_delegate && center.delegate == nil) {
                center.delegate = g_au_delegate;
            }
            NSString *nsId = (notif_id && notif_id[0] != '\0')
                ? [NSString stringWithUTF8String:notif_id]
                : [[NSUUID UUID] UUIDString];
            NSString *nsTitle = (title && title[0] != '\0')
                ? [NSString stringWithUTF8String:title]
                : @"Anime Universe";
            NSString *nsBody = (body && body[0] != '\0')
                ? [NSString stringWithUTF8String:body]
                : @"You have a new notification.";

            UNMutableNotificationContent *content = [[UNMutableNotificationContent alloc] init];
            content.title = nsTitle;
            content.body = nsBody;
            content.sound = [UNNotificationSound defaultSound];
            content.userInfo = @{
                @"notification_id": nsId,
                @"target_screen": @"notifications"
            };

            UNNotificationRequest *request = [UNNotificationRequest requestWithIdentifier:nsId
                                                                                  content:content
                                                                                  trigger:nil];
            dispatch_semaphore_t sem = dispatch_semaphore_create(0);
            [center addNotificationRequest:request withCompletionHandler:^(NSError * _Nullable error) {
                ok = (error == nil) ? 1 : 0;
                dispatch_semaphore_signal(sem);
            }];
            dispatch_time_t timeout = dispatch_time(DISPATCH_TIME_NOW, (int64_t)(3 * NSEC_PER_SEC));
            dispatch_semaphore_wait(sem, timeout);
        } @catch (NSException *ex) {
            (void)ex;
            return 0;
        }
        return ok;
    }
}

// Opens macOS System Settings -> Notifications (macOS 13+ Ventura/Sonoma/Sequoia with macOS 12 fallback)
int au_macos_open_notification_settings(void) {
    @autoreleasepool {
        @try {
            NSURL *url = [NSURL URLWithString:@"x-apple.systempreferences:com.apple.Notifications-Settings.extension"];
            if (url && [[NSWorkspace sharedWorkspace] openURL:url]) {
                return 1;
            }
            NSURL *legacyUrl = [NSURL URLWithString:@"x-apple.systempreferences:com.apple.preference.notifications"];
            if (legacyUrl && [[NSWorkspace sharedWorkspace] openURL:legacyUrl]) {
                return 1;
            }
        } @catch (NSException *ex) {
            (void)ex;
        }
        return 0;
    }
}
