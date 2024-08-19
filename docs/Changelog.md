# Changelog

## 1.12.0 (Aug, 2024)

- Added resultTexts to UserOperation [(#152)](https://github.com/wultra/mtoken-sdk-ios/pull/152)
- Extended PushParser to support parsing of inbox notifications [(#150)](https://github.com/wultra/mtoken-sdk-android/pull/150)
- Added statusReason to UserOperation [(#148)](https://github.com/wultra/mtoken-sdk-android/pull/148)

## 1.11.0 (May, 2024)

- Changed name of the log class to the `WMTLogger`
- Added listener to the log class

## 1.10.0 
- Removed currentServerTime() method [(#139)](https://github.com/wultra/mtoken-sdk-android/pull/139)
- Improved operations handling [(#138)](https://github.com/wultra/mtoken-sdk-android/pull/138)
- Added Huawei notification support [(#136)](https://github.com/wultra/mtoken-sdk-android/pull/136)
- Implement default and minimum pollingInterval [(#142)](https://github.com/wultra/mtoken-sdk-android/pull/142)

## 1.9.0 (Jan 25, 2024)

- Added possibility for custom reject reason [(#130)](https://github.com/wultra/mtoken-sdk-android/pull/130)
- Updated Amount and Conversion attributes to the new backend scheme [(#129)](https://github.com/wultra/mtoken-sdk-android/pull/129)
- Added this changelog to the documentation

## 1.8.4 (Jan 9, 2024)

- Operation detail and non-personalized operation claim [(#114)](https://github.com/wultra/mtoken-sdk-android/pull/114)

## 1.8.3 (Dec 22, 2023)

- Raise `minSdk` to 21 [(#125)](https://github.com/wultra/mtoken-sdk-android/pull/125)
- Added exception handling in PAC deeplink parsing [(#127)](https://github.com/wultra/mtoken-sdk-android/pull/127)

## 1.8.2 (Dec 16, 2023)

- Update `targetSdk` and dependencies [(#121)](https://github.com/wultra/mtoken-sdk-android/pull/121)
- Fix Javadoc generation [(#123)](https://github.com/wultra/mtoken-sdk-android/pull/123)
- Changed proximity timestamps names [(#118)](https://github.com/wultra/mtoken-sdk-android/pull/118)

## 1.8.1 (Nov 30, 2023)

- Introduced PACUtils [(#117)](https://github.com/wultra/mtoken-sdk-android/pull/117)

## 1.8.0 (Nov 28, 2023)

⚠️ This version of SDK requires PowerAuth Server version `1.5.0` or newer.

- Upgrade to PowerAuth 1.8.0 [(#110)](https://github.com/wultra/mtoken-sdk-android/pull/110)

## 1.7.4 (Dec 22, 2023)

- Added exception handling in PAC deeplink parsing [(#127)](https://github.com/wultra/mtoken-sdk-android/pull/127)

## 1.7.3 (Dec 16, 2023)

- Changed proximity timestamps names [(#119)](https://github.com/wultra/mtoken-sdk-android/pull/119)

## 1.7.2 (Nov 30, 2023)

- Introduced PACUtils [(#115)](https://github.com/wultra/mtoken-sdk-android/pull/115)

## 1.7.1 (Nov 29, 2023)

- Fixed deeplink parsing in PAC [(#113)](https://github.com/wultra/mtoken-sdk-android/pull/113)

## 1.7.0 (Nov 20, 2023)

- Added Proximity Anti-Fraud Check (PAC) feature [(#104)](https://github.com/wultra/mtoken-sdk-android/pull/104)

## 1.6.0 (Jun 23, 2023)

- Added Amount Conversion attribute [(#88)](https://github.com/wultra/mtoken-sdk-android/pull/88)
- Added Image attribute [(#89)](https://github.com/wultra/mtoken-sdk-android/pull/89)
- Added server time property to operations [(#92)](https://github.com/wultra/mtoken-sdk-android/pull/92)
- UI object moved to user operation [(#100)](https://github.com/wultra/mtoken-sdk-android/pull/100)

## 1.5.0 (Jan 17, 2023)

- Added new Inbox Service [(#76)](https://github.com/wultra/mtoken-sdk-android/pull/76)
- All services now provide API with callbacks with `Result<T>`. All interface-based APIs are now deprecated [(#78)](https://github.com/wultra/mtoken-sdk-android/pull/78)

## 1.4.3 (Aug 26, 2022)

- Customizable URI ID used for offline signature [(#65)](https://github.com/wultra/mtoken-sdk-android/pull/65)
- Added possession factor [(#66)](https://github.com/wultra/mtoken-sdk-android/pull/66)
- Support for PowerAuth mobile SDK 1.7.x [(#70)](https://github.com/wultra/mtoken-sdk-android/pull/70)
- Custom serialization + user agent [(#75)](https://github.com/wultra/mtoken-sdk-android/pull/75)

## 1.4.2 (Feb 17, 2022)

- Updated networking dependency

## 1.4.0 (Sep 24, 2021)

- Networking code was moved to its own library. This allows sharing configuration and some error handling across Wultra libraries.

## 1.3.0 (Aug 16, 2021)

⚠️ PowerAuth Mobile SDK v 1.6.x is now required [(#51)](https://github.com/wultra/mtoken-sdk-android/pull/51)  
⚠️ Be aware that SDK is no longer available via `jcenter()`. Please use `mavenCentral()`.

- Operation History API [(#52)](https://github.com/wultra/mtoken-sdk-android/pull/52)
- Improved documentation
- Updated dependencies

## 1.2.0 (Mar 4, 2021)

- Crash fix [(#31)](https://github.com/wultra/mtoken-sdk-android/pull/31)
- Added option to start polling without waiting [(#40)](https://github.com/wultra/mtoken-sdk-android/pull/40)
- Added "Operation Expiration Watcher" utility [(#41)](https://github.com/wultra/mtoken-sdk-android/pull/41)

## 1.1.6 (Sep 7, 2020)

- Added logging capabilities via the `Logger` class.

## 1.1.5 (Aug 24, 2020)

- Added `PushParser` class for parsing push notifications.

## 1.1.4 (Jun 15, 2020)

- Fixed exception in `TokenManager.getTokenAsync`

## 1.1.3 (Jun 10, 2020)

- Added ProGuard configuration to the library

## 1.1.2 (Jun 2, 2020)

- Fixed inconsistencies in `QROperationParser`

## 1.1.1 (Jun 1, 2020)

- Added the possibility to approve or reject operations received via different channels than this SDK.

## 1.1.0 (May 19, 2020)

- The `Operation` class changed to `UserOperation`
- Refactoring
- Better in-code documentation
- Better documentation

## 1.0.1 (May 5, 2020)

- Improved APIs
- Better documentation.

