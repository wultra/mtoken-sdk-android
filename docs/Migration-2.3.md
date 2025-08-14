# Migration from 2.2.x to 2.3.0

This guide explains how to migrate projects using **Wultra Mobile Token SDK for Android** from `2.2.x` to `2.3.x`.

The key change is a **move from ThreeTenABP (`org.threeten.bp`) to the platform `java.time` API**. ThreeTenABP has been discontinued, so we aligned the SDK with the modern, supported APIs.

Because we continue to support **minSdk 21**, **core library desugaring is required** in your app.

---

## 1) Why this change?

- **ThreeTenABP is discontinued** and requires manual `Application` initialization that’s easy to miss.
- `java.time` is the standard Java 8+ time API and is built into Android **API 26+**. With **core library desugaring**, it also works on **API 21–25**.

---

## 2) What changed in the SDK

### 2.1 Time types moved to `java.time`
- All public date/time types now use **`java.time`** (e.g., `ZonedDateTime`, `Instant`, `ZoneId`).


### 2.2 New helper: synchronized timestamp for `ProximityCheck`
You can now use the static factory [ProximityCheck.withSynchronizedTime](https://github.com/wultra/mtoken-sdk-android/blob/8ce306f551589c0666226af2e251f609c5c14860/library/src/main/java/com/wultra/android/mtokensdk/api/operation/model/UserOperation.kt#L253-L266) to create a ProximityCheck instance with a timestamp that prefers **server-synchronized** time from PowerAuth, falling back to the device clock if synchronization is not available.

### 2.3 Initialization
- **No more ThreeTenABP initialization** in your `Application`. Remove any `AndroidThreeTen.init(context)` or similar calls.

---

## 3) App changes you must do

Because this SDK now uses java.time while supporting minSdk 21, you must enable **core library desugaring** to compile and run.

### Gradle (kts)

**app/build.gradle.kts**

```kotlin
android {
    compileOptions {
        // Enable desugaring of core library APIs (java.time, streams, etc.)
        isCoreLibraryDesugaringEnabled = true
    }
}

dependencies {
    // AGP 7.4+ supports desugar_jdk_libs:2.x
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
}
```


### Replace imports
Search & replace in your codebase:

| Old (ThreeTen)               | New (java.time)        |
|-----------------------------|------------------------|
| `org.threeten.bp.Instant`   | `java.time.Instant`    |
| `org.threeten.bp.ZoneId`    | `java.time.ZoneId`     |
| `org.threeten.bp.ZonedDateTime` | `java.time.ZonedDateTime` |
| `org.threeten.bp.Duration`  | `java.time.Duration`   |
| `org.threeten.bp.format.DateTimeFormatter` | `java.time.format.DateTimeFormatter` |

Most APIs are 1:1 compatible:

```kotlin
// ThreeTenABP
val zdt = org.threeten.bp.ZonedDateTime.now()

// java.time
val zdt = java.time.ZonedDateTime.now()
```

### Remove ThreeTenABP init
Delete any `AndroidThreeTen.init(appContext)` or similar in `Application`.

---

## 4) Summary

- ThreeTenABP is discontinued → we switched to **`java.time`**.
- Prefer `ProximityCheck.withSynchronizedTime(...)` to reduce clock‑drift issues in time‑sensitive flows.
- Remove ThreeTenABP initialization and imports.
- Most API usage remains the same; update imports and any custom adapters you maintain.
