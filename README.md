# Wultra Mobile Token SDK for Android

> NOTE: This SDK is under development. More information and documentation will follow.

## Purpose

Wultra Mobile Token SDK (WMT) is a high-level SDK written in `kotlin` for strong customer authentication operation confirmation.

This SDK is an addon to `PowerAuth2 Mobile SDK`.

**OPERATIONS**  

This SDK contains an Operations service that handles operation retrieving, approving, and rejecting.

**PUSH**

> This is not available yet

## Basic usage

### Integration

Integration is now supported only via local maven.

Download latest release from releases here on github and copy it to your local maven repository.

### Code usage

To retrieve `IOperationsService` service from your PowerAuth activation, simply use:

```kotlin   
val opService = powerAuthSDK.createOperationsService(appContext, okHttpClient, "http://mydomain.com/myservice/ops")
```