# Wultra Mobile Token SDK for Android

<!-- begin remove -->
<p align="center"><img src="images/intro.png" alt="Wultra Mobile Token SDK for Android" width="60%" /></p>

![build](https://github.com/wultra/mtoken-sdk-android/workflows/build/badge.svg)
![tests](https://github.com/wultra/mtoken-sdk-android/workflows/tests/badge.svg)
[![jcenter](https://img.shields.io/bintray/v/lime-company/WultraMobileTokenSDK/com.wultra.android.mtokensdk)](https://bintray.com/lime-company/WultraMobileTokenSDK/com.wultra.android.mtokensdk?tab=overview)
[![license](https://img.shields.io/github/license/wultra/mtoken-sdk-android)](LICENSE)

__Wultra Mobile Token SDK__ is a high-level SDK for operation approval.
<!-- end -->

## Introduction
<!-- begin remove -->
<img align="right" src="images/il-mobile-token.svg" width="40%" />
<!-- end -->
 
With Wultra Mobile Token (WMT) SDK, you can integrate an out-of-band operation approval into an existing mobile app, instead of using a standalone mobile token application. WMT is built on top of [PowerAuth Mobile SDK](https://github.com/wultra/powerauth-mobile-sdk). It communicates with the "Mobile Token REST API" and "Mobile Push Registration API". Individual endpoints are described in the [PowerAuth Webflow documentation](https://github.com/wultra/powerauth-webflow/).

To understand the Wultra Mobile Token SDK purpose on a business level better, you can visit our own [Mobile Token application](https://www.wultra.com/mobile-token). We use Wultra Mobile Token SDK in our mobile token application as well.

WMT SDK library does precisely this:

- Retrieves the list of operations that are pending for approval for a given user.
- Registers an existing PowerAuth activation to receive push notifications.
- Approves or rejects operations with PowerAuth transaction signing.

_Note: We also provide an [iOS version of this library](https://github.com/wultra/mtoken-sdk-ios)_

## Integration Tutorials
- [SDK Integration](SDK-Integration.md)
- [Using Operations Service](Using-Operations-Service.md)
- [Using Push Service](Using-Push-Service.md)
- [Error Handling](Error-Handling.md)
- [Language Configuration](Language-Configuration.md)

## License

All sources are licensed using the Apache 2.0 license. You can use them with no restrictions. If you are using this library, please let us know. We will be happy to share and promote your project.

## Contact

If you need any assistance, do not hesitate to drop us a line at [hello@wultra.com](mailto:hello@wultra.com) or our official [gitter.im/wultra](https://gitter.im/wultra) channel.

### Security Disclosure

If you believe you have identified a security vulnerability with Wultra Mobile Token SDK, you should report it as soon as possible via email to [support@wultra.com](mailto:support@wultra.com). Please do not post it to the public issue tracker.
