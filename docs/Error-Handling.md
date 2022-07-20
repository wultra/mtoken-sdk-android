# Error Handling

All methods that communicate with the server API return an [`ApiError`](https://github.com/wultra/networking-android/blob/develop/library/src/main/java/com/wultra/android/powerauth/networking/error/ApiError.kt#docucheck-keep-link) instance in case of an error.  
Every `ApiError` consist of:  
- property `e` with an original exception that was thrown  
- convenience nullable error property `error` for known API error codes

## Known API Error codes

| Error Code | Description |
|---|---|
|`ERROR_GENERIC`|When unexpected error happened|
|`POWERAUTH_AUTH_FAIL`|General authentication failure (wrong password, wrong activation state, etc...)|
|`INVALID_REQUEST`|Invalid request sent - missing request object in the request|
|`INVALID_ACTIVATION`|Activation is not valid (it is different from configured activation)|
|`PUSH_REGISTRATION_FAILED`|Error code for a situation when registration to push notification fails|
|`OPERATION_ALREADY_FINISHED`|Operation is already finished|
|`OPERATION_ALREADY_FAILED`|Operation is already failed|
|`OPERATION_ALREADY_CANCELED`|Operation is canceled|
|`OPERATION_EXPIRED`|Operation is expired|
|`ERR_AUTHENTICATION`|Error in case that PowerAuth authentication fails|
|`ERR_SECURE_VAULT`|Error during secure vault unlocking|
|`ERR_ENCRYPTION`|Returned in case encryption or decryption fails|
