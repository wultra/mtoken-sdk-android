# Migration from 2.4.x to 2.5.x (Android)

This guide provides instructions for migrating from **Wultra Mobile Token SDK for Android** version `2.4.x` to version `2.5.x`.

Version `2.5.x` improves proximity check time handling. There are **no breaking changes** — only deprecations.

---

### Proximity Check Time Synchronization (Improved)

The SDK now automatically synchronizes timestamps with the PowerAuth server when authorizing an operation with a proximity check. This eliminates issues that could occur when the device's system time is incorrect (e.g., manually changed by the user) and the proximity check is created before time synchronization completes.

**Before (2.4.x):**

You were responsible for providing a server-synchronized timestamp, typically using `withSynchronizedTime`:

```kotlin
val proximityCheck = ProximityCheck.withSynchronizedTime(
    totp = totp,
    type = ProximityCheckType.QR_CODE,
    powerAuthSDK = powerAuth
)
operation.proximityCheck = proximityCheck
```

**After (2.5.x):**

Simply create the proximity check with the TOTP and type. The SDK handles time synchronization internally during `authorizeOperation`:

```kotlin
operation.proximityCheck = ProximityCheck(totp = totp, type = ProximityCheckType.QR_CODE)
```

---

### Deprecated APIs

| Deprecated | Replacement |
|---|---|
| `ProximityCheck.withSynchronizedTime(totp, type, powerAuthSDK)` | `ProximityCheck(totp, type)` |
| `ProximityCheck(totp, type, timestampReceived)` | `ProximityCheck(totp, type)` |

The deprecated APIs continue to work but their behavior has changed. The `timestampReceived` parameter in `ProximityCheck(totp, type, timestampReceived)` is now **ignored** — the SDK always captures `ZonedDateTime.now()` at creation and adjusts it to server time during `authorizeOperation`. Custom timestamps are not supported because the SDK can only correct the system clock offset, not arbitrary values provided by the consumer.

---

### `timestampReceived` Access Level

The `timestampReceived` property moved from the `data class` primary constructor to a body property with `internal set`. It remains read-only from outside the SDK. The SDK now adjusts this value internally during `authorizeOperation` to align with server time.

---
