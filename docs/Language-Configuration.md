# Language Configuration

Before using any methods from this SDK that are calling backend proper language should be set. A properly translated content is served based on this configuration.

### Usage

Both `IOperationsService` and `IPushService` contains an `acceptLanguage` property that should be set to user preferred language.

> __Tip:__ Set `acceptLanguage` every time that the application boots as it's not persisted and when the user changes the application language (if such feature available) for seamless properly translated content delivery.

### Format

The default value is always `en`. With other languages, use values compliant with standard RFC [Accept-Language](https://tools.ietf.org/html/rfc7231#section-5.3.5)

_Note that language capabilities are limited by the implementation of the server_.