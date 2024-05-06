# Logging

The library is intensively logging into the console via `WMTLogger`.

<!-- begin box info -->
`WMTLogger` calls internally the `android.util.Log` class.
<!-- end -->

### Verbosity Level

You can limit the amount of logged information via the `verboseLevel` static property.

| Level                 | Description                                         |
|-----------------------|-----------------------------------------------------|
| `OFF`                 | Silences all messages.                              |
| `ERROR`               | Only errors will be printed to the console.         |
| `WARNING` _(default)_ | Errors and warnings will be printed to the console. |
| `DEBUG`               | All messages will be printed to the console.        |

### Networking Logs

Networking logs are managed by the [networking library and it's logger](https://github.com/wultra/networking-android)

### Log Listener

The `WMTLogger` class offers a static `logListener` property. If you provide a listener, all logs will also be passed to it (the library always logs into the Android default log).

<!-- begin box info -->
Log listener comes in handy when you want to log into a file or some online service.
<!-- end -->
