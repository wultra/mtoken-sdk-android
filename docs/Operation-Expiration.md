# Operation Expiration Handling

Every operation should have an expiration time. An expired operation cannot be confirmed nor rejected - the server will return an error with the appropriate error.

## Retrieving Expiration Time

### UserOperation
The `UserOperation` provided by the `IOperations` service has its expiration time inside the `expires` property.

### Custom Operation
If you're creating your own custom operation by implementing the `IOperation` interface, you need to provide the expiration time by yourself. The expiration time is optional because it's not part of the operation signature.

## Handling via Push Notifications

If the device is [registered to receive push notifications](Using-Push-Service.md), it will receive an [`PushMessageOperationFinished `](https://github.com/wultra/mtoken-sdk-android/blob/develop/library/src/main/java/com/wultra/android/mtokensdk/push/PushParser.kt#L80#docucheck-keep-link) notification with the [`TIMEOUT `](https://github.com/wultra/mtoken-sdk-android/blob/develop/library/src/main/java/com/wultra/android/mtokensdk/push/PushParser.kt#L115#docucheck-keep-link) result when the operation expires.

Operation list should be refreshed on such notification.


Please be aware that push notifications are not guaranteed to be received. There are several scenarios where push notification delivery will fail, such as:

- user didn't grant notification permission to your app
- network error occurred
- notification token has expired and is waiting for renewal
- ... other situations not under the developer's control

## Local Handling

Since push notifications are not guaranteed to be delivered, you should implement a mechanism that will refresh the list to validate if it was expired on the server too.

Server and client device time could differ! You should never remove the operation just locally, but refresh the operation list instead.

### OperationExpirationWatcher

Utility class that will observe operations and informs you when it expired.

#### Sample Implementation

```kotlin
// Sample implementation of a class that's using the OperationExpirationWatcher
class OperationsManager(private val ops: IOperationsService) {

    private val operationWatcher = OperationExpirationWatcher()

    init {
        operationWatcher.listener = object : OperationExpirationWatcherListener {
            // operationsExpired is called on main thread
            override fun operationsExpired(expiredOperations: List<ExpirableOperation>) {
                // some operation expired, refresh the list
                launchFetchOperations()
                // this behavior could be improved for example with
                // checking if the expired operations is currently displayed etc..
            }
        }
    }

    fun launchFetchOperations() {
        ops.getOperations(object : IGetOperationListener {
            override fun onSuccess(operations: List<UserOperation>) {
                // simplified but working example how operations can be observed for expiration
                operationWatcher.removeAll()
                operationWatcher.add(operations)
                // process operations
            }
            override fun onError(error: ApiError) {
                // process error
            }
        })
    }
}

```
