# Migration from 1.4.x to 1.5.x

This guide contains instructions for migration from Wultra Mobile Token SDK for Android version `1.4.x` to version `1.5.x`.

## Deprecated APIs

All API calls based on listener interfaces are now deprecated and you can now use the new callbacks returning `Result<T>` as a replacement.

Deprecated functions in `IOperationsService`:

- `fun getLastOperationsResult(): OperationsResult?`
  - Replace with `lastOperationsResult` property.
- `fun getOperations(listener: IGetOperationListener?)`
  - Replace with `fun getOperations(callback: (result: Result<List<UserOperation>>) -> Unit)`
- `fun getHistory(authentication: PowerAuthAuthentication, listener: IGetHistoryListener)`
  - Replace with `fun getHistory(authentication: PowerAuthAuthentication, callback: (result: Result<List<OperationHistoryEntry>>) -> Unit)`
- `fun authorizeOperation(operation: IOperation, authentication: PowerAuthAuthentication, listener: IAcceptOperationListener)`
  - Replace with `fun authorizeOperation(operation: IOperation, authentication: PowerAuthAuthentication, callback: (result: Result<Unit>) -> Unit)`
- `fun rejectOperation(operation: IOperation, reason: RejectionReason, listener: IRejectOperationListener)`
  - Replace with `fun rejectOperation(operation: IOperation, reason: RejectionReason, callback: (result: Result<Unit>) -> Unit)`

Deprecated functions in `IPushService`:

- `fun register(fcmToken: String, listener: IPushRegisterListener)`
  - Replace with `fun register(fcmToken: String, callback: (result: Result<Unit>) -> Unit)`
  
Deprecated interfaces and classes:

- `interface IAcceptOperationListener`
- `interface IRejectOperationListener`
- `interface IGetOperationListener`
- `interface IGetHistoryListener`
- `interface IPushRegisterListener`
- `abstract class OperationsResult`
- `data class SuccessOperationsResult`
- `data class ErrorOperationsResult`
