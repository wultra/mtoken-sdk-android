# SDK Integration

## Requirements

- `minSdkVersion 23` (Android 6.0 Marshmallow)
- [PowerAuth Mobile SDK](https://github.com/wultra/powerauth-mobile-sdk) needs to be available in your project.

## Gradle

To use __WMT__ in your Android application, include the following dependency in your gradle file.

```groovy
repositories {
    mavenCentral() // if not defined elsewhere...
}

// Replace VERSION_DEFINITION with the actual library version.
implementation "com.wultra.android.mtokensdk:wultra-mtoken-sdk:VERSION_DEFINITION"
// if not added yet, include PowerAuth SDK too
implementation "com.wultra.android.powerauth:powerauth-sdk:X.Y.Z"
```

## PowerAuth SDK configuration

Configure biometric invalidation explicitly when creating the `PowerAuthSDK` instance. This links biometric items to the
current biometric set and invalidates the biometric factor when the enrolled biometric set changes.

```kotlin
val keychainConfig = PowerAuthKeychainConfiguration.Builder()
    .linkBiometricItemsToCurrentSet(true)
    .build()

val powerAuth = PowerAuthSDK.Builder(powerAuthConfig)
    .keychainConfiguration(keychainConfig)
    .build(appContext)
```

The SDK does not define application-level customer-demo environments. Applications that expose ENV4 as Customer Demo
must include only their OneID environment; digital onboarding remains part of custom environment configuration and
should be added only where needed.
