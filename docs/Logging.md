# Logging

For logging purposes, WMT uses `Logger` class inside the `com.wultra.android.mtokensdk.common` package that prints to the console. 

_Note that `Logger` is internally using `android.util.Log` class with verbosity ERROR, WARNING, and DEBUG and is subjected to its internal logic._

### Verbosity Level

You can limit the amount of logged information via `verboseLevel` static         property.

| Level | Description |
| --- | --- |
| `OFF` | Silences all messages. |
| `ERROR` | Only errors will be printed to the console. |
| `WARNING` _(default)_ | Errors and warnings will be printed to the console. |
| `DEBUG` | All messages will be printed to the console. |

Example configuration:

```kotlin
import com.wultra.android.mtokensdk.common.Logger

Logger.verboseLevel = Logger.VerboseLevel.DEBUG
```

### Networking logs

All requests and responses are logged in `DEBUG` level. 

_In case that you provided your own okhttp instance, you need to setup your own logging for monitoring the traffic via the okhttp interceptors._