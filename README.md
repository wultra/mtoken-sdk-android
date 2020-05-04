# Wultra Mobile Token SDK for Android

> NOTE: This SDK is under development. More information and documentation will follow.

## Purpose

Wultra Mobile Token SDK (WMT) is a high-level SDK written in `kotlin` for strong customer authentication operation confirmation.

This SDK is an addon to `PowerAuth2 Mobile SDK`.

**OPERATIONS**  

This SDK contains an Operations service that handles operation retrieving, approving, and rejecting.

**PUSH**

This SDK contains a Push service that handles registering push notification.

## Basic usage

### Integration

Integration is now supported only via local maven.
Download latest release from releases here on github and copy it to your local maven repository.
In your gradle file, use:
```rb
implementation "com.wultra.android.mtokensdk:wultra-mtoken-sdk:1.0.0"
```

### Code usage

To create `IOperationsService` from your PowerAuth activation, simply use:

```kotlin   
val opService = powerAuthSDK.createOperationsService(appContext, "https://mydomain.com/myservice/ops", SSLValidationStrategy.default())
```

To create `IPushService` from your PowerAuth activation, simply use:

```kotlin   
val pushService = powerAuthSDK.createPushService(appContext, "https://mydomain.com/myservice/push", SSLValidationStrategy.default())
```