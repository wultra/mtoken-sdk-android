# Wultra Mobile Token SDK for Android

<!-- begin remove -->
`Wultra Mobile Token SDK` is a high-level SDK for operation approval.
<!-- end -->
<!-- begin TOC -->
- [Introduction](#introduction)
- [Installation](#installation)
    - [Requirements](#requirements)
    - [Gradle](#gradle)
- [Usage](#usage)
    - [Operations](#operations)
    - [Push Messages](#push-messages)
    - [Error Handling](#error-handling)
- [License](#license)
- [Contact](#contact)
    - [Security Disclosure](#security-disclosure)
<!-- end -->

## Introduction
 
With Wultra Mobile Token (WMT) SDK, you can integrate an out-of-band operation approval into an existing mobile app, instead of using a standalone mobile token application. WMT is built on top of [PowerAuth Mobile SDK](https://github.com/wultra/powerauth-mobile-sdk#docucheck-keep-link). It communicates with the "Mobile Token REST API" and "Mobile Push Registration API". Individual endpoints are described in the [PowerAuth Webflow documentation](https://developers.wultra.com/docs/2019.11/powerauth-webflow/).

To understand Wultra Mobile Token SDK purpose on a business level better, you can visit our own [Mobile Token application](https://www.wultra.com/mobile-token#docucheck-keep-link). We use Wultra Mobile Token SDK in our mobile token application as well.

Wultra Mobile Token SDK library does precisely this:

- Registering an existing PowerAuth activation to receive push notifications.
- Retrieving list of operations that are pending for approval for given user.
- Approving and rejecting operations with PowerAuth transaction signing.

_Note: We also provide an [iOS version of this library](https://github.com/wultra/mtoken-sdk-ios#docucheck-keep-link)_

## Installation

### Requirements

- `minSdkVersion 16` (Android 4.1 Jelly Bean)
- [PowerAuth Mobile SDK](https://github.com/wultra/powerauth-mobile-sdk#docucheck-keep-link) needs to be available in your project.

### Gradle

To use **WMT** in you Android app, add this dependency:

```gradle
implementation "com.wultra.android.mtokensdk:wultra-mtoken-sdk:1.0.0"
```

Note that this documentation is using version `1.0.0` as an example. You can find the latest version at [github's release](https://github.com/wultra/mtoken-sdk-android/releases#docucheck-keep-link) page.

Also, make sure you have `mavenLocal()` repository among the project repositories and the version you are linking available in your local Maven repository.

## Usage

To use this library, you need to have a `PowerAuthSDK` object available and initialized with a valid activation. Without a valid PowerAuth activation, all endpoints will return an error. PowerAuth SDK implements two categories of services:

- Operations - Responsible for fetching the operation list (login request, payment, etc.), and for approving or rejecting operations.
- Push Messages - Responsible for registering the device for the push notifications.

### Operations

This part of the SDK communicates with [Mobile Token API endpoints](https://github.com/wultra/powerauth-webflow/blob/develop/docs/Mobile-Token-API.md).

#### Factory Extension With SSL Validation Strategy

Convenience factory method that will return a new instance. A new `OkHttpClient` will be created based on chosen `SSLValidationStrategy` in the last parameter.

```kotlin
fun PowerAuthSDK.createOperationsService(appContext: Context, baseURL: String, strategy: SSLValidationStrategy): IOperationsService
``` 

- `appContext` - application context
- `baseURL` - address, where your operations server can be reached
- `strategy` - strategy used when validating HTTPS requests. Following strategies can be used:
    - `SSLValidationStrategy.default`
    - `SSLValidationStrategy.noValidation`
    - `SSLValidationStrategy.sslPinning`

#### Factory Extension With OkHttpClient

Convenience factory method that will return a new instance with provided `OkHttpClient` that you can configure on your own.

```kotlin
fun PowerAuthSDK.createOperationsService(appContext: Context, baseURL: String, httpClient: OkHttpClient): IOperationsService
``` 

- `appContext` - application context
- `baseURL`-  address, where your operations server can be reached
- `httpClient` - `OkHttpClient` instance used for API requests 

#### Retrieve the Pending Operations

To fetch the list with pending operations, implement the `IOperationsService` API, you can call:

```kotlin
// TBD
```

After you retrieve the pending operation, you can render them in the UI, for example, as a list of pending operations with a detail of operation on tap:

```kotlin
// TBD
```

#### Start Periodic Polling

Mobile token API is highly asynchronous - to simplify the work for you, we added a convenience operation list polling feature:

```kotlin
// TBD
```

#### Approve or Reject Operation

Approve or reject a given operation, simply hook these actions to the approve or reject buttons:

```kotlin
// TBD
```

#### Off-line Authorization

In case the user is not online, you can use off-line authorizations. In this operation mode, the user needs to scan a QR code, enter PIN code or use biometry, and rewrite the resulting code. Wultra provides a special format for [the operation QR codes](https://github.com/wultra/powerauth-webflow/blob/develop/docs/Off-line-Signatures-QR-Code.md), that is automatically processed with the SDK.

To process the operation QR code, simply call:

```kotlin
```

After that, you can produce an off-line signature using the following code:

```kotlin
```

#### Operations API Reference

All available methods and attributes of `IOperationsService` API are:

- `listener` - Listener object that receives info about operation loading.
- `acceptLanguage` - Language settings, that will be sent along with each request.
- `getLastOperationsResult()` - Cached last operations result.
- `isLoadingOperations()` - Indicates if the service is loading operations.
- `getOperations(listener: IGetOperationListener?)` - Retrieves pending operations from the server.
    - `listener` - Called when operation finishes.
- `isPollingOperations()` - If the app is periodically polling for the operations from the server.
- `startPollingOperations(pollingInterval: Long)` - Starts periodic operation polling.
    - `pollingInterval` - How often should operations be refreshed.
- `stopPollingOperations()` - Stops periodic operation polling.
- `authorizeOperation(operation: Operation, authentication: PowerAuthAuthentication, listener: IAcceptOperationListener)` - Authorize provided operation.
    - `operation` - Operation to approve, retrieved from `getOperations` call.
    - `authentication` - PowerAuth authentication object for operation signing.
    - `listener` - Called when authorization request finishes.
- `rejectOperation(operation: Operation, reason: RejectionReason, listener: IRejectOperationListener)` - Reject provided operation.
    - `operation` - Operation to reject, retrieved from `getOperations` call.
    - `reason` - Rejection reason.
    - `listener` - Called when rejection request finishes.
- `processOfflineQrPayload(payload: String)` - Parses offline (QR) operation string into a structure.
    - `payload` - Parse data from QR code scanned with the app.
- `signOfflineOperationWithPassword(password: String, offlineOperation: QROperation)` - Sign offline (QR) operation with password
    - `password` - Password for PowerAuth activation.
    - `offlineOperation` - Offline operation retrieved via `processOfflineQrPayload` method.
- `signOfflineOperationWithBiometry(biometry: ByteArray, offlineOperation: QROperation)` - Sign offline (QR) operation with biometry data.
    - `biometry` - Biometry data retrieved from `powerAuthSDK.authenticateUsingBiometry` call.
    - `offlineOperation` - Offline operation retrieved via `processOfflineQrPayload` method.

For more details on the API, visit [`IOperationsService` code documentation](https://github.com/wultra/mtoken-sdk-android/blob/master/library/src/main/java/com/wultra/android/mtokensdk/operation/IOperationsService.kt).

### Push Messages

This part of the SDK communicates with [Mobile Push Registration API](https://github.com/wultra/powerauth-webflow/blob/develop/docs/Mobile-Push-Registration-API.md).

To register PowerAuth enabled application to receive push notifications, use one of the following convenience extension methods (Kotlin):

#### Extension Factory With SSL Validation Strategy

This factory method will create its own `OkHttpClient` instance based on the chosen SSL validation strategy.

```kotlin
fun PowerAuthSDK.createPushService(appContext: Context, baseURL: String, strategy: SSLValidationStrategy): IPushService
``` 

- `appContext` - application context
- `baseURL` - address, where your operations server can be reached
- `strategy` - strategy used when validating HTTPS requests. Following strategies can be used:
    - `SSLValidationStrategy.default`
    - `SSLValidationStrategy.noValidation`
    - `SSLValidationStrategy.sslPinning`

#### Extension Factory With OkHttpClient

```kotlin
fun PowerAuthSDK.createPushService(appContext: Context, baseURL: String, httpClient: OkHttpClient): IPushService
``` 
- `appContext` - application context
- `baseURL` - address, where your operations server can be reached
- `httpClient` - `OkHttpClient` instance used for API requests 

#### Registering to Push Notifications

To register an app to push notifications, you can simply call the register method:

```kotlin
```

#### Push Message API Reference

All available methods of the `IPushService` API are:

- `acceptLanguage` - Language settings, that will be sent along with each request.
- `register(fcmToken: String, listener: IPushRegisterListener)` - Registers Firebase Cloud Messaging token on the backend
    - `fcmToken` - Firebase Cloud Messaging token.
    - `listener` - Called request finishes

For more details on the API, visit [`IPushService` code documentation](https://github.com/wultra/mtoken-sdk-android/blob/master/library/src/main/java/com/wultra/android/mtokensdk/push/IPushService.kt).

### Error Handling

All methods that communicate with server APIs return an [`ApiError`](https://github.com/wultra/mtoken-sdk-android/blob/master/library/src/main/java/com/wultra/android/mtokensdk/api/general/ApiError.kt) instance in case of an error.
Every API error contains an original exception that was thrown, and a convenience error property for known API error states (for example, if the operation is already canceled during approval).

## License

All sources are licensed using the Apache 2.0 license. You can use them with no restrictions. If you are using this library, please let us know. We will be happy to share and promote your project.

## Contact

If you need any assistance, do not hesitate to drop us a line at [hello@wultra.com](mailto:hello@wultra.com) or our official [gitter.im/wultra](https://gitter.im/wultra) channel.

### Security Disclosure

If you believe you have identified a security vulnerability with Wultra Mobile Token SDK, you should report it as soon as possible via email to [support@wultra.com](mailto:support@wultra.com). Please do not post it to the public issue tracker.
