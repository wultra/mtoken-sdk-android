# OIDC and PowerAuth Integration

- [Introduction](#introduction)
- [Creating an Instance](#creating-an-instance)
- [Retrieving Configuration](#retrieving-configuration)
- [Preparing OIDC Authorization Data](#preparing-oidc-authorization-data)
- [Open authorize URL in a web browser](#open-authorize-url-in-a-web-browser)
- [Processing a Deeplink and initializing PowerAuth activation flow](#processing-a-deeplink-and-initializing-powerAuth-activation-flow)
- [OIDCUtils](#oidcutils)

## Introduction

The OIDC and PowerAuth integration enables secure user authentication and the preparation of necessary attributes to initiate a PowerAuth activation. This integration provides tools for managing OpenID Connect (OIDC) flows, including preparing for OIDC activation, processing deeplinks, and handling PKCE codes and authorization URIs.

OIDC is commonly used for scenarios like secure user login, authorization to access resources, or linking third-party accounts.

<!-- begin box warning -->
Note: Before using the OIDC and PowerAuth integration, you need to have a `PowerAuthSDK` object available.
<!-- end -->

The integration communicates with the [OpenID Connect Standard](https://openid.net/connect/) and enhances the process with secure PKCE (Proof Key for Code Exchange) and state validation to ensure the integrity of the OIDC flow.

---

## Creating an Instance

The preferred way of instantiating Operations Service is via `WultraMobileToken` class.
See: [Example Usage](./Example-Usage)

### Customized initialization

If you need to create a more customized instance, you can do so as follows.

```kotlin
val oidcService = OIDCService(
    powerAuthSDK,
    appContext,
    okHttpClient,
    baseURL,
    tokenProvider,
    userAgent,
    gsonBuilder
)
```

- `powerAuthSDK ` - PowerAuthSDK instance
- `appContext` - application context
- `httpClient ` - [`OkHttpClient`](https://square.github.io/okhttp/) with following SSLValidationStrategy
  - `SSLValidationStrategy.default`
  - `SSLValidationStrategy.noValidation`
  - `SSLValidationStrategy.sslPinning`
- `baseURL` - address, where your OIDC server can be reached (ending with `/enrollment-server` in the default setup)


__Optional parameters:__

For these, if null is provided, default internal implementation is provided.

- `tokenProvider` - Provider that provides a valid PowerAuth token from token store for api communication.
- `userAgent` - Optional default user agent used for each request
- `gsonBuilder` - Optional GSON builder for custom deserialization


## Retrieving Configuration

The `getConfig` method retrieves the OIDC provider configuration based on a predefined `providerId`, returning a `OIDCConfig` object with essential details about the provider, client, and PKCE settings.

### OIDCConfig

The `OIDCConfig` structure contains essential OIDC configuration values for authentication.

| Property        | Type    | Description                                                                                     |
|-----------------|---------|------------------------------------------------------------------------------|
| `providerId`    | `String`| The unique identifier for the OIDC provider.                                                   |
| `clientId`      | `String`| The OAuth 2.0 client ID used to form the URL for the authorization request.                     |
| `scopes`        | `String`| A space-delimited list of OAuth 2.0 scopes for the authorization request.                       |
| `authorizeUri`  | `String`| The OAuth 2.0 authorization URI where the user is redirected for authentication.                |
| `redirectUri`   | `String`| The OAuth 2.0 redirect URI where the server sends responses after authentication.               |
| `pkceEnabled`   | `Bool`  | Indicates whether PKCE (Proof Key for Code Exchange) should be used in the authentication flow. |

##### Example:

```kotlin
oidcService.getConfig("example_provider") { result ->
    result.onSuccess {
        // OIDC configuration
    }.onFailure {
        // show error
    }
}
```

## Preparing OIDC Authorization Request Data

The `prepareAuthorizationData` method generates the necessary data for initiating the OIDC authorization process from `OIDCConfig`. `OIDCConfig` can be obtained by calling `getConfig(providerId)` or instantiated directly.


##### OIDCAuthorizationRequest

Encapsulates the data required to initiate the OIDC authorization flow and also other properties for PowerAuth Activation flow.

| Property         | Type      | Description                                             |
|------------------|-----------|---------------------------------------------------------|
| `authorizeUri`   | `Uri`     | URI to redirect the user for OIDC authentication.       |
| `providerId`     | `String`  | Identifier for the OIDC provider configuration.         |
| `nonce`          | `String`  | Random value to prevent replay attacks.                |
| `state`          | `String`  | Random value to maintain state between request/callback.|
| `codeVerifier`   | `String?` | PKCE code verifier, if applicable.                      |

##### Example:

```kotlin
oidcService.prepareAuthorizationData("example_provider") { result ->
  result.onSuccess { oidcAuthRequest ->
    // Use oidcAuthRequest.authorizeUri to open the browser
  }.onFailure { error ->
    // handle error
  }
}
```

## Open authorize URI in a web browser

To start the OIDC flow, you must open the authorization URI in a web browser. The recommended approach is to use Custom Tabs for a seamless and secure user experience. Custom Tabs provide a lightweight browsing interface with the added benefits of session sharing and faster loading times compared to a WebView.

Since the Wultra Mobile Token SDK does not include any UI logic, it is up to you to implement this functionality. Below is an example of how you can handle this:

### Example

```kotlin
fun openWebBrowser(authorizationUri: Uri) {
  if (isCustomTabsSupported()) {
    // Launch Custom Tabs if supported
    CustomTabsIntent.Builder()
      .setShareState(CustomTabsIntent.SHARE_STATE_OFF)
      .build()
      .launchUrl(context, authorizationUri)
  } else {
    // Fallback to the default browser
    val browserIntent = Intent(Intent.ACTION_VIEW, authorizationUri)
    context.startActivity(browserIntent)
  }
}

fun isCustomTabsSupported(): Boolean {
  val activityIntent = Intent(Intent.ACTION_VIEW, Uri.parse("http://www.example.com"))
  val packageManager = context.packageManager

  val viewIntentHandlers = packageManager.queryIntentActivities(activityIntent, 0)
  val packageNames = viewIntentHandlers.map { it.activityInfo.packageName }

  // Check if any browser supports Custom Tabs
  return CustomTabsClient.getPackageName(context, packageNames, false) != null
}
```

## Processing a Deeplink and initializing PowerAuth activation flow

After the user completes the OIDC flow in the web browser, the returned deeplink can be processed to extract the necessary attributes.

### Registering Deeplink Handler

Before your application can receive and process OIDC callback deeplinks, you must register the appropriate intent filter in your `AndroidManifest.xml`. The deeplink scheme and host should match the `redirectUri` configured in your OIDC provider settings.

#### AndroidManifest.xml Configuration

Add an intent filter to the activity that should handle the OIDC callback:

```xml
<activity android:name=".YourCallbackActivity">
    <intent-filter android:autoVerify="true">
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data android:scheme="yourapp" android:host="oauth" />
    </intent-filter>
</activity>
```

<!-- begin box info -->
**Note:** Replace `yourapp://oauth` with the actual scheme and host from your OIDC configuration's `redirectUri`. For example, if your `redirectUri` is `com.example.myapp://callback`, use `android:scheme="com.example.myapp"` and `android:host="callback"`.
<!-- end -->

#### Jetpack Compose Navigation

If you're using Jetpack Compose with Navigation Component, you can register the deeplink in your navigation graph:

```kotlin
composable(
    route = "callback",
    deepLinks = listOf(
        navDeepLink {
            uriPattern = "yourapp://oauth"
        }
    )
) {
    // Handle the OIDC callback here
    CallbackScreen()
}
```

### Processing a deeplink 

The `OIDCUtils.processDeeplink` utility function extracts and validates the data needed to initiate PowerAuth activation from the OIDC flow's callback URI.


##### PowerAuthActivationAttributes

Represents the attributes required to initiate a PowerAuth activation after completing an OIDC flow.

| Property       | Type      | Description                                        |
|----------------|-----------|----------------------------------------------------|
| `providerId`   | `String`  | Identifier for the OIDC provider configuration.    |
| `code`         | `String`  | Authorization code received from the OIDC flow.    |
| `nonce`        | `String`  | Random value for ensuring integrity of the flow.   |
| `codeVerifier` | `String?` | PKCE code verifier, if applicable.                 |


### Initiating PowerAuth Activation with OIDC

The final step in the OIDC and PowerAuth integration is to use the `createOIDCActivation` method. This extension function on `PowerAuthSDK` initiates the activation process by calling the PowerAuth Standard RESTful API.


```kotlin
try {
  val attributes = UriUtils.processDeeplink(deeplinkUri, oidcAuth)
  powerAuthSDK.createOIDCActivation(attributes, "Petr's phone", object : ICreateActivationListener {
    override fun onActivationCreateSuccess(activationResult: CreateActivationResult) {
      // Activation succeeded with activationResult
    }

    override fun onActivationCreateFailed(error: Throwable) {
      // Activation failed with error
    }
  })
} catch (e: Exception) {
  // Handle OIDC deeplink processing failure
}
```

## OIDCUtils

#### PKCE

Provides methods for generating PKCE codes.

- **`createPKCE`**: Generates a code verifier and code challenge based on the length input.

```kotlin
try {
    val pkceCodes = OIDCUtils.createPKCE(32)
    // PKCE Codes created successfully
} catch (e: Exception) {
    // Error generating PKCE codes
}
```

#### Random String Generating

Provides methods to generate random strings in Base64 URL-safe format, useful for creating nonces and states.

- **`getRandomBase64UrlSafe`**: Generates a code verifier and code challenge based on the length input.

```kotlin
try {
    val nonce = OIDCUtils.getRandomBase64UrlSafe(32)
    // Random string generated successfully
} catch (e: Exception) {
    // Error generating random string
}
```

#### Uri

Provides methods for handling URIs.

- **`createAuthorizationUri`**: Constructs an authorization URI.

```kotlin
try {
    val authorizationUri = OIDCUtils.createAuthorizationUri(config, nonce, state, pkceCodes)
    println("Authorization URI: $authorizationUri")
} catch (e: Exception) {
    println("Error creating authorization URI: ${e.message}")
}
```

- **`processDeeplink`**: Processes a deeplink to extract activation attributes.

```kotlin
try {
    val activationAttributes = OIDCUtils.processDeeplink(deeplinkUri, oidcAuth)
    println("Activation attributes ready: $activationAttributes")
} catch (e: Exception) {
    println("Failed to process deeplink URI: ${e.message}")
}
```
