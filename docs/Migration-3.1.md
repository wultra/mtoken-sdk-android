# Migration from 3.0.x to 3.1.x (Android)

This guide provides instructions for migrating from **Wultra Mobile Token SDK for Android** version `3.0.x` to version `3.1.x`.

Version `3.1.x` integrates **PowerAuth Mobile SDK `2.1.x`** and **PowerAuth Networking SDK `3.0.x`**. The only source-breaking change is the removal of the `tokenProvider` parameter. For the full list of changes in the underlying libraries, see the [PowerAuth Mobile SDK migration guide from 2.0 to 2.1](https://developers.wultra.com/components/powerauth-mobile-sdk/develop/documentation/Migration-from-2.0-to-2.1) and the [Networking SDK migration guide to 3.0](https://github.com/wultra/networking-android/blob/develop/docs/Migration-3.0.md).

---

### Dependencies

Update the PowerAuth dependencies:

```kotlin
implementation("com.wultra.android.powerauth:powerauth-sdk:2.1.0")
implementation("com.wultra.android.powerauth:powerauth-networking:3.0.0")
```

PowerAuth **"server stack" `2.0+`** is still required.

---

### `tokenProvider` Parameter Removed

The `IPowerAuthTokenProvider` interface was removed from the Networking SDK. Token-authenticated requests now always obtain tokens directly from `PowerAuthSDK.tokenStore`. As a result, the `tokenProvider` parameter was removed from:

- `PowerAuthSDK.createWultraMobileToken(...)`
- `WultraMobileToken(...)`
- `OperationsService(...)`
- `InboxService(...)`
- `OIDCService(...)`
- `PushService(...)`

**Before (3.0.x):**

```kotlin
val operationsService = OperationsService(
    powerAuthSDK,
    appContext,
    httpClient,
    baseURL,
    tokenProvider,
    userAgent,
    gsonBuilder
)
```

**After (3.1.x):**

```kotlin
val operationsService = OperationsService(
    powerAuthSDK,
    appContext,
    httpClient,
    baseURL,
    userAgent,
    gsonBuilder
)
```

Remove the `tokenProvider` argument from these calls and delete any custom `IPowerAuthTokenProvider` implementation. If you passed the arguments positionally and `tokenProvider` was `null`, removing it is all you need to do. If you used named arguments, just drop `tokenProvider = ...` (or `tokeProvider = ...` in `createWultraMobileToken`).

---

### Authenticated Requests Are Serialized

Requests signed with a PowerAuth authentication code (for example `authorizeOperation`, `rejectOperation` and `getHistory`) are now dispatched one at a time, so the signature counter always reaches the server in the order it was assigned. Token-authenticated requests (for example `getOperations` or inbox requests) are still dispatched concurrently. No code changes are required.

---

### Automatic HTTP Redirects Are Disabled

Networking `3.0.x` disables automatic HTTP redirects for SDK requests. The Mobile Token services retain this default and do not expose the Networking `Api.followRedirects` opt-in. Redirect settings on a supplied `OkHttpClient` or in `okHttpInterceptor` cannot override it.

If your integration previously relied on an HTTP redirect, configure the final backend URL directly, either in `PowerAuthConfiguration.baseEndpointUrl` when using `WultraMobileToken`, or in the `baseURL` passed to each service constructor. Otherwise, redirected requests now report a failure instead of following the redirect.
