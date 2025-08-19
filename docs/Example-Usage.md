# Example Usage

This is an example of the most common use case of this SDK - fetching operations and approving them.

## SDK Integration

Follow the [SDK Integration](./SDK-Integration.md) tutorial for SDK installation.

## Example Code

```kotlin
// PowerAuth instance needs to be configured and a user-activated instance.
// More about PowerAuth SDK can be found here: https://github.com/wultra/powerauth-mobile-sdk


fun exampleUsage(appContext: Context, powerAuth: PowerAuthSDK) {
    // Create the WultraMobileToken instance
    val wmt = powerAuth.createWultraMobileToken(appContext, acceptLanguage = "de")

    // Fetch operations using a callback
    wmt.operations.getOperations {
        it.onSuccess { operations ->
            // Handle operations
            for (operation in operations) {
                // Process each operation
            }
        }.onFailure { error ->
            // Handle error
        }
    }
}

```

For more examples see [IntegrationTests](https://github.com/wultra/mtoken-sdk-android/blob/develop/library/src/androidTest/java/IntegrationTests.kt)

## Read Next

- [Using Operations Service](./Using-Operations-Service.md)
- [Using Push Service](./Using-Push-Service.md)
- [Using Inbox Service](./Using-Inbox-Service.md)
- [Using OIDC Service](./Using-OIDC-Service.md)
