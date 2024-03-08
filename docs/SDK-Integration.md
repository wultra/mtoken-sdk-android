# SDK Integration

## Requirements

- `minSdkVersion 21` (Android 5.0 Lollipop)
- [PowerAuth Mobile SDK](https://github.com/wultra/powerauth-mobile-sdk) needs to be available in your project.

## Gradle

To use __WMT__ in your Android application include the following dependency to your gradle file.

```groovy
repositories {
    mavenCentral() // if not defined elsewhere...
}

implementation "com.wultra.android.mtokensdk:wultra-mtoken-sdk:1.x.y"
// if not added yet, include PowerAuth SDK too
implementation "com.wultra.android.powerauth:powerauth-sdk:1.x.y"
```

## PowerAuth Compatibility

| WMT SDK            | PowerAuth SDK |  
|--------------------|---|
| `1.0.x` - `1.2.x`  | `1.0.x` - `1.5.x` |
| `1.3.x` - `1.4.x`  | `1.6.x` |
| `1.5.x` - `1.7.x`  | `1.7.x` |
| `1.8.x` - `1.10.x` | `1.8.x` |


