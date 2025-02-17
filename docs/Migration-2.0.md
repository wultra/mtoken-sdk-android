# Migration from 1.12.x to 2.0.x

This guide provides instructions for migrating from Wultra Mobile Token SDK for Android version `1.12.x` to version `2.0.x`.

Version `2.0.x` introduces a significant simplification of the SDK’s design and usage.

---

### Added Functionality
**Preferred Instantiation Method**  
   The `WultraMobileToken` class is now the recommended way to instantiate the SDK.
   `PowerAuthSDK` extension method is provided for this purpose.
    
```kotlin
fun createWultraTokenMobile(appContext: Context, powerAuth: PowerAuthSDK) {
    val wmt = powerAuth.createWultraMobileToken(appContext)
    
    val operationsService = wmt.operations
    val pushService = wmt.inbox
    val inboxService = wmt.push
}
```

### Removed Functionality

1. **Removed interfaces**

The interfaces `IOperationsService`, `IInboxService`, and `IPushService` have been removed and concrete class implementations `OperationsService`, `InboxService`, and `PushService`shall be used instead.

2. **Removed Extension Methods**  
   
The following extension methods of `PowerAuthSDK` and `WPNNetworkingService` have been removed:
   
      -  `createWMTOperations()`
      -  `createWMTInbox()`
      -  `createWMTPush`

Services should now be instantiated using the recommended `WultraMobileToken` approach. For advanced service configuration, refer to the links below.


   - [Using Operations Service: Creating an Instance](./Using-Operations-Service.md#creating-an-instance)
   - [Using Push Service: Creating an Instance](./Using-Push-Service.md#creating-an-instance)
   - [Using Inbox Service: Creating an Instance](./Using-Inbox-Service.md#creating-an-instance)

