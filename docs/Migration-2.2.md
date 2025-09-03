# Migration from 2.1.x to 2.2.x

This guide provides instructions for migrating from Wultra Mobile Token SDK for Android version `2.1.x` to version `2.2.x`.

---

## Changes in `PushService` API

Methods `register(fcmToken:callback:)` and `register(hmsToken:callback:)` have been deprecated in favor of the new `register(data:completion:)` method.

<!-- begin box warning -->
If your server is running an older version than `1.10.x`, use the deprecated methods instead to stay compatible.
<!-- end -->