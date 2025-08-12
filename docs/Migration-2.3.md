# Migration from 2.2.x to 2.3.0

This guide explains how to migrate projects using **Wultra Mobile Token SDK for Android** from `2.2.x` to `2.3.x`.

The key change is a **move from ThreeTenABP (`org.threeten.bp`) to the platform `java.time` API**. ThreeTenABP has been discontinued, so we aligned the SDK with the modern, supported APIs.

Because we continue to support **minSdk 21**, **core library desugaring is required** in your app to use `java.time` on Android < 26.

---

## 1) Why this change?

- **ThreeTenABP is discontinued** and requires manual `Application` initialization that’s easy to miss.
- `java.time` is the standard Java 8+ time API and is built into Android **API 26+**. With **core library desugaring**, it also works on **API 21–25**.

---

## 2) What changed in the SDK

### 2.1 Time types moved to `java.time`
- All public date/time types now use **`java.time`** (e.g., `ZonedDateTime`, `Instant`, `ZoneId`).
- Example model:
  ```kotlin
  open class UserOperation(
      val created: ZonedDateTime,
      override val expires: ZonedDateTime,
      // ...
  )
  ```

### 2.2 New helper: synchronized timestamp for `ProximityCheck`
We added a convenient factory that prefers **server‑synchronized time** from PowerAuth and falls back to the device clock:
```kotlin
data class ProximityCheck(
    val totp: String,
    val type: ProximityCheckType,
    val timestampReceived: ZonedDateTime = ZonedDateTime.now()
) {
    companion object {
        /**
         * Creates a new instance using time synchronized with PowerAuth server, if available.
         * Falls back to system time when synchronization is not available.
         */
        fun withSynchronizedTime(
            totp: String,
            type: ProximityCheckType,
            powerAuthSDK: PowerAuthSDK
        ): ProximityCheck {
            val timeService = powerAuthSDK.timeSynchronizationService
            val currentDate = if (timeService.isTimeSynchronized) {
                ZonedDateTime.ofInstant(Instant.ofEpochMilli(timeService.currentTime), ZoneId.systemDefault())
            } else {
                ZonedDateTime.now()
            }
            return ProximityCheck(totp, type, currentDate)
        }
    }
}
```

**Why use it?**
- TOTP/proximity flows are **time‑sensitive**; relying on device time can cause drift‑related errors if the user’s clock is wrong.
- Using `withSynchronizedTime(...)` ensures the timestamp aligns with the server whenever possible.

**Recommended migration:**
- Wherever you previously constructed `ProximityCheck(totp, type)` or `ProximityCheck(totp, type, ZonedDateTime.now())`, switch to:
  ```kotlin
  val pc = ProximityCheck.withSynchronizedTime(totp, type, powerAuthSDK)
  ```
- You can still pass your own `timestampReceived` if you have a trusted time source.

### 2.3 Initialization
- **No more ThreeTenABP initialization** in your `Application`. Remove any `AndroidThreeTen.init(context)` or similar calls.

---

## 3) App changes you must do

Because your app likely has `minSdk < 26`, you must enable **core library desugaring** to use `java.time`.

### Gradle (KTS)

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

**Gradle (Groovy)**
```groovy
android {
  compileOptions {
    coreLibraryDesugaringEnabled true
  }
}

dependencies {
  coreLibraryDesugaring "com.android.tools:desugar_jdk_libs:2.1.5"
}
```

---

## 4) Code changes in your app

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

## 5) Serialization (Gson) notes

If you previously registered ThreeTen adapters, you can now use adapters for `java.time` (your project may already have a `ZonedDateTime` adapter). We handle our models internally, but if you serialize app-specific time fields, register your own Gson adapters for `java.time` as needed.

Example ISO adapter (simplified):
```kotlin
class ZonedDateTimeAdapter : JsonSerializer<ZonedDateTime>, JsonDeserializer<ZonedDateTime> {
    override fun serialize(src: ZonedDateTime?, type: Type?, ctx: JsonSerializationContext?): JsonElement =
        JsonPrimitive(src?.format(DateTimeFormatter.ISO_ZONED_DATE_TIME))

    override fun deserialize(json: JsonElement, type: Type, ctx: JsonDeserializationContext): ZonedDateTime {
        val s = json.asString
        // Handle "+HHmm" -> "+HH:mm" if your backend sends that:
        val fixed = s.replace(Regex("\+([0-9]{2})([0-9]{2})$"), "+$1:$2")
        return ZonedDateTime.parse(fixed, DateTimeFormatter.ISO_ZONED_DATE_TIME)
    }
}
```

---

## 6) Troubleshooting

- **Build fails with AAR metadata error about desugaring**  
  The app didn’t enable core library desugaring. Add:
  ```gradle
  coreLibraryDesugaringEnabled true
  coreLibraryDesugaring "com.android.tools:desugar_jdk_libs:2.1.5"
  ```

- **`NoClassDefFoundError: java.time.*` at runtime on Android 5–7**  
  Desugaring not enabled or the dependency missing. See Gradle snippet above.

- **Parsing ISO strings ending with `+0000`**  
  Use an adapter that normalizes `+HHmm` → `+HH:mm` before `ZonedDateTime.parse`.

---

## 7) Summary

- ThreeTenABP is discontinued → we switched to **`java.time`**.
- Prefer `ProximityCheck.withSynchronizedTime(...)` to reduce clock‑drift issues in time‑sensitive flows.
- Remove ThreeTenABP initialization and imports.
- Most API usage remains the same; update imports and any custom adapters you maintain.
