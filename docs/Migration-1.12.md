# Migration from 1.11.x to 1.12.x

This guide contains instructions for migration from Wultra Mobile Token SDK for Android version `1.11.x` to version `1.12.x`.

## 1. Implement status in UserOperation

### Added Functionality

The following status property was added to the `WMTUserOperations`:

```kotlin
val status: UserOperationStatus

/** Processing status of the operation */
enum class UserOperationStatus {
    /** Operation was approved */
    APPROVED,
    /** Operation was rejected */
    REJECTED,
    /** Operation is pending its resolution */
    PENDING,
    /** Operation was canceled */
    CANCELED,
    /** Operation expired */
    EXPIRED,
    /** Operation failed */
    FAILED
}
```

The `UserOperationStatus` within `UserOperation.status`  now represents the status of an operation, making the `OperationHistoryEntryStatus` and `OperationHistoryEntry` redundant. As a result, `OperationHistoryEntry` has been removed. In all instances where `OperationHistoryEntry` was previously used, `UserOperation` is used instead.

### Replaced at

In the `getHistory` method of `IOperationsService`, `OperationHistoryEntry` has been replaced by `UserOperation` for retrieving user operation history.

```kotlin
    /// Retrieves the history of operations
    /// - Parameters:
    ///   - authentication: Authentication object for signing.
    ///   - completion: Result completion.
    ///                 This completion is always called on the main thread.
    /// - Returns: Operation object for its state observation.
    fun getHistory(authentication: PowerAuthAuthentication, callback: (result: Result<List<UserOperation>>) -> Unit)
```

## 2. Deprecated methods removal

`IOperationsService` Methods which were deprecated in version 1.5.0 are no more available. For further assistance consult [Migration from 1.4.x to 1.5.x](Migration-1.5.md)

