# Wultra Mobile Token SDK for Android
 
With Wultra Mobile Token (WMT) SDK, you can integrate an out-of-band operation approval into an existing mobile app, instead of using a standalone mobile token application. WMT is built on top of [PowerAuth Mobile SDK](https://github.com/wultra/powerauth-mobile-sdk). It communicates with the "Mobile Token REST API" and "Mobile Push Registration API". Individual endpoints are described in the [PowerAuth Webflow documentation](https://github.com/wultra/powerauth-webflow/).

To understand the Wultra Mobile Token SDK purpose on a business level better, you can visit our own [Mobile Token application](https://www.wultra.com/mobile-token). We use Wultra Mobile Token SDK in our mobile token application as well.

WMT SDK library does precisely this:

- Retrieves the list of operations that are pending for approval for a given user.
- Approves or rejects operations with PowerAuth transaction signing.
- Registers an existing PowerAuth activation to receive push notifications.

Remarks:

- This library does not contain any UI.
- We also provide an [iOS version of this library](https://github.com/wultra/mtoken-sdk-ios). 

## Migration Guides

If you need to upgrade Wultra Mobile Token SDK for Android to a newer version, you can check the following migration guides:

- [Migration from version `1.4.x` to `1.5.x`](Migration-1.5.md)


<!-- begin remove -->
## Integration Tutorials
- [SDK Integration](SDK-Integration.md)
- [Using Operations Service](Using-Operations-Service.md)
- [Using Push Service](Using-Push-Service.md)
- [Using Inbox Service](Using-Inbox-Service.md)
- [Operation Expiration Handling](Operation-Expiration.md)
- [Error Handling](Error-Handling.md)
- [Language Configuration](Language-Configuration.md)
- [Logging](Logging.md)
<!-- end -->
