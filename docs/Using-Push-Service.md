# Using Push Service

<!-- begin remove -->
- [Introduction](#introduction)
- [Creating an Instance](#creating-an-instance)
- [Push Service API Reference](#push-service-api-reference)
- [Registering to WMT Push Notifications](#registering-to-push-notifications)
- [Receiving WMT Push Notifications](#receiving-wmt-push-notifications)
## Introduction
<!-- end -->

Push Service is responsible for registering the device for the push notifications about the Operations that are tied to the current PowerAuth activation.

<!-- begin box warning -->
Note: Before using Push Service, you need to have a `PowerAuthSDK` object available and initialized with a valid activation. Without a valid PowerAuth activation, the service will return an error.
<!-- end -->

Push Service communicates with the [Mobile Token API](https://developers.wultra.com/components/enrollment-server/develop/documentation/Mobile-Token-API).

## Creating an Instance

The preferred way of instantiating Operations Service is via `WultraMobileToken` class.
See: [Example Usage](./Example-Usage)


### Customized initialization

In case you need to create more customized instance. You can do so with an initializer.

```kotlin
val pushService = PushService(
    powerAuthSDK, 
    appContext, 
    okHttpClient, 
    baseURL, 
    tokenProvider, 
    userAgent
)
```

- `powerAuthSDK ` - PowerAuthSDK instance
- `appContext` - application context
- `httpClient ` - [`OkHttpClient`](https://square.github.io/okhttp/) with following SSLValidationStrategy
    - `SSLValidationStrategy.default`
    - `SSLValidationStrategy.noValidation`
    - `SSLValidationStrategy.sslPinning`
- `baseURL` - address, where your operations server can be reached (ending with `/enrollment-server` in the default setup)


__Optional parameters:__

For these, if null is provided, default internal implementation is provided.

- `tokenProvider` - Provider that provides a valid PowerAuth token from token store for api communication.
- `userAgent` - Optional default user agent used for each request

## Push Service API Reference

All available methods of the `IPushService` API are:

- `acceptLanguage` - Language settings, that will be sent along with each request.
- `register(data: PushData, callback: (result: Result<Unit>) -> Unit)` - Subscribes for WMT push notification on the server

## Registering to Push Notifications

<!-- begin box warning -->
If your server is running an older version than `1.10.x`, use the `register(fcmToken:callback:)` and `register(hmsToken:callback:)` deprecated methods instead to stay compatible.
<!-- end -->

To register an app to push notifications, you can simply call the `register` method:

### Firebase Cloud Messaging

```kotlin
// first, retrieve Firebase token (do so in the background thread)
FirebaseInstanceId.getInstance().instanceId.addOnCompleteListener { task ->
    if (task.isSuccessful) {
        task.result?.token?.let { token ->
            pushService.register(PushData.fcm(token)) {
                it.onSuccess {
                    // push notification registered
                }.onFailure {
                    // push notification registration failed  
                }
            }
        }       
    } else {
        // on error
    }
}
```

To be able to successfully process notifications, you need to register the app to receive push notifications in the first place. For more information visit [official documentation](https://firebase.google.com/docs/cloud-messaging/android/client).

### Huawei Messaging Service (HarmonyOS / EMUI)

For Huawei devices, you can also register your app to receive push notifications using Huawei Push Kit. To integrate Huawei Push Kit into your app, please refer to the Huawei Push Kit documentation.

```kotlin
// first, retrieve HMS token (do so in the background thread)
try {
    val appId = AGConnectOptionsBuilder().build(appContext).getString("client/app_id")
    val token = HmsInstanceId.getInstance(appContext).getToken(appId, "HCM")

    if (token.isNotEmpty()) {
        pushService.register(PushData.hms(token)) {
            it.onSuccess {
                // push notification registered
            }.onFailure {
                // push notification registration failed
            }
        }
    } else {
        // token retrieval failed
    }
} catch (e: Exception) {
    // token retrieval failed
}
```
For more information visit [official documentation](https://developer.huawei.com/consumer/en/doc/hmscore-guides/android-client-dev-0000001050042041)

## Receiving WMT Push Notifications

To process the raw notification obtained from the Firebase Cloud Messaging service (FCM) or from the HUAWEI Mobile Services (HMS), you can use the `PushParser` helper class that will parse the notification into a `PushMessage` result.

The `PushMessage` is an abstract class that is implemented by following classes for concrete results

- `PushMessageOperationCreated` - a new operation was triggered with the following properties
  -  `id` of the operation
  -  `name` of the operation
  -  `originalData` - data on which was the push message constructed
- `PushMessageOperationFinished` - an operation was finished, successfully or non-successfully with the following properties
  -  `id` of the operation
  -  `name` of the operation
  -  `result` of the operation (for example that the operation was canceled by the user).
  -  `originalData` - data on which was the push message constructed
- `PushMessageInboxReceived` - a new inbox message was triggered with the id
  -  `id` of the message
  -  `originalData` - data on which was the push message constructed
- `PushMessageActivationStatusChanged` - the activation status has changed (for example, the activation was blocked or removed).
  -  `activationId` _(optional)_ of the activation whose status changed
  -  `originalData` - data on which was the push message constructed


Example push notification processing:

```kotlin
// Overridden method of FirebaseMessagingService
override fun onMessageReceived(remoteMessage: RemoteMessage) {
    super.onMessageReceived(remoteMessage)
    val push = PushParser.parseNotification(remoteMessage.data)
    if (push != null) {
        // process the mtoken notification and react to it in the UI
    } else {
        // process all the other notification types using your own logic
    }
}
```
