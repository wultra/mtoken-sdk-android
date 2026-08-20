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
