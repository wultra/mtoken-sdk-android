# Using Operations Service

<!-- begin remove -->
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
- [TOTP ProximityCheck](#totp-proximitycheck)
## Introduction
<!-- end -->

Operations Service is responsible for fetching the operation list and for approving or rejecting operations.

An operation can be anything you need to be approved or rejected by the user. It can be for example money transfer, login request, access approval, ...

<!-- begin box warning -->
Note: Before using Operations Service, you need to have a `PowerAuthSDK` object available and initialized with a valid activation. Without a valid PowerAuth activation, all endpoints will return an error.
<!-- end -->

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

__Optional parameters:__

- `userAgent` - Optional default user agent used for each request
- `gsonBuilder` - Optional GSON builder for custom deserialization 

### Factory Extension With OkHttpClient

Convenience factory method that will return a new instance with provided [`OkHttpClient`](https://square.github.io/okhttp/) that you can configure on your own.

```kotlin
fun PowerAuthSDK.createOperationsService(appContext: Context, baseURL: String, httpClient: OkHttpClient): IOperationsService
```

- `appContext` - application context
- `baseURL`-  address, where your operations server can be reached
- `httpClient` - [`OkHttpClient`](https://square.github.io/okhttp/) instance used for API requests

__Optional parameters:__

- `userAgent` - Optional default user agent used for each request
- `gsonBuilder` - Optional GSON builder for custom deserialization

## Retrieve Pending Operations

To fetch the list with pending operations, implement the `IOperationsService` API, you can call:

```kotlin
operationsService.getOperations {
    it.onSuccess {
        // render operations
    }.onFailure {
        // render error state
    }
}
```

After you retrieve the pending operations, you can render them in the UI, for example, as a list of items with a detail of operation shown after a tap.

<!-- begin box warning -->
Note: The language of the UI data inside the operation depends on the configuration of the `IOperationsService.acceptLanguage`.
<!-- end -->

## Start Periodic Polling

Mobile token API is highly asynchronous - to simplify the work for you, we added a convenience operation list polling feature:

```kotlin
// fetch new operations every 7 seconds periodically
if (!operationsService.isPollingOperations()) {
    operationsService.startPollingOperations(7_000, false)
}
```

To receive the result of the polling, set up a listener.

<!-- begin box warning -->
Note that the listener is called for all "fetch operations" requests (not just the polling).
<!-- end -->

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

To approve an operation use `IOperationsService.authorizeOperation`. You can simply use it with following examples:

```kotlin
// Approve operation with password
fun approve(operation: IOperation, password: String) {

    val auth = PowerAuthAuthentication.possessionWithPassword(password)
    this.operationsService.authorizeOperation(operation, auth) {
        it.onSuccess {
            // show success UI
        }.onFailure {
            // show error UI
        }
    }
}
```

To approve offline operations with biometry, your PowerAuth instance [need to be configured with biometry factor](https://github.com/wultra/powerauth-mobile-sdk/blob/develop/docs/PowerAuth-SDK-for-Android.md#biometric-authentication-setup).

```kotlin
// Approve operation with biometry
fun approveWithBiometry(operation: IOperation) {

    // UserOperation contains information if biometry can be used
    if (operation is UserOperation) {
        if (!operation.allowedSignatureType.factors.contains(AllowedSignatureType.Factor.POSSESSION_BIOMETRY)) {
            return
        }
    }

    this.powerAuthSDK.authenticateUsingBiometry(appContext, fragmentManager,
        "Operation approval",
        "Use biometry to approve the operation",
        object : IBiometricAuthenticationCallback {

            override fun onBiometricDialogSuccess(biometricKeyData: BiometricKeyData) {
                val auth = PowerAuthAuthentication.possessionWithBiometry(biometricKeyData.derivedData)
                this.operationsService.authorizeOperation(operation, auth) {
                    it.onSuccess {
                        // show success UI
                    }.onFailure {
                        // show error UI
                    }
                }
            }

            override fun onBiometricDialogCancelled(userCancel: Boolean) {
                // the biometry dialog was canceled
            }

            override fun onBiometricDialogFailed(error: PowerAuthErrorException) {
                // biometry authentication failed
            }
        }
    )
}
```

## Reject an Operation

To reject an operation use `IOperationsService.rejectOperation`. Operation rejection is confirmed by possession factor so there is no need for creating  `PowerAuthAuthentication ` object. You can simply use it with the following example.

```kotlin
// Reject operation with some reason
fun reject(operation: IOperation, reason: RejectionReason) {
    this.operationsService.rejectOperation(operation, reason) {
        it.onSuccess {
            // show success UI
        }.onFailure {
            // show error UI
        }
    }
}
```

## Operation History

You can retrieve an operation history via the `IOperationsService.getHistory` method. The returned result is operations and their current status.

```kotlin
// Retrieve operation history with password
fun history(password: String) {

    val auth = PowerAuthAuthentication.possessionWithPassword(password)

    this.operationService.getHistory(auth) {
        it.onSuccess {
            // process operation history
        }.onFailure {
            // process error
        }
    }
}
```

Note that the operation history availability depends on the backend implementation and might not be available. Please consult this with your backend developers.

## Off-line Authorization

In case the user is not online, you can use off-line authorizations. In this operation mode, the user needs to scan a QR code, enter PIN code or use biometry, and rewrite the resulting code. Wultra provides a special format for [the operation QR codes](https://github.com/wultra/powerauth-webflow/blob/develop/docs/Off-line-Signatures-QR-Code.md), that are automatically processed with the SDK.

### Processing Scanned QR Operation

```kotlin
@Throws(IllegalArgumentException::class)
fun onQROperationScanned(scannedCode: String): QROperation {
    // retrieve parsed operation
    val operation = QROperationParser.parse(scannedCode)
    // verify the signature against the powerauth instance
    val verified = this.powerAuthSDK.verifyServerSignedData(operation.signedData, operation.signature.signature, operation.signature.isMaster())
    if (!verified) {
        throw IllegalArgumentException("Invalid offline operation")
    }
    return operation
}
```

### Authorizing Scanned QR Operation

<!-- begin box info -->
An offline operation needs to be __always__ approved with __2-factor scheme__ (password or biometry).
<!-- end -->

<!-- begin box info -->
Each offline operation created on the server has an __URI ID__ to define its purpose and configuration. The default value used here is `/operation/authorize/offline` and can be modified with the `uriId` parameter in the `authorize` method.
<!-- end -->

#### With Password

```kotlin
// Approves QR operation with password
fun approveQROperation(operation: QROperation, password: String) {
    val auth = PowerAuthAuthentication.possessionWithPassword(password)
    try {
        val offlineSignature = this.operationsService.authorizeOfflineOperation(operation, auth)
        // Display the signature to the user so it can be manually rewritten.
        // Note that the operation will be signed even with the wrong password!
    } catch (e: Exception) {
       // Failed to sign the operation
    }
}
```

<!-- begin box info -->
An offline operation can and will be signed even with an incorrect password. The signature cannot be used for manual approval in such a case. This behavior cannot be detected, so you should warn the user that an incorrect password will result in an incorrect "approval code".
<!-- end -->

#### With Password and Custom `uriId`

```kotlin
// Approves QR operation with password
fun approveQROperation(operation: QROperation, password: String) {
    val auth = PowerAuthAuthentication.possessionWithPassword(password)
    try {
        val offlineSignature = this.operationsService.authorizeOfflineOperation(operation, auth, "/confirm/offline/operation")
        // Display the signature to the user so it can be manually rewritten.
        // Note that the operation will be signed even with the wrong password!
    } catch (e: Exception) {
       // Failed to sign the operation
    }
}
```

#### With Biometry

To approve offline operations with biometry, your PowerAuth instance [need to be configured with biometry factor](https://github.com/wultra/powerauth-mobile-sdk/blob/develop/docs/PowerAuth-SDK-for-Android.md#biometric-authentication-setup).

To determine if biometry can be used for offline operation authorization, use `QROperation.flags.biometryAllowed`.

```kotlin
// Approves QR operation with biometry
fun approveQROperationWithBiometry(operation: QROperation, appContext: Context, fragmentManager: FragmentManager) {

    if (!operation.flags.biometryAllowed) {
        // biometry usage is not allowed on this operation
        return
    }

    this.powerAuthSDK.authenticateUsingBiometry(appContext, fragmentManager,
        "Operation approval",
        "Use biometry to approve the operation",
        object : IBiometricAuthenticationCallback {

            override fun onBiometricDialogSuccess(biometricKeyData: BiometricKeyData) {
                val auth = PowerAuthAuthentication.possessionWithBiometry(biometricKeyData.derivedData)
                try {
                    val offlineSignature = operationsService.authorizeOfflineOperation(operation, auth)
                    // Display the signature to the user so it can be manually rewritten.
                } catch (e: Exception) {
                    // Failed to sign the operation
                }
            }

            override fun onBiometricDialogCancelled(userCancel: Boolean) {
                // the biometry dialog was canceled
            }

            override fun onBiometricDialogFailed(error: PowerAuthErrorException) {
                // biometry authentication failed
            }
        }
    )
}
```

## Operations API Reference

All available methods and attributes of `IOperationsService` API are:

- `listener` - Listener object that receives info about operation loading.
- `acceptLanguage` - Language settings, that will be sent along with each request. The server will return properly localized content based on this value. Value follows standard RFC [Accept-Language](https://tools.ietf.org/html/rfc7231#section-5.3.5)
- `lastOperationsResult` - Cached last operations result.
- `currentServerDate()` - Current server date. This is a calculated property based on the difference between the phone date and the date on the server. Value is available after the first successful operation list request. It might be nil if the server doesn't provide such a feature.
- `isLoadingOperations()` - Indicates if the service is loading operations.
- `getOperations(callback: (result: Result<List<UserOperations>>) -> Unit)` - Retrieves pending operations from the server.
  - `callback` - Called when getting list request finishes.
- `fetchOperations()` - Retrieves pending operations from the server. This method is useful only if you set the listener to the service.
- `isPollingOperations()` - If the app is periodically polling for the operations from the server.
- `startPollingOperations(pollingInterval: Long, delayStart: Boolean)` - Starts periodic operation polling.
  - `pollingInterval` - How often should operations be refreshed.
  - `delayStart` - When true, polling starts after the first `pollingInterval` time passes.
- `stopPollingOperations()` - Stops periodic operation polling.
- `getHistory(authentication: PowerAuthAuthentication, callback: (result: Result<List<OperationHistoryEntry>>) -> Unit)` - Retrieves operation history
  - `authentication` - PowerAuth authentication object for signing.
  - `callback` - Called when getting history request finishes.
- `authorizeOperation(operation: IOperation, authentication: PowerAuthAuthentication, callback: (result: Result<Unit>) -> Unit)` - Authorize provided operation.
  - `operation` - An operation to approve, retrieved from `getOperations` call or [created locally](#creating-a-custom-operation).
  - `authentication` - PowerAuth authentication object for operation signing.
  - `callback` - Called when authorization request finishes.
- `rejectOperation(operation: IOperation, reason: RejectionReason, callback: (result: Result<Unit>) -> Unit)` - Reject provided operation.
  - `operation` - An operation to reject, retrieved from `getOperations` call or [created locally](#creating-a-custom-operation).
  - `reason` - Rejection reason.
  - `callback` - Called when rejection request finishes.
- `fun authorizeOfflineOperation(operation: QROperation, authentication: PowerAuthAuthentication, uriId: String)` - Sign offline (QR) operation
  - `operation` - Offline operation retrieved via `QROperationParser.parse` method.
  - `authentication` - PowerAuth authentication object for operation signing.
  - `uriId` - Custom signature URI ID of the operation. Use URI ID under which the operation was created on the server. Default value is `/operation/authorize/offline`.
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

    /** Unique operation identifier */
    val id: String

    /** 
     * System name of the operation.
     *
     * This property lets you adjust the UI for various operation types.
     * For example, the "login" operation may display a specialized interface with
     * an icon or an illustration, instead of an empty list of attributes,
     * "payment" operation can include a special icon that denotes payments, etc.
     */
    val name: String

    /** Actual data that will be signed. */
    val data: String

    /** Date and time when the operation was created. */
    val created: ZonedDateTime

    /** Date and time when the operation will expire. */
    val expires: ZonedDateTime

    /** Data that should be presented to the user. */
    val formData: FormData

    /** 
     * Allowed signature types.
     *
     * This hints if the operation needs a 2nd factor or can be approved simply by
     * tapping an approve button. If the operation requires 2FA, this value also hints if
     * the user may use the biometry, or if a password is required.
     */
    val allowedSignatureType: AllowedSignatureType
    
    
    /**
     *  Data for the operation UI presented
     *
     *  Accompanying information about the operation additional UI which should be presented such as
     *  Pre-Approval Screen or Post-Approval Screen
     */
    val ui: OperationUIData?
    
    /** Proximity Check Data to be passed when OTP is handed to the app */
    var proximityCheck: ProximityCheck? = null
}
```

Definition of `FormData`:

```kotlin
class FormData {

    /** Title of the operation */
    val title: String

    /** Message for the user */
    val message: String

    /**
     * Other attributes. 
     * 
     * Each attribute presents one line in the UI. Attributes are differentiated by type property
     * and specific classes such as NoteAttribute or AmountAttribute.
     */
    val attributes: List<Attribute>
}
```

Attributes types:  
- `AMOUNT` like "100.00 CZK"  
- `KEY_VALUE` any key value pair  
- `NOTE` just like `KEY_VALUE`, emphasizing that the value is a note or message  
- `HEADING` single highlighted text, written in a larger font, used as a section heading  
- `PARTY_INFO` providing structured information about third-party data (for example known e-shop)  
- `AMOUNT_CONVERSION` providing data about Money conversion  
- `IMAGE` image row  
- `UNKNOWN` fallback option when unknown attribute type is passed. Such attribute only contains the label.  

Definition of `OperationUIData`:

```kotlin
class OperationUIData {
    /** Confirm and Reject buttons should be flipped both in position and style */
    val flipButtons: Boolean?
    
    /** Block approval when on call (for example when on a phone or Skype call) */
    val blockApprovalOnCall: Boolean?
    
    /** UI for pre-approval operation screen */
    val preApprovalScreen: PreApprovalScreen?

    /**
     * UI for post-approval operation screen
     * 
     * Type of PostApprovalScreen is presented with different classes (Starting with `PostApprovalScreen*`)
     */
    val postApprovalScreen: PostApprovalScreen?
}
```

PreApprovalScreen types:

- `WARNING`
- `INFO`
- `QR_SCAN` this type indicates that the `ProximityCheck` must be used for authorization
- `UNKNOWN`

PostApprovalScreen types:
`PostApprovalScreen*` classes commonly contain `heading` and `message` and different payload data

- `REVIEW` provides an array of operations attributes with data: type, id, label, and note
- `REDIRECT` providing text for button, countdown, and redirection URL
- `GENERIC` may contain any object

Definition of `ProximityCheck`:

```kotlin
class ProximityCheck {
  
    /** The actual Time-based one time password */
    val totp: String
    
    /** Type of the Proximity check */
    val type: ProximityCheckType
    
    /** Timestamp when the operation was scanned (QR Code) or delivered to the device (Deeplink) */
    val timestampRequested: ZonedDateTime = ZonedDateTime.now()
}
```

ProximityCheckType types:

- `QR_CODE` TOTP was scanned from QR code
- `DEEPLINK` TOTP was delivered to the app via Deeplink

## Creating a Custom Operation

In some specific scenarios, you might need to approve or reject an operation that you received through a different channel than `getOperations`. In such cases, you can implement the `IOperation` interface in your custom class and then feed created objects to both `authorizeOperation` and `rejectOperation` methods.

<!-- begin box success -->
You can use the `LocalOperation` convenience class that implements the `IOperation` protocol.
<!-- end -->

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

    /** 
     * Additional information with proximity check data 
     */ 
    var proximityCheck: ProximityCheck?
}
```

## TOTP ProximityCheck

Two-Factor Authentication (2FA) using Time-Based One-Time Passwords (TOTP) in the Operations Service is facilitated through the use of ProximityCheck. This allows secure approval of operations through QR code scanning or deeplink handling.

- QR Code Flow:

When the `UserOperation` contains a `PreApprovalScreen.QR_SCAN`, the app should open the camera to scan the QR code before confirming the operation. Use the camera to scan the QR code containing the necessary data payload for the operation.

- Deeplink Flow:

When the app is launched via a deeplink, preserve the data from the deeplink and extract the relevant data. When operations are loaded compare the operation ID from the deeplink data to the operations within the app to find a match.

- Assign TOTP and Type to the Operation
  Once the QR code is scanned or match from the deeplink is found, create a `WMTProximityCheck` with:
  - `totp`: The actual Time-Based One-Time Password.
  - `type`: Set to `ProximityCheckType.QR_CODE` or `ProximityCheckType.DEEPLINK`.
  - `timestampRequested`: The timestamp when the QR code was scanned (by default, it is created as the current timestamp when the object is instantiated).

- Authorizing the ProximityCheck
  When authorizing, the SDK will by default add `timestampSigned` to the `ProximityCheck` object. This timestamp indicates when the operation was signed.

### PACUtils
- For convenience, utility class for parsing and extracting data from QR codes and deeplinks used in the PAC (Proximity Anti-fraud Check), is provided.
```kotlin
    /** Data payload which is returned from the parser */
    data class PACData(

        /** The ID of the operation associated with the TOTP */
        val operationId: String,

        /** The actual Time-based one time password */
        val totp: String?
    )
```

- two methods are provided:
  - `parseDeeplink(uri: Uri): PACData?` - uri is expected to be in format `"scheme://code=$JWT"` or `scheme://operation?oid=5b753d0d-d59a-49b7-bec4-eae258566dbb&potp=12345678}`
  - `parseQRCode(code: String): PACData?` - code is to be expected in the same format as deeplink formats or as a plain JWT
  - mentioned JWT should be in format `{“typ”:”JWT”, “alg”:”none”}.{“oid”:”5b753d0d-d59a-49b7-bec4-eae258566dbb”, “potp”:”12345678”} `

- Accepted formats:
  - notice that totp key in JWT and in query shall be `potp`!