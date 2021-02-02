# Operation Expiration Handling

Every operation should have an expiration time. An expired operation cannot be confirmed nor rejected - the server will return an error with the appropriate error. 

## Retrieving expiration time 

### UserOperation
The `UserOperation` provided by the `IOperations` service has its expiration time inside the `expires` property.

### Custom operation
If you're creating your own custom operation by implementing the `IOperation` interface, you need to provide the expiration time by yourself. The expiration time is optional because it's not part of the operation signature.

## Handling via push notifications

If the device is [registered to receive push notifications](Using-Push-Service.md), it will receive an [`PushMessageOperationFinished `](https://github.com/wultra/mtoken-sdk-android/blob/develop/library/src/main/java/com/wultra/android/mtokensdk/push/PushParser.kt#L80#docucheck-keep-link) notification with the [`TIMEOUT `](https://github.com/wultra/mtoken-sdk-android/blob/develop/library/src/main/java/com/wultra/android/mtokensdk/push/PushParser.kt#L115#docucheck-keep-link) result when the operation expires.

__Operation list should be refreshed on such notification.__


_Please be aware that push notifications are not guaranteed to be received. There are several scenarios where push notification delivery will fail, such as:_

- _user didn't grant notification permission to your app_
- _network error_
- _notification token has expired and is waiting for renewal_
- ...

## Local handling

Since push notifications are not guaranteed to be delivered, you should implement a mechanism that will refresh the list to validate if it was expired on the server too.

__Server and client device time could differ! You should never remove the operation just locally, but refresh the operation list instead.__

### OperationExpirationWatcher

Utility class that will observe operations and informs you when it expired.

_Sample implementation:_

```kotlin
// Sample implementation of a class that's using the OperationExpirationWatcher
class OperationsManager(private val ops: IOperationsService) {
    
    private val operationWatcher = OperationExpirationWatcher()
    
    init {
        operationWatcher.listener = object : OperationExpirationWatcherListener {
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