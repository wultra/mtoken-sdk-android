# Using Inbox Service

<!-- begin remove -->
- [Introduction](#introduction)
- [Creating an Instance](#creating-an-instance)
- [Inbox Service Usage](#inbox-service-usage)
  - [Get Number of Unread Messages](#get-number-of-unread-messages)
  - [Get List of Messages](#get-list-of-messages)
  - [Get Message Detail](#get-message-detail)
  - [Set Message as Read](#set-message-as-read)
- [Error handling](#error-handling)

## Introduction
<!-- end -->

Inbox Service is responsible for managing messages in the Inbox. The inbox is a simple one way delivery system that allows you to deliver messages to the user.

<!-- begin box warning -->
Note: Before using Inbox Service, you need to have a `PowerAuthSDK` object available and initialized with a valid activation. Without a valid PowerAuth activation, service will return an error.
<!-- end -->

## Creating an Instance

### Factory Extension With SSL Validation Strategy

Convenience factory method that will return a new instance. A new [`OkHttpClient`](https://square.github.io/okhttp/) will be created based on the chosen `SSLValidationStrategy` in the last parameter.

```kotlin
fun PowerAuthSDK.createInboxService(appContext: Context, baseURL: String, strategy: SSLValidationStrategy): IInboxService
```

- `appContext` - application context
- `baseURL` - address, where your operations server can be reached
- `strategy` - strategy used when validating HTTPS requests. Following strategies can be used:
    - `SSLValidationStrategy.default`
    - `SSLValidationStrategy.noValidation`
    - `SSLValidationStrategy.sslPinning`

__Optional parameters:__

- `userAgent` - Optional default user agent used for each request

### Factory Extension With OkHttpClient

Convenience factory method that will return a new instance with provided [`OkHttpClient`](https://square.github.io/okhttp/) that you can configure on your own.

```kotlin
fun PowerAuthSDK.createInboxService(appContext: Context, baseURL: String, httpClient: OkHttpClient): IInboxService
```

- `appContext` - application context
- `baseURL`-  address, where your operations server can be reached
- `httpClient` - [`OkHttpClient`](https://square.github.io/okhttp/) instance used for API requests

__Optional parameters:__

- `userAgent` - Optional default user agent used for each request

## Inbox Service Usage

### Get Number of Unread Messages

To get the number of unread messages, use the following code:

```kotlin
inboxService.getUnreadCount { 
    it.onSuccess {
        if (it.countUnread > 0) {
            print("There are ${it.countUnread} new message(s) in your inbox")
        } else {
            print("Your inbox is empty")
        }
    it.onFailure {
        print("Error \(error)")
    }
}
```

### Get List of Messages

The Inbox Service provide a paged list of messages:

```kotlin
// First page is 0, next 1, etc...
inboxService.getMessageList(pageNumber = 0, pageSize = 50, onlyUnread = false) {
    it.onSuccess { messages ->
        if (messages.count) < 50 {
            // This is the last page
        }
        // Process messages    
    }
    it.onFailure {
        // Process error
    }
}
```

To get the list of all messages, call:

```kotlin
inboxService.getAllMessages {
    it.onSuccess { messages ->
        print("Inbox contains the following message(s):")
        for (msg in messages) {
            print(" - ${msg.subject}")
            print("   * ID = ${msg.id}")
        }
    }
    it.onFailure {
        // Process error
    }
}
```

### Get Message Detail

Each message has its unique identifier. To get the body of message, use the following code:

```kotlin
let messageId = messagesList.first!.id
inboxService.getMessageDetail(messageId) {
    it.onSuccess {
        print("Received message:")
        print("${it.subject}")
        print("${it.body}")
    }
    it.onFailure {
        // Process error
    }
}
```

### Set Message as Read

To mark message as read by the user, use the following code:

```kotlin
let messageId = messagesList.first!.id
inboxService.markRead(messageId: messageId) {
    it.onSuccess {
        // OK
    }
    it.onFailure {
        // Process error
    }
}
```

Alternatively, you can mark all messages as read:

```kotlin
inboxService.markAllRead {
    it.onSuccess {
        // OK
    }
    it.onFailure {
        // Process error
    }
}
```

## Error handling

Every error produced by the Push Service is of a `WMTError` type. For more information see detailed [error handling documentation](Error-Handling.md).
