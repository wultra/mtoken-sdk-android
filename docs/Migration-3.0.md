# Migration from 2.4.x to 3.0.x (Android)

This guide provides instructions for migrating from **Wultra Mobile Token SDK for Android** version `2.4.x` to version `3.0.x`.

Version `3.0.x` integrates [PowerAuth Mobile SDK 2.0](https://developers.wultra.com/components/powerauth-mobile-sdk/develop/documentation/Migration-from-1.9-to-2.0). PowerAuth 2.0 brings post-quantum cryptography readiness with stronger cryptographic algorithms. Most of the migration work happens in your application's PowerAuth integration; the public Mobile Token SDK API itself is largely source-compatible.

## Updated Dependencies

| Dependency | Old | New |
|---|---|---|
| `powerauth-sdk` | `1.9.x` | `2.0.x` |
| `powerauth-networking` | `1.x` | `2.0.x` |

### Gradle

Update your dependency versions:

```kotlin
implementation("com.wultra.android.powerauth:powerauth-sdk:2.0.0")
implementation("com.wultra.android.powerauth:powerauth-networking:2.0.0")
implementation("com.wultra.android.mtokensdk:wultra-mtoken-sdk:3.0.0")
```

### Minimum SDK

PowerAuth 2.0 requires **minSdk 23** (Android 6.0). Your application must target at least API level 23.

## Source-Code Migration

Most of the Mobile Token SDK's public Kotlin API is unchanged. You need to adjust the parts of your application that interact directly with `PowerAuthSDK` (refer to the upstream [PowerAuth Mobile SDK 2.0 migration guide](https://developers.wultra.com/components/powerauth-mobile-sdk/develop/documentation/Migration-from-1.9-to-2.0) for the full list), and the QR-operation symbols described below.

### QR Operation Signature

The `QROperationSignature` type has been reworked to support post-quantum-ready offline signatures (KMAC-based MAC signatures) alongside the existing ECDSA-based master and personalized signatures.

| Old (2.4.x) | New (3.0.x) |
|---|---|
| `QROperationSignature.SigningKey` | `QROperationSignature.KeyType` |
| `signature.signingKey` | `signature.keyType` |
| `signature.signatureString: String` (Base64) | `signature.data: ByteArray` (raw) |
| _n/a_ | `KeyType.MAC_PERSONALIZED` (32-byte MAC) |
| _n/a_ | `QROperation.verifySignature(powerAuth)` |
| _n/a_ | `QROperationParser(powerAuth)` constructor |

The old properties (`signingKey`, `signatureString`, `signature`) are still available as deprecated shims for source compatibility.

#### Automatic Verification (Recommended)

The recommended way to verify a parsed QR operation is to pass a `PowerAuthSDK` instance to the parser during construction. The parser then verifies the signature automatically and throws `IllegalArgumentException` if it is invalid:

```kotlin
// After (3.0.x) — recommended: automatic verification during parsing
val parser = QROperationParser(powerAuth)
val op = parser.parse(code)
// signature is already verified at this point
```

#### Manual Verification

You can also verify manually using the operation helper:

```kotlin
// Before (2.4.x)
val isValid = powerAuth.verifyServerSignedData(op.signedData, op.signature.signatureString.toByteArray(), op.signature.signingKey == SigningKey.MASTER)

// After (3.0.x) — manual verification
op.verifySignature(powerAuth)
// throws if signature is invalid
```

If you need lower-level access, use `signature.data` together with `signature.keyType.powerAuthKeyId`, which maps each `KeyType` to the appropriate `CoreSignatureKeyId` constant (`MASTER_EC`, `SERVER_EC`, or `MAC_PERSONALIZED`).

### QR Operation Parser File Rename

The `QROperationParser` source file has been renamed (removed trailing space). If you reference it directly in build scripts or documentation, update accordingly.

### Networking Endpoint Types

The underlying networking library will rename endpoint types in its 2.0 release:

| Old (deprecated) | New |
|---|---|
| `EndpointSignedWithToken` | `EndpointAuthenticatedWithToken` |
| `EndpointSigned` | `EndpointAuthenticated` |

The old names will continue to compile (as typealiases) but will emit deprecation warnings. If you subclass or extend the SDK's API layer, update your endpoint declarations once you upgrade to networking library 2.0.

