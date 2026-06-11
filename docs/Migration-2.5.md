# Migration from 2.4.x to 2.5.x (Android)

This guide provides instructions for migrating from **Wultra Mobile Token SDK for Android** version `2.4.x` to version `2.5.x`.

Version `2.5.x` integrates **PowerAuth Mobile SDK `2.0.0`**. This changes the offline (QR) authorization flow, the QR operation parser, and a few related APIs, and raises the minimum supported Android version. For the full list of PowerAuth changes, see the [PowerAuth Mobile SDK migration guide from 1.9 to 2.0](https://developers.wultra.com/components/powerauth-mobile-sdk/develop/documentation/Migration-from-1.9-to-2.0).

---

### Dependencies and Build Configuration

1. **Update PowerAuth dependencies** to `2.0.0`:

    ```kotlin
    implementation("com.wultra.android.powerauth:powerauth-sdk:2.0.0")
    implementation("com.wultra.android.powerauth:powerauth-networking:2.0.0")
    ```

2. **Raise `minSdkVersion` to at least `23`.** Support for API levels `21` and `22` (Android 5.0 / 5.1) has been dropped.

3. PowerAuth **"server stack" `2.0+`** is now required.

---

### Offline (QR) Authorization Is Now Asynchronous

`authorizeOfflineOperation` no longer returns the authentication code synchronously. It now reports the result through a callback and returns an `ICancelable`.

**Before (2.4.x):**

```kotlin
try {
    val signature = operationsService.authorizeOfflineOperation(operation, auth)
    // Display the signature to the user.
} catch (e: Exception) {
    // Failed to sign the operation.
}
```

**After (2.5.x):**

```kotlin
val cancelable = operationsService.authorizeOfflineOperation(operation, auth) { result ->
    result.onSuccess { signature ->
        // Display the signature to the user.
    }.onFailure { error ->
        // Failed to sign the operation.
    }
}
```

---

### QR Parsing and Signature Verification

`verifyServerSignedData` was removed from PowerAuth `2.0.0`. `QROperationParser` is now an instantiable class that can verify the operation signature for you when created with a `PowerAuthSDK` instance.

**Before (2.4.x):**

```kotlin
val operation = QROperationParser.parse(scannedCode)
val verified = powerAuthSDK.verifyServerSignedData(
    operation.signedData,
    operation.signature.signature,
    operation.signature.isMaster()
)
if (!verified) {
    throw IllegalArgumentException("Invalid offline operation")
}
```

**After (2.5.x) - recommended:**

```kotlin
// Parses and verifies the signature in one step.
// Throws QROperationParseException if parsing or verification fails.
val operation = QROperationParser(powerAuthSDK).parse(scannedCode)
```

If you need to parse without automatic verification, use the parameterless parser and verify manually:

```kotlin
val operation = QROperationParser.parse(scannedCode)
operation.verifySignature(powerAuthSDK) // throws on failure
```

<!-- begin box warning -->
Signature verification runs synchronously inside `parse`. Call the parser on a background thread to avoid blocking the UI.
<!-- end -->

---

### `QROperationSignature` Changes

The signature model was reworked. The old members are kept as deprecated aliases, but note that `SigningKey` is now `KeyType` (a different type):

| Removed / Deprecated | Replacement |
|---|---|
| `signingKey: SigningKey` | `keyType: KeyType` |
| `signature: ByteArray` | `data: ByteArray` |
| `signatureString: String` | `dataSource: String` |
| `isMaster()` | `keyType == QROperationSignature.KeyType.MASTER` |
| `SigningKey.fromTypeValue(...)` | `KeyType.fromTypeValue(...)` |

A new `KeyType.MAC_PERSONALIZED` value was added to support KMAC-based (symmetric) signatures.

---

### Error Handling

The parser now throws a structured **`QROperationParseException`** that carries a **`QRParseError`** reason. It extends `IllegalArgumentException`, so existing `catch (e: IllegalArgumentException)` blocks keep working. You can switch on `exception.reason` for finer-grained handling.

---

### OIDC Activation

`PowerAuthSDK.createOIDCActivation` no longer declares or throws `PowerAuthMissingConfigException`, which was removed in PowerAuth `2.0.0` (configuration is now validated by `PowerAuthConfiguration.Builder.build()`).

---

### Migration Checklist

- Update `powerauth-sdk` and `powerauth-networking` to `2.0.0`.
- Raise your app's `minSdkVersion` to **`23`**.
- Convert `authorizeOfflineOperation` calls to the **asynchronous callback** form.
- Replace `verifyServerSignedData` usage with `QROperationParser(powerAuthSDK).parse(...)` or `QROperation.verifySignature(...)`.
- Migrate deprecated `QROperationSignature` members (`signingKey`/`signature`/`signatureString`/`isMaster()`).
- Optionally handle `QROperationParseException.reason` for structured parse errors.

---
