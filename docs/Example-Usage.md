# Example Usage

This is an example of the most common use case of this SDK - fetching operations and approving them.

## SDK Integration

Follow the [SDK Integration](./SDK-Integration.md) tutorial for SDK installation.

## Example Code

```swift
// PowerAuth instance needs to be configured and a user-activated instance.
// More about PowerAuth SDK can be found here: https://github.com/wultra/powerauth-mobile-sdk


    fun exampleUsage(appContext: Context, powerAuth: PowerAuthSDK) {
        // Create the WultraMobileToken instance
        val wmt = powerAuth.createWultraMobileToken(appContext, acceptLanguage = "de")

        // Launch a coroutine to handle the async operation
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Fetch the operations asynchronously
                val operations = suspendCancellableCoroutine { cont ->
                    wmt.operations.getOperations {
                        it.onSuccess { operationList ->
                            cont.resume(operationList) // Resume with the result
                        }.onFailure { error ->
                            cont.resumeWithException(error) // Resume with an exception
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    operations.forEach { operation ->
                        // handle operations on the main thread
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    handle error on the main thread
                }
            }
        }
    }

```

For more examples see [IntegrationTests](https://github.com/wultra/mtoken-sdk-ios/blob/develop/WultraMobileTokenSDKTests/IntegrationTests.swift)

## Read Next

- [Using Operations Service](./Using-Operations-Service.md)
- [Using Push Service](./Using-Push-Service.md)
- [Using Inbox Service](./Using-Inbox-Service.md)
- [Using OIDC Service](./Using-OIDC-Service.md)
