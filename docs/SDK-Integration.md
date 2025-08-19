# SDK Integration

## Requirements

- `minSdkVersion 21` (Android 5.0 Lollipop)
- [PowerAuth Mobile SDK](https://github.com/wultra/powerauth-mobile-sdk) needs to be available in your project.

## Gradle

To use __WMT__ in your Android application, include the following dependency in your gradle file.

```groovy
repositories {
    mavenCentral() // if not defined elsewhere...
}

implementation "com.wultra.android.mtokensdk:wultra-mtoken-sdk:2.2.1"
// if not added yet, include PowerAuth SDK too
implementation "com.wultra.android.powerauth:powerauth-sdk:X.Y.Z"
```

## PowerAuth Compatibility

| WMT SDK            | PowerAuth SDK     |  
|--------------------|-------------------|
| `2.0.x` - `2.3.x`  | `1.9.x`           |
| `1.12.x`           | `1.9.x`           |
| `1.8.x` - `1.11.x` | `1.8.x`           |
| `1.5.x` - `1.7.x`  | `1.7.x`           |
| `1.3.x` - `1.4.x`  | `1.6.x`           |
| `1.0.x` - `1.2.x`  | `1.0.x` - `1.5.x` |


