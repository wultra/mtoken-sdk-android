# Using Operations Service

<!-- begin TOC -->
- [Introduction](#introduction)
- [Creating an Instance](#creating-an-instance)
- [Retrieve Pending Operations](#retrieve-pending-operations)
- [Start Periodic Polling](#start-periodic-polling)
- [Approve an Operation](#approve-an-operation)
- [Reject an Operation](#reject-an-operation)
- [Off-line Authorization](#off-line-authorization)
- [Operations API Reference](#operations-api-reference)
- [UserOperation](#useroperation)
- [Creating a Custom Operation](#creating-a-custom-operation)
<!-- end -->

## Introduction

Operations Service is responsible for fetching the operation list and for approving or rejecting operations.

An operation can be anything you need to be approved or rejected by the user. It can be for example money transfer, login request, access approval, ...

> __Note:__ Before using Operations Service, you need to have a `PowerAuthSDK` object available and initialized with a valid activation. Without a valid PowerAuth activation, all endpoints will return an error

Operations Service communicates with a backend via [Mobile Token API endpoints](https://github.com/wultra/powerauth-webflow/blob/develop/docs/Mobile-Token-API.md).

## Creating an Instance

### Factory Extension With SSL Validation Strategy

Convenience factory method that will return a new instance. A new [`OkHttpClient`](https://square.github.io/okhttp/) will be created based on the chosen `SSLValidationStrategy` in the last parameter.

```kotlin
fun PowerAuthSDK.createOperationsService(appContext: Context, baseURL: String, strategy: SSLValidationStrategy): IOperationsService
``` 

- `appContext` - application context
- `baseURL` - address, where your operations server can be reached
- `strategy` - strategy used when validating HTTPS requests. Following strategies can be used:
    - `SSLValidationStrategy.default`
    - `SSLValidationStrategy.noValidation`
    - `SSLValidationStrategy.sslPinning`

### Factory Extension With OkHttpClient

Convenience factory method that will return a new instance with provided [`OkHttpClient`](https://square.github.io/okhttp/) that you can configure on your own.

```kotlin
fun PowerAuthSDK.createOperationsService(appContext: Context, baseURL: String, httpClient: OkHttpClient): IOperationsService
``` 

- `appContext` - application context
- `baseURL`-  address, where your operations server can be reached
- `httpClient` - [`OkHttpClient`](https://square.github.io/okhttp/) instance used for API requests 

## Retrieve Pending Operations

To fetch the list with pending operations, implement the `IOperationsService` API, you can call:

```kotlin
operationsService.getOperations(object: IGetOperationListener {
    override fun onSuccess(operations: List<UserOperation>) {
        // render operations
    }
    override fun onError(error: ApiError) {
        // render error state
    }
})
```

After you retrieve the pending operations, you can render them in the UI, for example, as a list of items with a detail of operation shown after a tap.

*Note: The language of the UI data inside the operation depends on the configuration of the `IOperationsService.acceptLanguage`.*

## Start Periodic Polling

Mobile token API is highly asynchronous - to simplify the work for you, we added a convenience operation list polling feature:

```kotlin
// fetch new operations every 7 seconds periodically
if (!operationsService.isPollingOperations()) {
    operationsService.startPollingOperations(7_000, false)
}
```

To receive the result of the polling, set up a listener.

_Note that the listener is called for all "fetch operations" requests (not just the polling)._

```kotlin
operationsService.listener = object: IOperationsServiceListener {
    override fun operationsFailed(error: ApiError) {
        // show UI the last fetch has failed
    }

    override fun operationsLoaded(operations: List<UserOperation>) {
        // refresh operation list UI
    }

    override fun operationsLoading(loading: Boolean) {
        // show loading UI
    }
}
```

## Approve an Operation

To approve an operation use `IOperationsService.authorizeOperation`. You can simply use it with the following example:

```kotlin
// Approve operation with password
fun approve(operation: IOperation, password: String) {
    
    val auth = PowerAuthAuthentication()
    auth.usePossession = true
    auth.usePassword = password
    auth.useBiometry = null // needed only when approving with biometry

    operationsService.authorizeOperation(operation, authentication, object: IAcceptOperationListener {
        override fun onSuccess() {
            // show success UI
        }
    
        override fun onError(error: ApiError) {
            // show error UI
        }
    })
}
```

## Reject an Operation

To reject an operation use `IOperationsService.rejectOperation`. Operation rejection is confirmed by possession factor so there is no need for creating  `PowerAuthAuthentication ` object. You can simply use it with the following example.

```kotlin
// Reject operation with some reason
fun reject(operation: IOperation, reason: RejectionReason) {
    operationsService.rejectOperation(operation, reason, object: IRejectOperationListener {
        override fun onSuccess() {
            // show success UI
        }

        override fun onError(error: ApiError) {
            // show error UI
        }
    })
}
```

## Off-line Authorization

In case the user is not online, you can use off-line authorizations. In this operation mode, the user needs to scan a QR code, enter PIN code or use biometry, and rewrite the resulting code. Wultra provides a special format for [the operation QR codes](https://github.com/wultra/powerauth-webflow/blob/develop/docs/Off-line-Signatures-QR-Code.md), that are automatically processed with the SDK.

To process the operation QR code, you can use:

```kotlin
@Throws(IllegalArgumentException::class)
fun onQROperationScanned(scannedCode: String): QROperation {
    // retrieve parsed operation
    val operation = QROperationParser.parse(scannedCode)
    // verify the signature against the powerauth instance
    val verified = powerAuthSDK.verifyServerSignedData(operation.signedData, operation.signature.signature, operation.signature.isMaster())
    if (!verified) {
        throw IllegalArgumentException("Invalid offline operation")
    }
    return operation
}
```

After that, you can produce an off-line signature using the following code:

```kotlin
@Throws
fun approveQROperation(operation: QROperation, password: String): String {
    val authentication = PowerAuthAuthentication()
    authentication.usePossession = true
    authentication.usePassword = password
    return operationsService.authorizeOfflineOperation(operation, authentication)
}
```

## Operations API Reference

All available methods and attributes of `IOperationsService` API are:

- `listener` - Listener object that receives info about operation loading.
- `acceptLanguage` - Language settings, that will be sent along with each request. The server will return properly localized content based on this value. Value follows standard RFC [Accept-Language](https://tools.ietf.org/html/rfc7231#section-5.3.5)
- `getLastOperationsResult()` - Cached last operations result.
- `isLoadingOperations()` - Indicates if the service is loading operations.
- `getOperations(listener: IGetOperationListener?)` - Retrieves pending operations from the server.
    - `listener` - Called when operation finishes.
- `isPollingOperations()` - If the app is periodically polling for the operations from the server.
- `startPollingOperations(pollingInterval: Long, delayStart: Boolean)` - Starts periodic operation polling.
    - `pollingInterval` - How often should operations be refreshed.
    - `delayStart` - When true, polling starts after the first `pollingInterval` time passes.
- `stopPollingOperations()` - Stops periodic operation polling.
- `authorizeOperation(operation: IOperation, authentication: PowerAuthAuthentication, listener: IAcceptOperationListener)` - Authorize provided operation.
    - `operation` - An operation to approve, retrieved from `getOperations` call or [created locally](#creating-a-custom-operation).
    - `authentication` - PowerAuth authentication object for operation signing.
    - `listener` - Called when authorization request finishes.
- `rejectOperation(operation: IOperation, reason: RejectionReason, listener: IRejectOperationListener)` - Reject provided operation.
    - `operation` - An operation to reject, retrieved from `getOperations` call or [created locally](#creating-a-custom-operation).
    - `reason` - Rejection reason.
    - `listener` - Called when rejection request finishes.
- `fun authorizeOfflineOperation(operation: QROperation, authentication: PowerAuthAuthentication)` - Sign offline (QR) operation
    - `operation` - Offline operation retrieved via `QROperationParser.parse` method.
    - `authentication` - PowerAuth authentication object for operation signing.
- `signOfflineOperationWithBiometry(biometry: ByteArray, offlineOperation: QROperation)` - Sign offline (QR) operation with biometry data.
    - `biometry` - Biometry data retrieved from `powerAuthSDK.authenticateUsingBiometry` call.
    - `offlineOperation` - Offline operation retrieved via `processOfflineQrPayload` method.

## UserOperation

Operations objects retrieved through the `getOperations` API method (like `getOperations` method in `IOperationsService`) are called "user operations".

Under this abstract name, you can imagine for example "Login operation", which is a request for signing in to the online account in a web browser on another device. **In general, it can be any operation that can be either approved or rejected by the user.**

Visually, the operation should be displayed as an info page with all the attributes (rows) of such operation, where the user can decide if he wants to approve or reject it.

Definition of the `UserOperations`:

```kotlin
class UserOperation: IOperation {

    // Unique operation identifier
    val id: String
        
    // System name of the operation.
    //
    // This property lets you adjust the UI for various operation types. 
    // For example, the "login" operation may display a specialized interface with 
    // an icon or an illustration, instead of an empty list of attributes, 
    // "payment" operation can include a special icon that denotes payments, etc.
    val name: String
    
    // Actual data that will be signed.
    val data: String
    
    // Date and time when the operation was created.
    val created: ZonedDateTime
    
    // Date and time when the operation will expire.
    val expires: ZonedDateTime
    
    // Data that should be presented to the user.
    val formData: FormData
    
    // Allowed signature types.
    //
    // This hints if the operation needs a 2nd factor or can be approved simply by 
    // tapping an approve button. If the operation requires 2FA, this value also hints if 
    // the user may use the biometry, or if a password is required.
    val allowedSignatureType: AllowedSignatureType
}
```

Definition of `FormData`: 

```kotlin
class FormData {
    
    /// Title of the operation
    val title: String
    
    /// Message for the user
    val message: String
    
    /// Other attributes.
    ///
    /// Each attribute presents one line in the UI. Attributes are differentiated by type property
    /// and specific classes such as NoteAttribute or AmountAttribute.
    val attributes: List<Attribute>
}
```

Attributes types:  
- `AMOUNT` like "100.00 CZK"  
- `KEY_VALUE` any key value pair  
- `NOTE` just like `KEY_VALUE`, emphasizing that the value is a note or message  
- `HEADING` single highlighted text, written in a larger font, used as a section heading  
- `PARTY_INFO` providing structured information about third-party data (for example known eshop)

## Creating a Custom Operation

In some specific scenarios, you might need to approve or reject an operation that you received through a different channel than `getOperations`. In such cases, you can implement the `IOperation` interface in your custom class and then feed created objects to both `authorizeOperation` and `rejectOperation` methods.

_Note: For such cases, you can use concrete convenient class `LocalOperation`, that implements this interface._

Definition of the `IOperation`:

```kotlin
interface IOperation {

    /**
     * Operation identifier
     */
    val id: String

    /**
     * Data for signing
     */
    val data: String
}
```
