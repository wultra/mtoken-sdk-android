# Using Operations Service

<!-- begin remove -->
- [Introduction](#introduction)
- [Creating an Instance](#creating-an-instance)
- [Retrieve Pending Operations](#retrieve-pending-operations)
- [Start Periodic Polling](#start-periodic-polling)
- [Approve an Operation](#approve-an-operation)
- [Reject an Operation](#reject-an-operation)
- [Mobile Token Data](#mobile-token-data)
- [Operation detail](#operation-detail)
- [Claim the Operation](#claim-the-operation)
- [Off-line Authorization](#off-line-authorization)
- [Operations API Reference](#operations-api-reference)
- [UserOperation](#useroperation)
- [ProximityCheck](#proximitycheck)
- [Creating a Custom Operation](#creating-a-custom-operation)

## Introduction
<!-- end -->

The Operations Service is responsible for fetching the operation list and for approving or rejecting operations.

An operation can be anything you need to be approved or rejected by the user. It can be for example money transfer, login request, access approval, ...

<!-- begin box warning -->
Note: Before using Operations Service, you need to have a `PowerAuthSDK` object available and initialized with a valid activation. Without a valid PowerAuth activation, all endpoints will return an error.
<!-- end -->

Operations Service communicates with the [Mobile Token API](https://developers.wultra.com/components/enrollment-server/develop/documentation/Mobile-Token-API).

## Creating an Instance

The preferred way of instantiating Operations Service is via `WultraMobileToken` class.
See: [Example Usage](./Example-Usage)


### Customized initialization

In case you need to create more customized instance. You can do so with an initializer.


```kotlin
val operationsService = OperationsService(
    powerAuthSDK,
    appContext,
    httpClient,
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
- `baseURL` - address, where your operations server can be reached (ending with `/enrollment-server` in the default setup)


__Optional parameters:__

For these, if null is provided, default internal implementation is provided.

- `tokenProvider` - Provider that provides a valid PowerAuth token from token store for api communication.
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

After you retrieve the pending operations, you can render them in the UI, for example, as a list of items with a detail of the operation shown after a tap.

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

### Default and Minimum TimeInterval Enforcement

For convenience, there is a default implementation where you can omit the polling interval and it is automatically set to 7 seconds. If you specify an interval below 5 seconds, it will be automatically adjusted to 5 seconds to prevent server overload.

### Setting up a listener

To receive the result of the polling, set up a listener.

<!-- begin box warning -->
Note that the listener is called for all "fetch operations" requests (not just the polling).
<!-- end -->

```kotlin
operationsService.listener = object: IOperationsServiceListener {
    override fun operationsFailed(error: ApiError) {
        // show UI the last fetch has failed
    }

    override fun operationsChanged(operations: List<UserOperation>, removed: List<UserOperation>, added: List<UserOperation>) {
        // update UI with the latest operation list based on the last call
    }

    override fun operationsLoading(loading: Boolean) {
        // show loading UI
    }
}
```

### Best Practices and Recommendations

For optimal server performance, consider adjusting polling intervals based on your application's requirements. For instance, when push notifications are enabled, it's advisable to double the polling interval to minimize server load.

## Approve an Operation

To approve an operation use `IOperationsService.authorizeOperation`. You can simply use it with the following examples:

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

To approve offline operations with biometrics, your PowerAuth instance [needs to be configured with biometric factor](https://github.com/wultra/powerauth-mobile-sdk/blob/develop/docs/PowerAuth-SDK-for-Android.md#biometric-authentication-setup).

```kotlin
// Approve operation with biometrics
fun approveWithBiometrics(operation: IOperation) {

    // UserOperation contains information if biometrics can be used
    if (operation is UserOperation) {
        if (!operation.allowedSignatureType.factors.contains(AllowedSignatureType.Factor.POSSESSION_BIOMETRY)) {
            return
        }
    }

    this.powerAuthSDK.authenticateUsingBiometrics(appContext, fragmentManager,
        "Operation approval",
        "Use biometrics to approve the operation",
        object : IBiometricAuthenticationCallback {

            override fun onBiometricDialogSuccess(biometricKeyData: BiometricKeyData) {
                val auth = PowerAuthAuthentication.possessionWithBiometrics(biometricKeyData.derivedData)
                this.operationsService.authorizeOperation(operation, auth) {
                    it.onSuccess {
                        // show success UI
                    }.onFailure {
                        // show error UI
                    }
                }
            }

            override fun onBiometricDialogCancelled(userCancel: Boolean) {
                // the biometrics dialog was canceled
            }

            override fun onBiometricDialogFailed(error: PowerAuthErrorException) {
                // biometrics authentication failed
            }
        }
    )
}
```

## Reject an Operation

To reject an operation use `IOperationsService.rejectOperation`. Operation rejection is confirmed by the possession factor so there is no need for creating  `PowerAuthAuthentication ` object. You can simply use it with the following example.

```kotlin
// Reject operation with some reason
fun reject(operation: IOperation, reason: RejectionData) {
    this.operationsService.rejectOperation(operation, reason) {
        it.onSuccess {
            // show success UI
        }.onFailure {
            // show error UI
        }
    }
}
```

## Mobile Token Data

With PowerAuth Server **1.10+**, you can pass additional, customer-specific metadata during operation authorization using the `mobileTokenData` property.
Since PowerAuth Server **2.0+** you can pass additional mobileTokenData to reject method as well.

This feature is especially useful for **fraud detection systems (FDS)**, customer risk evaluation, or other backend-specific business logic.

You can provide this data in two ways:

---

### Direct Map Approach

If you already have a static set of key–value pairs to attach, you can directly construct a `Map<String, Any>` and assign it to your operation:

```kotlin
// Example: directly attaching a static map of metadata
val fdsData = mapOf(
    "deviceFingerprint" to "abc123def456",
    "riskScore" to 0.8,
    "location" to mapOf(
        "latitude" to 50.0755,
        "longitude" to 14.4378
    )
)

val operation = CustomOperation(
    id = "operationId123",
    data = "operationData",
    mobileTokenData = fdsData
)

val auth = PowerAuthAuthentication.possessionWithPassword("password123")

operationsService.authorizeOperation(operation, auth) { result ->
    result.onSuccess {
        // Operation approved successfully
    }.onFailure {
        // Handle network or SDK error
    }
}
```

---

### Builder-Based Approach

For **more dynamic, structured, or multi-step** data, use the helper `MobileTokenData.Builder`.

The builder provides a safe, thread-synchronized API for collecting and organizing data before finalizing it into a map for submission.

### MobileTokenData Builder

The `MobileTokenData.Builder` helps you safely compose structured data for an operation before it’s approved or rejected.

- **Initialize** it with optional `initialData` map.
- **Add generic entries** using `put(key, value)`.
- **Attach structured records** such as `PreApprovalScreensRecorder` using `put(record)`.
- **Extend** it with your own record types by implementing the `MobileTokenDataRecord` interface.

#### Example

```kotlin
// Optional initial data entries (e.g. FDS hints)
val initialData = mapOf("deviceFingerprint" to "abc123")

// Create the builder
val builder = MobileTokenData.Builder(initialData)

// Add generic entries
builder.put("riskScore", 0.82)

// Assign to the operation
operation.mobileTokenData = builder.build()
```

---

## Record Helpers

Sometimes, additional data attached to `mobileTokenData` is not just a few key–value pairs.
It can represent **structured sections of information** (for example, a timeline of user actions or device events).

To support these cases, the SDK defines the **`MobileTokenDataRecord` interface**.

#### The `MobileTokenDataRecord` Interface

A `MobileTokenDataRecord` represents a single top-level entry in the final `mobileTokenData` map.  
It defines **two key responsibilities**:

1. Provide a stable `key` — the top-level field name under which your record will appear.
2. Implement `build()` — a method that returns the **value** (any serializable object) for that key.

```kotlin
interface MobileTokenDataRecord {
    /** Top-level key under which this record is stored. */
    val key: String

    /** Produces the value object to store for this key. */
    fun build(): Any
}
```

This lets you encapsulate structured or time-dependent data, keep your `mobileTokenData` composition organized, and reuse record instances when needed.

---

### Example: Custom Record

You can implement your own record for any structured data section — for example, to log environment variables or app configuration info.

```kotlin
class CustomRecord : MobileTokenDataRecord {
    override val key = "customSection"
    private val data = mutableMapOf<String, Any>()

    fun add(name: String, value: Any) = apply { data[name] = value }

    override fun build(): Any = data // return a value snapshot
}
```

Usage example:

```kotlin
val builder = MobileTokenData.Builder()
val record = CustomRecord()
    .add("flag", true)
    .add("mode", "debug")

// Either pass the whole record…
builder.put(record)
// …or manually by key/value
// builder.put(record.key, record.build())

operation.mobileTokenData = builder.build()
```

---

#### Predefined Record Helper: `PreApprovalScreensRecorder`

The SDK includes a predefined implementation, `PreApprovalScreensRecorder`, which records how users navigate through **Pre-Approval screens**.

Each recorded “visit” contains:

- Screen identifier (`screen`)
- Opening timestamp
- Closing timestamp
- User action (`CONTINUE`, `CLOSE`, `REJECT`, `SCAN`, etc.)

The `PreApprovalScreensRecorder` exposes few methods for recording the user flow:
 
 - `begin(id: String)` – starts a new visit for the given screen ID.
If another visit is already open, it is automatically added to the list (without a closing timestamp or action).
 - `end(id: String, action: Action)` – closes the current visit if the given id matches.
If no visit is open, but the most recent recorded visit has the same id and is still unclosed, it is finalized instead.
 - `reset()` - resets recorded visits

Timestamps are aligned with server time via `PowerAuthSDK.timeSynchronizationService`.

```kotlin
// Create MobileTokenData.Builder instance
val builder = MobileTokenData.Builder()

// The PowerAuthSDK instance provides a timeSynchronizationService used
// to create accurate, server-aligned timestamps for each recorded event.
val screenRecorder = PreApprovalScreensRecorder(powerAuthSDK)

// Display UI for the PreApproval screen and record that it was shown
screenRecorder.begin(screen.id)
// Record when user leaves the PreApproval screen
screenRecorder.end(screen.id, PreApprovalScreensRecorder.Action.CONTINUE)

// ... repeat for all the screens from the operations.ui.preApprovalScreens list

// When your PreApproval flow is finished pass the WMTPreApprovalScreensRecorder to the WMTMobileTokenData.Builder    
builder.put(screenRecorder)

// Assign created MobileTokenData to the Operation before approving/rejecting
operation.mobileTokenData = builder.build()
```

The `mobileTokenData` is completely optional and the structure is customer-specific. If you don't need this functionality, you can continue using operations without providing this property.

---

## Operation detail

To get a detail of the operation based on operation ID use `IOperationsService.getDetail`. Operation detail is confirmed by the possession factor so there is no need for creating a `PowerAuthAuthentication` object. The returned result is the operation and its current status.

```kotlin
// Retrieve operation details based on the operation ID.
fun getDetail(operationId: String) {
    this.operationService.getDetail(operationId: operationId) {
        it.onSuccess {
            // process operation
        }.onFailure {
            // show error UI
        }
    }
}
```

## Claim the Operation

To claim a non-persolized operation use `IOperationsService.claim`. 

A non-personalized operation refers to an operation that is initiated without a specific userId. In this state, the operation is not tied to a particular user. 

Operation claim is confirmed by the possession factor so there is no need for creating a `PowerAuthAuthentication` object. The returned result is the operation and its current status. You can simply use it with the following example.

```kotlin
// Assigns the 'non-personalized' operation to the user
fun claim(operationId: String) {
    this.operationService.claim(operationId: operationId) { 
        it.onSuccess { 
            // process operation 
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

In case the user is not online, you can use off-line authorizations. In this operation mode, the user needs to scan a QR code, enter a PIN code, or use biometrics, and rewrite the resulting code. Wultra provides a special format for [the operation QR codes](https://github.com/wultra/enrollment-server/blob/develop/docs/Offline-Signatures-QR-Code.md), which are automatically processed with the SDK.

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
An offline operation needs to be __always__ approved with __a 2-factor scheme__ (password or biometrics).
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

#### With Biometrics

To approve offline operations with biometrics, your PowerAuth instance [needs to be configured with biometric factor](https://github.com/wultra/powerauth-mobile-sdk/blob/develop/docs/PowerAuth-SDK-for-Android.md#biometric-authentication-setup).

To determine if biometrics can be used for offline operation authorization, use `QROperation.flags.biometricsAllowed`.

```kotlin
// Approves QR operation with biometrics
fun approveQROperationWithBiometrics(operation: QROperation, appContext: Context, fragmentManager: FragmentManager) {

    if (!operation.flags.biometricsAllowed) {
        // biometrics usage is not allowed on this operation
        return
    }

    this.powerAuthSDK.authenticateUsingBiometrics(appContext, fragmentManager,
        "Operation approval",
        "Use biometrics to approve the operation",
        object : IBiometricAuthenticationCallback {

            override fun onBiometricDialogSuccess(biometricKeyData: BiometricKeyData) {
                val auth = PowerAuthAuthentication.possessionWithBiometrics(biometricKeyData.derivedData)
                try {
                    val offlineSignature = operationsService.authorizeOfflineOperation(operation, auth)
                    // Display the signature to the user so it can be manually rewritten.
                } catch (e: Exception) {
                    // Failed to sign the operation
                }
            }

            override fun onBiometricDialogCancelled(userCancel: Boolean) {
                // the biometrics dialog was canceled
            }

            override fun onBiometricDialogFailed(error: PowerAuthErrorException) {
                // biometrics authentication failed
            }
        }
    )
}
```

## Operations API Reference

All available methods and attributes of `IOperationsService` API are:

- `listener` - Listener object that receives info about operation loading.
- `acceptLanguage` - Language settings, that will be sent along with each request. The server will return properly localized content based on this value. Value follows standard RFC [Accept-Language](https://tools.ietf.org/html/rfc7231#section-5.3.5)
- `lastFetchResult` - Last result of getOperations call.
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
- `rejectOperation(operation: IOperation, reason: RejectionData, callback: (result: Result<Unit>) -> Unit)` - Reject provided operation.
  - `operation` - An operation to reject, retrieved from `getOperations` call or [created locally](#creating-a-custom-operation).
  - `reason` - Rejection reason.
  - `callback` - Called when rejection request finishes.
- `fun authorizeOfflineOperation(operation: QROperation, authentication: PowerAuthAuthentication, uriId: String)` - Sign offline (QR) operation
  - `operation` - Offline operation retrieved via `QROperationParser.parse` method.
  - `authentication` - PowerAuth authentication object for operation signing.
  - `uriId` - Custom signature URI ID of the operation. Use the URI ID under which the operation was created on the server. The default value is `/operation/authorize/offline`.

## UserOperation

Operations objects retrieved through the `getOperations` API method (like the `getOperations` method in `IOperationsService`) are called "user operations".

Under this abstract name, you can imagine for example "Login operation", which is a request for signing in to the online account in a web browser on another device. **In general, it can be any operation that can be either approved or rejected by the user.**

Visually, the operation should be displayed as an info page with all the attributes (rows) of such an operation, where the user can decide if he wants to approve or reject it.

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
     * the user may use the biometrics, or if a password is required.
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


    /**
     *  Enum-like reason why the status has changed.
     *
     *  Max 32 characters are expected. Possible values depend on the backend implementation and configuration.
     */
    val statusReason: String?
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
     *   Texts for the result of the operation
     *   
     *   This includes messages for different outcomes of the operation such as success, rejection, and failure.
     */
    val resultTexts: ResultTexts?

    /**
     * Other attributes. 
     * 
     * Each attribute presents one line in the UI. Attributes are differentiated by `type` property
     * and specific classes such as NoteAttribute or AmountAttribute.
     */
    val attributes: List<Attribute>
}
```

Definition of `ResultTexts`:

```kotlin
class ResultTexts(
    /** Optional message to be displayed when the approval of the operation is successful. */
    val success: String?,

    /** Optional message to be displayed when the operation approval or rejection fails. */
    val failure: String?,

    /** Optional message to be displayed when the operation is rejected. */
    val reject: String?
)
```

Attributes types:  
- `AMOUNT` like "100.00 CZK"  
- `KEY_VALUE` any key-value pair  
- `NOTE` just like `KEY_VALUE`, emphasizing that the value is a note or message  
- `HEADING` single highlighted text, written in a larger font, used as a section heading  
- `PARTY_INFO` providing structured information about third-party data (for example known e-shop)  
- `AMOUNT_CONVERSION` provides data about Money conversion  
- `IMAGE` image row  
- `ALERT` view to display success, info, warning or error message
- `UNKNOWN` fallback option when an unknown attribute type is passed. Such an attribute only contains the label.  

Definition of `OperationUIData`:

```kotlin
class OperationUIData {
    /** Confirm and Reject buttons should be flipped both in position and style */
    val flipButtons: Boolean?
    
    /** Block approval when on call (for example when on a phone or Skype call) */
    val blockApprovalOnCall: Boolean?
    
    /** UI for multiple pre-approval screens */
    val preApprovalScreens: List<PreApprovalScreen>?

    /**
     * UI for post-approval operation screen
     * 
     * Type of PostApprovalScreen is presented with different classes (Starting with `PostApprovalScreen*`)
     */
    val postApprovalScreen: PostApprovalScreen?
}
```

#### PreApprovalScreens:

Pre-approval screens define additional UI that can be displayed before the user decides to approve or reject an operation. They allow to display structured instructions, warnings, or interactive elements to the user.

Types:

- `WARNING`
- `INFO`
- `QR_SCAN` this type indicates that the `WMTProximityCheck` must be used
- `UNKNOWN` 

A pre-approval screen can contain the following building blocks:

- Heading and message – textual content displayed at the top of the screen.
- Optional metadata
  - id - unique identifier) 
  - backButton - show navigation back button
  - image - in-app asset identifier
- Elements – structured items that form the main content of the screen:
  - List item – text with optional icon with style (INFO, WARNING, DANGER).
  - Alert – highlighted box with style (INFO, WARNING, DANGER).
  - Button – action element with LINK, MAIL, or PHONE.
- Controls – configuration of approve/decline actions:
  - Decline – BACK or REJECT, with optional text. 
  - Approve – SLIDER or BUTTON, with optional text and optional countdown (counter). 
  - Layout options – axis (HORIZONTAL or VERTICAL) and flip (swap order of controls).

#### PostApprovalScreen:
`WMTPostApprovalScreen*` classes commonly contain `heading` and `message` and different payload data

Types:

- `REVIEW` provides an array of operations attributes with data: type, id, label, and note
- `REDIRECT` providing text for button, countdown, and redirection URL
- `GENERIC` may contain any object

Definition of `ProximityCheck`:

```kotlin
class ProximityCheck {
  
    /** The actual Time-based one-time password */
    val totp: String
    
    /** Type of the Proximity check */
    val type: ProximityCheckType
    
    /** Timestamp when the operation was scanned (QR Code) or delivered to the device (Deeplink) */
    val timestampReceived: ZonedDateTime = ZonedDateTime.now()
}
```

ProximityCheckType types:

- `QR_CODE` TOTP was scanned from the QR code
- `DEEPLINK` TOTP was delivered to the app via Deeplink


## TOTP ProximityCheck

Two-Factor Authentication (2FA) using Time-Based One-Time Passwords (TOTP) in the Operations Service is facilitated through the use of ProximityCheck. This allows secure approval of operations through QR code scanning or deeplink handling.

- QR Code Flow:

When the `UserOperation` contains a `PreApprovalScreen.QR_SCAN`, the app should open the camera to scan the QR code before confirming the operation. Use the camera to scan the QR code containing the necessary data payload for the operation.

- Deeplink Flow:

When the app is launched via a deeplink, preserve the data from the deeplink and extract the relevant data. When operations are loaded compare the operation ID from the deeplink data to the operations within the app to find a match.

- Assign TOTP and Type to the Operation
  Once the QR code is scanned or a match from the deeplink is found, create a `ProximityCheck` with:
  - `totp`: The actual Time-Based One-Time Password.
  - `type`: Set to `ProximityCheckType.QR_CODE` or `ProximityCheckType.DEEPLINK`.
  - `timestampReceived`: The timestamp when the QR code was scanned (by default, it is created as the current timestamp when the object is instantiated).

- Authorizing the ProximityCheck
  When authorizing, the SDK will by default add `timestampSent` to the `ProximityCheck` object. This timestamp indicates when the operation was sent.

### PACUtils

- For convenience, a utility class for parsing and extracting data from QR codes and deeplinks used in the PAC (Proximity Anti-fraud Check), is provided.

```kotlin
/** Data payload which is returned from the parser */
data class PACData(

    /** The ID of the operation associated with the TOTP */
    val operationId: String,

    /** The actual Time-based one-time password */
    val totp: String?
)
```

- two methods are provided:
  - `parseDeeplink(uri: Uri): PACData?` - URI is expected to be in the format `scheme://code=$JWT` or `scheme://operation?oid=5b753d0d-d59a-49b7-bec4-eae258566dbb&potp=12345678`
  - `parseQRCode(code: String): PACData?` - code is to be expected in the same format as deeplink formats or as a plain JWT
  - mentioned JWT should be in the format `{"type":"JWT", "alg":"none"}.{"oid":"5b753d0d-d59a-49b7-bec4-eae258566dbb", "potp":"12345678"}`

- Accepted formats:
  - notice that the totp key in JWT and in query shall be `potp`!

#### Creating a ProximityCheck with Server-Synchronized Time

When handling an operation with a required ProximityCheck, you will need to send a ProximityCheck along with your authorization request.

The SDK provides a factory method:

```kotlin
val proximityCheck = ProximityCheck.withSynchronizedTime(
    totp = "123456",
    type = ProximityCheckType.QR_CODE,
    powerAuthSDK = powerAuth
)
operation.proximityCheck = proximityCheck

this.operationsService.authorizeOperation(operation, auth) { result ->
    result.onSuccess {
        // Operation approved successfully
    }.onFailure { error ->
        // Handle error
    }
}
```

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
