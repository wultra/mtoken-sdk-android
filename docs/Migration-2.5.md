# Migration from 2.4.x to 2.5.x (Android)

This guide provides instructions for migrating from **Wultra Mobile Token SDK for Android** version `2.4.x` to version `2.5.x`.

Version `2.5.x` improves proximity check time handling. The public API changes are minimal — primarily deprecations — but `ProximityCheck`'s data class structure changed (see [`timestampReceived` Changes](#timestampreceived-changes)).

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

### `timestampReceived` Changes

The `timestampReceived` property moved from the `data class` primary constructor to a body property with `internal set`. It remains read-only from outside the SDK. The SDK now adjusts this value internally during `authorizeOperation` to align with server time.

Because `timestampReceived` is no longer a constructor parameter, it no longer participates in the generated `equals()`, `hashCode()`, `toString()`, `copy()`, or `componentN()` methods. Specifically:

- `copy(timestampReceived = ...)` **will not compile** — `timestampReceived` is no longer a `copy()` parameter.
- Destructuring `val (totp, type, ts) = proximityCheck` **will not compile** — only two components are generated.
- `equals()`/`hashCode()` now compare only `totp` and `type`.

In practice this is unlikely to affect consumers — `ProximityCheck` is a short-lived object created, assigned to an operation, and authorized.

---
