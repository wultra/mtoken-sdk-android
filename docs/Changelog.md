# Changelog

## X.X.X
- Integrated `PowerAuthSDK` `2.0.0` [(#234)](https://github.com/wultra/mtoken-sdk-android/issues/234). PowerAuth "server stack" `2.0+` is now required.
    - [Migration guide](Migration-2.5.md)
    - Raised `minSdkVersion` from `21` to `23` and updated the build toolchain (Kotlin `2.2.0`, Android Gradle Plugin `8.13.0`, Gradle `8.13`).
    - `IOperationsService.authorizeOfflineOperation` is now **asynchronous** - it reports the result through a `callback` and returns an `ICancelable`, replacing the previous synchronous variant.
    - `QROperationParser` is now an instantiable class. When created with a `PowerAuthSDK` instance it verifies the operation signature during parsing; added `QROperation.verifySignature(powerAuth)` for manual verification.
    - Added `QROperationParser.parseAsync` (both instance and static) that runs parsing on a background executor and delivers the `Result<QROperation>` on the main thread, so callers don't have to manage threading themselves.
    - The parser now throws a structured `QROperationParseException` (a subclass of `IllegalArgumentException`) carrying a `QRParseError` reason.
    - Reworked `QROperationSignature`: `signingKey` → `keyType`, `signature` → `data`, `signatureString` → `dataSource` (old members deprecated). Added support for KMAC-based signatures via `KeyType.MAC_PERSONALIZED`.
    - `PowerAuthSDK.createOIDCActivation` no longer declares/throws `PowerAuthMissingConfigException` (removed in PowerAuth `2.0.0`).

## 2.4.0
- Added multiple PreApprovalScreens support [(#207)](https://github.com/wultra/mtoken-sdk-android/issues/207).
- Fixed handling of the unexpected attributes payload [(#217)](https://github.com/wultra/mtoken-sdk-android/issues/217).

## 2.3.0
- Added Alert Attribute [(#186](https://github.com/wultra/mtoken-sdk-android/issues/186)
- Refactored time handling to use `java.time.ZonedDateTime` throughout the SDK, replacing ThreeTenABP due to its deprecation. [(#93)](https://github.com/wultra/mtoken-sdk-android/issues/93)
- Added `ProximityCheck.withSynchronizedTime()` factory method to create proximity checks with server-synchronized timestamps, reducing clock drift issues. [(#201)](https://github.com/wultra/mtoken-sdk-android/issues/201)

## 2.2.1
- When the status property for UserOperation is missing, it now falls back to the PENDING value (happens only on legacy server API).

## 2.2.0
- Implemented Push changes to FCM and HMS [(#174)](https://github.com/wultra/mtoken-sdk-android/issues/174)

## 2.1.0

- Added `mobileTokenData` to authorize request for passing customer-specific data ([documentation](Using-Operations-Service.md#Passing-Additional-Mobile-Token-Data))
  - Available with PowerAuth server 1.10+
  - Can be used for fraud detection systems (FDS) or other custom business logic

## 2.0.0

- SDK simplification and added `OIDC` activation feature [(#182)](https://github.com/wultra/mtoken-sdk-android/pull/182)
    - [Migration guide](Migration-2.0.md)

## 1.12.0

- Upgraded to `PowerAuthSDK` `1.9.x` [(#165)](https://github.com/wultra/mtoken-sdk-ios/pull/165)
- PowerAuth "server stack" `1.9+` is now required
- Document fixes and improvements
- Added status to `UserOperation` and removed redundant `OperationHistoryEntry` [(#171)](https://github.com/wultra/mtoken-sdk-ios/pull/171)
- Removed deprecated `IOperationsService` methods [(#171)](https://github.com/wultra/mtoken-sdk-ios/pull/171)

## 1.11.1

- Added resultTexts to UserOperation [(#152)](https://github.com/wultra/mtoken-sdk-ios/pull/152)
- Extended PushParser to support parsing of inbox notifications [(#150)](https://github.com/wultra/mtoken-sdk-android/pull/150)
- Added statusReason to UserOperation [(#148)](https://github.com/wultra/mtoken-sdk-android/pull/148)

## 1.11.0

- Changed name of the log class to the `WMTLogger`
- Added listener to the log class

## 1.10.0 
- Removed currentServerTime() method [(#139)](https://github.com/wultra/mtoken-sdk-android/pull/139)
- Improved operations handling [(#138)](https://github.com/wultra/mtoken-sdk-android/pull/138)
- Added Huawei notification support [(#136)](https://github.com/wultra/mtoken-sdk-android/pull/136)
- Implement default and minimum pollingInterval [(#142)](https://github.com/wultra/mtoken-sdk-android/pull/142)

## 1.9.0

- Added possibility for custom reject reason [(#130)](https://github.com/wultra/mtoken-sdk-android/pull/130)
- Updated Amount and Conversion attributes to the new backend scheme [(#129)](https://github.com/wultra/mtoken-sdk-android/pull/129)
- Added this changelog to the documentation

## 1.8.4

- Operation detail and non-personalized operation claim [(#114)](https://github.com/wultra/mtoken-sdk-android/pull/114)

## 1.8.3

- Raise `minSdk` to 21 [(#125)](https://github.com/wultra/mtoken-sdk-android/pull/125)
- Added exception handling in PAC deeplink parsing [(#127)](https://github.com/wultra/mtoken-sdk-android/pull/127)

## 1.8.2

- Update `targetSdk` and dependencies [(#121)](https://github.com/wultra/mtoken-sdk-android/pull/121)
- Fix Javadoc generation [(#123)](https://github.com/wultra/mtoken-sdk-android/pull/123)
- Changed proximity timestamps names [(#118)](https://github.com/wultra/mtoken-sdk-android/pull/118)

## 1.8.1

- Introduced PACUtils [(#117)](https://github.com/wultra/mtoken-sdk-android/pull/117)

## 1.8.0

⚠️ This version of SDK requires PowerAuth Server version `1.5.0` or newer.

- Upgrade to PowerAuth 1.8.0 [(#110)](https://github.com/wultra/mtoken-sdk-android/pull/110)

## 1.7.4

- Added exception handling in PAC deeplink parsing [(#127)](https://github.com/wultra/mtoken-sdk-android/pull/127)

## 1.7.3

- Changed proximity timestamps names [(#119)](https://github.com/wultra/mtoken-sdk-android/pull/119)

## 1.7.2

- Introduced PACUtils [(#115)](https://github.com/wultra/mtoken-sdk-android/pull/115)

## 1.7.1

- Fixed deeplink parsing in PAC [(#113)](https://github.com/wultra/mtoken-sdk-android/pull/113)

## 1.7.0

- Added Proximity Anti-Fraud Check (PAC) feature [(#104)](https://github.com/wultra/mtoken-sdk-android/pull/104)

## 1.6.0

- Added Amount Conversion attribute [(#88)](https://github.com/wultra/mtoken-sdk-android/pull/88)
- Added Image attribute [(#89)](https://github.com/wultra/mtoken-sdk-android/pull/89)
- Added server time property to operations [(#92)](https://github.com/wultra/mtoken-sdk-android/pull/92)
- UI object moved to user operation [(#100)](https://github.com/wultra/mtoken-sdk-android/pull/100)

## 1.5.0

- Added new Inbox Service [(#76)](https://github.com/wultra/mtoken-sdk-android/pull/76)
- All services now provide API with callbacks with `Result<T>`. All interface-based APIs are now deprecated [(#78)](https://github.com/wultra/mtoken-sdk-android/pull/78)

## 1.4.3

- Customizable URI ID used for offline signature [(#65)](https://github.com/wultra/mtoken-sdk-android/pull/65)
- Added possession factor [(#66)](https://github.com/wultra/mtoken-sdk-android/pull/66)
- Support for PowerAuth mobile SDK 1.7.x [(#70)](https://github.com/wultra/mtoken-sdk-android/pull/70)
- Custom serialization + user agent [(#75)](https://github.com/wultra/mtoken-sdk-android/pull/75)

## 1.4.2

- Updated networking dependency

## 1.4.0

- Networking code was moved to its own library. This allows sharing configuration and some error handling across Wultra libraries.

## 1.3.0

⚠️ PowerAuth Mobile SDK v 1.6.x is now required [(#51)](https://github.com/wultra/mtoken-sdk-android/pull/51)  
⚠️ Be aware that SDK is no longer available via `jcenter()`. Please use `mavenCentral()`.

- Operation History API [(#52)](https://github.com/wultra/mtoken-sdk-android/pull/52)
- Improved documentation
- Updated dependencies

## 1.2.0

- Crash fix [(#31)](https://github.com/wultra/mtoken-sdk-android/pull/31)
- Added option to start polling without waiting [(#40)](https://github.com/wultra/mtoken-sdk-android/pull/40)
- Added "Operation Expiration Watcher" utility [(#41)](https://github.com/wultra/mtoken-sdk-android/pull/41)

## 1.1.6

- Added logging capabilities via the `Logger` class.

## 1.1.5

- Added `PushParser` class for parsing push notifications.

## 1.1.4

- Fixed exception in `TokenManager.getTokenAsync`

## 1.1.3

- Added ProGuard configuration to the library

## 1.1.2

- Fixed inconsistencies in `QROperationParser`

## 1.1.1

- Added the possibility to approve or reject operations received via different channels than this SDK.

## 1.1.0

- The `Operation` class changed to `UserOperation`
- Refactoring
- Better in-code documentation
- Better documentation

## 1.0.1

- Improved APIs
- Better documentation.

## 1.0.0

- Initial release of the Wultra Mobile Token SDK for Android.
