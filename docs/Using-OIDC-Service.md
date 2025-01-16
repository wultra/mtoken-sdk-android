# OIDC and PowerAuth Integration

- [Introduction](#introduction)
- [Creating an Instance](#creating-an-instance)
- [Preparing for OIDC Activation](#preparing-for-oidc-activation)
- [Open authorize URI in a web browser](#open-authorize-uri-in-a-web-browser)
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
val oidcService = OidcService(
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
- `baseURL` - address, where your oidc server can be reached (ending with `/enrollment-server` in the default setup)


__Optional parameters:__

For these, if null is provided, default internal implementation is provided.

- `tokenProvider` - Provider that provides a valid PowerAuth token from token store for api communication.
- `userAgent` - Optional default user agent used for each request
- `gsonBuilder` - Optional GSON builder for custom deserialization


## Preparing for OIDC Activation

To prepare for an OIDC activation, you have two main approaches depending on how much control you want over the process:

### a) OIDC service prepares it all with `prepareOidcActivation`

For a streamlined approach, use the `prepareOidcActivation` method. This method handles all the steps for you, including fetching the OIDC configuration, generating PKCE codes, creating `nonce` and `state`, and constructing the authorization URI.

##### Example:

```kotlin
oidcService.prepareOidcActivation("example_provider") { result ->
  result.onSuccess { oidcAuthRequest ->
    // Use oidcAuthRequest.authorizeUri to open the browser
  }.onFailure { error ->
    // handle error
  }
}
```

##### OidcAuthorizationRequest

Encapsulates the data required to initiate the OIDC authorization flow and also other properties for PowerAuth Activation flow.

| Property         | Type      | Description                                             |
|------------------|-----------|---------------------------------------------------------|
| `authorizeUri`   | `Uri`     | URI to redirect the user for OIDC authentication.       |
| `providerId`     | `String`  | Identifier for the OIDC provider configuration.         |
| `nonce`          | `String`  | Random value to prevent replay attacks.                |
| `state`          | `String`  | Random value to maintain state between request/callback.|
| `codeVerifier`   | `String?` | PKCE code verifier, if applicable.                      |



### b) Manual Configuration with `getConfig`

In this approach, you fetch the OIDC configuration based on a predefined `providerId` and handle the rest of the activation preparation manually.

##### Steps:
1. Use the `getConfig` method to fetch the OIDC configuration.
2. You can manually construct the authorization URI using the provided utility classes.
  - `PKCEUtils.create` for generating PKCE codes.
  - `RandomGeneratorUtils.getRandomBase64UrlSafe` for generating `nonce` and `state`.
  - `UriUtils.createAuthorizationUri` for constructing the authorization URI.


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

### Processing a deeplink 

The `UriUtils.processDeeplinkOidc` utility function extracts and validates the data needed to initiate PowerAuth activation from the OIDC flow's callback URI.


##### PowerAuthActivationAttributes

Represents the attributes required to initiate a PowerAuth activation after completing an OIDC flow.

| Property       | Type      | Description                                        |
|----------------|-----------|----------------------------------------------------|
| `providerId`   | `String`  | Identifier for the OIDC provider configuration.    |
| `code`         | `String`  | Authorization code received from the OIDC flow.    |
| `nonce`        | `String`  | Random value for ensuring integrity of the flow.   |
| `codeVerifier` | `String?` | PKCE code verifier, if applicable.                 |


### Initiating PowerAuth Activation with OIDC

The final step in the OIDC and PowerAuth integration is to use the `createOidcActivation` method. This extension function on `PowerAuthSDK` initiates the activation process by calling the PowerAuth Standard RESTful API.


```kotlin
val attributes = UriUtils.processDeeplinkOidc(oidcAuth, deeplinkUri)
if (attributes != null) {
    powerAuthSDK.createOidcActivation(attributes, object : ICreateActivationListener {
        override fun onActivationCreateSuccess(activationResult: CreateActivationResult) {
            // Activation succeeded with activationResult
        }

        override fun onActivationCreateFailed(error: Throwable) {
            // Activation failed with error
        }
    })
} else {
    // Failed to process OIDC deeplink.
}
```

## OIDCUtils

#### PKCEUtils

Provides methods for generating PKCE codes.

- **`createPKCE`**: Generates a code verifier and code challenge based on the length input.

```kotlin
val pkceResult = OidcUtils.createPKCE(32)
pkceResult.onSuccess { pkceCodes ->
    // PKCE Codes created
}.onFailure { error ->
    // Error during generating PKCE codes
}
```

#### RandomGeneratorUtils

Provides methods to generate random strings in Base64 URL-safe format, useful for creating nonces and states.

- **`getRandomBase64UrlSafe`**: Generates a code verifier and code challenge based on the length input.

```kotlin
val nonce = OidcUtils.getRandomBase64UrlSafe(32)
```

#### UriUtils

Provides methods for handling URIs.

- **`createAuthorizationUri`**: Constructs an authorization URI.

```kotlin
val uriResult = UriUtils.createAuthorizationUri(config, nonce, state, pkceCodes)
uriResult.onSuccess { uri ->
    println("Authorization URI: $uri")
}.onFailure { error ->
    println("Error creating authorization URI: ${error.message}")
}
```

- **`processDeeplink`**: Processes a deeplink to extract activation attributes.

```kotlin
val activationAttributes = UriUtils.processDeeplinkOidc(oidcAuth, deeplinkUri)
if (activationAttributes != null) {
    println("Activation attributes ready: $activationAttributes")
} else {
    println("Failed to process deeplink URI.")
}
```
