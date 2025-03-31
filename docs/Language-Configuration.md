# Language Configuration

Before using any methods from this SDK that call the backend, a proper language should be set. A properly translated content is served based on this configuration. The property that stores language settings does not persist. You need to set `acceptLanguage` every time that the application boots.

<!-- begin box warning -->
Note: Content language capabilities are limited by the implementation of the server - it must support the provided language.
<!-- end -->

### Usage

You can use `WultraMobileToken.setAcceptLanguage` set to the user's preferred language for all WMT services. 

If you're creating services manually (e.g., `OperationsService`), you can set the `acceptLanguage` property directly.

### Format

The default value is always `en`. With other languages, we use values compliant with standard RFC [Accept-Language](https://tools.ietf.org/html/rfc7231#section-5.3.5).
