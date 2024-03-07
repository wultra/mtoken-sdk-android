# Logging

For logging purposes, WMT uses the `Logger` class inside the `com.wultra.android.mtokensdk.common` package that prints to the console.

<!-- begin box warning -->
Note that `Logger` is internally using `android.util.Log` class with verbosity ERROR, WARNING, and DEBUG and is subjected to its internal logic.
<!-- end -->

### Verbosity Level

You can limit the amount of logged information via the `verboseLevel` static property.

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

### Networking Logs

All requests and responses are logged in the `DEBUG` level.

<!-- begin box warning -->
In case you provided your own OkHttp instance, you need to set up your own logging for monitoring the traffic via the OkHttp interceptors.
<!-- end -->
