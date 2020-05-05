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
    - [Push](#push)
    - [Error handling](#error-handling)
- [License](#license)
- [Contact](#contact)
    - [Security Disclosure](#security-disclosure)
<!-- end -->

## Introduction
 
With `Wultra Mobile Token (WMT) SDK`, you will make access to your digital channels easier for your customers with a highly secure and user-friendly means of authentication and authorizing operations.

WMT is built on top of [PowerAuth Mobile SDK](https://github.com/wultra/powerauth-mobile-sdk#docucheck-keep-link) and is communication with `Mobile Token REST API` and `Mobile Push Registration API` endpoints described in [PowerAuth Webflow documentation](https://developers.wultra.com/docs/2019.11/powerauth-webflow/) 

To understand `WMT SDK` application-level purpose, you can visit our own [Mobile Token application](https://www.wultra.com/mobile-token#docucheck-keep-link) that is integrating this SDK.

`Wultra Mobile Token SDK` library does precisely this:
- Registering powerauth activation to receive push notifications
- Retrieving list of operations that are pending for approval
- Approving and rejecting operations with PowerAuth authentications

> We also provide an [iOS version of this library](https://github.com/wultra/mtoken-sdk-ios#docucheck-keep-link)

## Installation

### Requirements

- minSdkVersion 16 (Android 4.1 Jelly Bean)
- [PowerAuth Mobile SDK](https://github.com/wultra/powerauth-mobile-sdk#docucheck-keep-link) needs to be available in your project 

### Gradle

To use **WMT** in you Android app add this dependency:

```gradle
implementation "com.wultra.android.mtokensdk:wultra-mtoken-sdk:1.0.0"
```

Note that this documentation is using version `1.0.0` as an example. You can find the latest version at [github's release](https://github.com/wultra/mtoken-sdk-android/releases#docucheck-keep-link) page.

Also, make sure you have `mavenLocal()` repository among the project repositories and the version, you're linking available in your local maven repository.

## Usage

To use this library, you need to have `PowerAuthSDK` object available and initialized with valid activation. 
If not, all endpoints will return an error.

### Operations

This part of the SDK communicates with [Mobile Token API endpoints](https://github.com/wultra/powerauth-webflow/blob/develop/docs/Mobile-Token-API.md).

**Factory extension with SSL Validation Strategy**

Convenience factory method that will return a new instance. A new `OkHttpClient` will be created based on
chosen `SSLValidationStrategy` in the last parameter.

```kotlin
fun PowerAuthSDK.createOperationsService(appContext: Context, baseURL: String, strategy: SSLValidationStrategy): IOperationsService
``` 
- `appContext` - application context
- `baseURL` - address, where your operations server can be reached
- `strategy` - strategy used when validating HTTPS requests. Following strategies can be used:
    - `SSLValidationStrategy.default`
    - `SSLValidationStrategy.noValidation`
    - `SSLValidationStrategy.sslPinning`

**Factory extension with OkHttpClient**

Convenience factory method that will return a new instance with provided `OkHttpClient` that you can configure
at your own will.

```kotlin
fun PowerAuthSDK.createOperationsService(appContext: Context, baseURL: String, httpClient: OkHttpClient): IOperationsService
``` 
- `appContext` - application context
- `baseURL`-  address, where your operations server can be reached
- `httpClient` - `OkHttpClient` instance used for API requests 

#### IOperationsService API

- `listener` - Listener object that receives info about operation loading
- `acceptLanguage` - Language settings, that will be sent along with each request.
- `getLastOperationsResult()` - Cached last operations result
- `isLoadingOperations()` - If the service is loading operations
- `getOperations(listener: IGetOperationListener?)` - Retrieves operations from the server
    - `listener` - Called when operation finishes
- `isPollingOperations()` - If the operations are periodically polling from the server
- `startPollingOperations(pollingInterval: Long)` - Starts periodic operation polling
    - `pollingInterval` - How often should operations be refreshed 
- `stopPollingOperations()` - Stops periodic operation polling
- `authorizeOperation(operation: Operation, authentication: PowerAuthAuthentication, listener: IAcceptOperationListener)` - Authorize operation on the backend
    - `operation` - Operation to approve, retrieved from `getOperations` call
    - `authentication` - PowerAuth authentication object for operation signing
    - `listener` - Called when authorization request finishes
- `rejectOperation(operation: Operation, reason: RejectionReason, listener: IRejectOperationListener)` - Reject operation on the backend
    - `operation` - Operation to reject, retrieved from `getOperations` call
    - `reason` - Rejection reason
    - `listener` - Called when rejection request finishes
- `signOfflineOperationWithBiometry(biometry: ByteArray, offlineOperation: QROperation)` - Sign offline (QR) operation with biometry data
    - `biometry` - Biometry data retrieved from `powerAuthSDK.authenticateUsingBiometry` call
    - `offlineOperation` - Offline operation retrieved via `processOfflineQrPayload` method
- `signOfflineOperationWithPassword(password: String, offlineOperation: QROperation)` - Sign offline (QR) operation with password
    - `password` - Password for PowerAuth activation
    - `offlineOperation` - Offline operation retrieved via `processOfflineQrPayload` method
- `processOfflineQrPayload(payload: String)` - Parses offline (QR) operation string into a structure
    - `payload` - Parse data from QR code scanned with the app

For more details on the API, visit [`IOperationsService` code documentation](https://github.com/wultra/mtoken-sdk-android/blob/master/library/src/main/java/com/wultra/android/mtokensdk/operation/IOperationsService.kt).

### Push

This part of the SDK communicates with [Mobile Push Registration API](https://github.com/wultra/powerauth-webflow/blob/develop/docs/Mobile-Push-Registration-API.md).

#### To register PowerAuth enabled application to receive push notifications, use one of the following convenience extension methods (kotlin):

**Extension factory with SSL Validation Strategy**

This factory method will create its own `OkHttpClient` instance based on the chosen ssl validation strategy.
```kotlin
fun PowerAuthSDK.createPushService(appContext: Context, baseURL: String, strategy: SSLValidationStrategy): IPushService
``` 
- `appContext` - application context
- `baseURL` - address, where your operations server can be reached
- `strategy` - strategy used when validating HTTPS requests. Following strategies can be used:
    - `SSLValidationStrategy.default`
    - `SSLValidationStrategy.noValidation`
    - `SSLValidationStrategy.sslPinning`

**Extension factory with OkHttpClient**
```kotlin
fun PowerAuthSDK.createPushService(appContext: Context, baseURL: String, httpClient: OkHttpClient): IPushService
``` 
- `appContext` - application context
- `baseURL` - address, where your operations server can be reached
- `httpClient` - `OkHttpClient` instance used for API requests 

#### IPushService API

- `acceptLanguage` - Language settings, that will be sent along with each request.
- `register(fcmToken: String, listener: IPushRegisterListener)` - Registers Firebase Cloud Messaging token on the backend
    - `fcmToken` - Firebase Cloud Messaging token
    - `listener` - Called request finishes

For more details on the API, visit [`IPushService` code documentation](https://github.com/wultra/mtoken-sdk-android/blob/master/library/src/main/java/com/wultra/android/mtokensdk/push/IPushService.kt).

### Error handling

All methods, that are communicating with server APIs will return an [`ApiError`](https://github.com/wultra/mtoken-sdk-android/blob/master/library/src/main/java/com/wultra/android/mtokensdk/api/general/ApiError.kt) instance.
Every API error contains an original exception, that was thrown and convenience error property if a known API error happened (for example when the operation is already canceled during approval).

## License

All sources are licensed using the Apache 2.0 license. You can use them with no restrictions. 
If you are using this library, please let us know. We will be happy to share and promote your project.

## Contact

If you need any assistance, do not hesitate to drop us a line at [hello@wultra.com](mailto:hello@wultra.com) 
or our official [gitter.im/wultra](https://gitter.im/wultra) channel.

### Security Disclosure

If you believe you have identified a security vulnerability with WultraSSLPinning, 
you should report it as soon as possible via email to [support@wultra.com](mailto:support@wultra.com). Please do not post it to a public issue tracker.