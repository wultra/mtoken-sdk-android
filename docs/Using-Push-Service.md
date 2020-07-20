# Using Push Service

## Introduction

Push Service is responsible for registering the device for the push notifications about the Operations that are tied to the current PowerAuth activation.

> __Note:__ Before using Push Service, you need to have a `PowerAuthSDK` object available and initialized with a valid activation. Without a valid PowerAuth activation, service will return an error

Push Service communicates with [Mobile Push Registration API](https://github.com/wultra/powerauth-webflow/blob/develop/docs/Mobile-Push-Registration-API.md).

## Creating an Instance

### Extension Factory With SSL Validation Strategy

This factory method will create its own [`OkHttpClient`](https://square.github.io/okhttp/) instance based on the chosen SSL validation strategy.

```kotlin
fun PowerAuthSDK.createPushService(appContext: Context, baseURL: String, strategy: SSLValidationStrategy): IPushService
``` 

- `appContext` - application context
- `baseURL` - address, where your operations server can be reached
- `strategy` - strategy used when validating HTTPS requests. Following strategies can be used:
    - `SSLValidationStrategy.default`
    - `SSLValidationStrategy.noValidation`
    - `SSLValidationStrategy.sslPinning`

### Extension Factory With OkHttpClient

```kotlin
fun PowerAuthSDK.createPushService(appContext: Context, baseURL: String, httpClient: OkHttpClient): IPushService
``` 
- `appContext` - application context
- `baseURL` - address, where your operations server can be reached
- `httpClient` - [`OkHttpClient`](https://square.github.io/okhttp/) instance used for API requests 

## Registering to Push Notifications

To register an app to push notifications, you can simply call the register method:

```kotlin
// first, retrieve FireBase token
FirebaseInstanceId.getInstance().instanceId.addOnCompleteListener { task ->
    if (task.isSuccessful) {
        task.result?.token?.let { token ->
            pushService.register(token, object: IPushRegisterListener {
                override fun onSuccess() {
                    // push notification registered
                }

                override fun onFailure(e: ApiError) {
                    // push notification failed
                }
            })
        }       
    } else {
        // on error
    }
}
```

## Push Message API Reference

All available methods of the `IPushService` API are:

- `acceptLanguage` - Language settings, that will be sent along with each request.
- `register(fcmToken: String, listener: IPushRegisterListener)` - Registers Firebase Cloud Messaging token on the backend
    - `fcmToken` - Firebase Cloud Messaging token.
    - `listener` - Called when the request finishes.