# SDK integration

## Requirements

- `minSdkVersion 16` (Android 4.1 Jelly Bean)
- [PowerAuth Mobile SDK](https://github.com/wultra/powerauth-mobile-sdk) needs to be available in your project.

## Gradle

To use __WMT__ in your Android application include the following dependency to your gradle file.

```groovy
implementation "com.wultra.android.mtokensdk:wultra-mtoken-sdk:1.0.0"
// if not added yet, include PowerAuth SDK too
implementation "com.wultra.android.powerauth:powerauth-sdk:1.0.0"
```

_Note that this documentation is using version `1.0.0` as an example._

## PowerAuth compatibility

| WMT SDK | PowerAuth SDK |  
|---|---|
| `1.0.x` - `1.2.x` | `1.0.x` - `1.5.x` |
| `1.3.x` | `1.6.x` |
