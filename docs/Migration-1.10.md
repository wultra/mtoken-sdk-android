# Migration from 1.9.x to 1.10.x

This guide contains instructions for migration from Wultra Mobile Token SDK for Android version `1.9.x` to version `1.10.x`.

## 1. Current Server Date

### Removed Functionality

The following calculated property was removed from the `IOperationsService` interface:

```kotlin
fun currentServerDate(): ZonedDateTime?
```

The `currentServerDate()` method provided a calculated property based on the difference between the phone's date and the date on the server. However, it had limitations and could be incorrect under certain circumstances, such as when the user decided to change the system time during the runtime of the application.

### Replace with

The new time synchronization directly from `PowerAuthSDK.timeSynchronizationService` is more precise and reliable. 

Here is the updated test method for reference:

```kotlin
/** currentServerDate was removed in favour of PowerAuthSDK timeSynchronizationService */
@Test
fun testServerTime() {
    var currentTime: ZonedDateTime? = null
    val timeService = pa.timeSynchronizationService
    if (timeService.isTimeSynchronized) {
        val instant = Instant.ofEpochMilli(timeService.currentTime)
        val zoneId = ZoneId.systemDefault()
        currentTime = ZonedDateTime.ofInstant(instant, zoneId)
    } 
    Assert.assertNotNull(currentTime)
}
```

## 2. Operations handling

API unified with iOS implementation. Instead of replacing list of operations every time `getOperation()` is called OperationsRegister is implemented to keep last fetched operations list and update it when necessary.


### Removed Functionality
`fun operationsLoaded(operations: List<UserOperation>)`
### Replace with 
`fun operationsChanged(operations: List<UserOperation>, removed: List<UserOperation>, added: List<UserOperation>)`
