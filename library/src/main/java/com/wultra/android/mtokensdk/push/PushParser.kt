/*
 * Copyright 2022 Wultra s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 */

package com.wultra.android.mtokensdk.push

/**
 * Helper class that can translate incoming notifications from FCM to [PushMessage].
 */
class PushParser {

    companion object {

        /**
         * When you receive a push notification, you can test it here if it's a "WMT" notification.
         *
         * Return type can be [PushMessageOperationCreated], [PushMessageOperationFinished] or [PushMessageInboxReceived]
         *
         * @param notificationData: data of received notification.
         * @return parsed known [PushMessage] or null
         */
        @JvmStatic
        @JvmName("parseNotification")
        fun parseNotification(notificationData: Map<String, String>): PushMessage? {
            val messageType = notificationData["messageType"] ?: return null

            return when (messageType) {
                "mtoken.operationInit" -> parseOperationCreated(notificationData)
                "mtoken.operationFinished" -> parseOperationFinished(notificationData)
                "mtoken.inboxMessage.new" -> parseInboxMessageReceived(notificationData)
                else -> null
            }
        }

        // Helper methods
        private fun parseOperationCreated(notificationData: Map<String, String>): PushMessage? {
            val id = notificationData["operationId"] ?: return null
            val name = notificationData["operationName"] ?: return null
            return PushMessageOperationCreated(id, name, notificationData)
        }

        private fun parseOperationFinished(notificationData: Map<String, String>): PushMessage? {
            val id = notificationData["operationId"] ?: return null
            val name = notificationData["operationName"] ?: return null
            val result = notificationData["mtokenOperationResult"] ?: return null
            val operationResult = PushMessageOperationFinished.parseResult(result)
            return PushMessageOperationFinished(id, name, operationResult, notificationData)
        }

        private fun parseInboxMessageReceived(notificationData: Map<String, String>): PushMessage? {
            val inboxId = notificationData["inboxId"] ?: return null
            return PushMessageInboxReceived(inboxId, notificationData)
        }
    }
}

/**
 * Known push message abstract class.
 */
abstract class PushMessage(
    /** Original data on which was the push message constructed. */
    val originalData: Map<String, String>
)

/**
 * Created when a new operation was triggered.
 */
class PushMessageOperationCreated(
    /** Id of the operation. */
    val id: String,

    /** Name of the operation */
    val name: String,

    /** Original data on which was the push message constructed. */
    originalData: Map<String, String>
): PushMessage(originalData)

/**
 * Created when an operation was finished, successfully or non-successfully.
 */
class PushMessageOperationFinished(
    /** Id of the operation. */
    val id: String,

    /** Name of the operation */
    val name: String,

    /** Action which finished the operation */
    val result: Result,

    /** Original data on which was the push message constructed. */
    originalData: Map<String, String>
): PushMessage(originalData) {

    /** Action which finished the operation. */
    enum class Result {
        /** Operation was successfully confirmed. */
        SUCCESS,

        /** Operation failed to confirm. */
        FAIL,

        /** Operation expired */
        TIMEOUT,

        /** Operation was cancelled by the user. */
        CANCELED,

        /**
         * mToken authentication method was removed from the user.
         * This is very rare case.
         */
        METHOD_NOT_AVAILABLE,

        /** Unknown result. */
        UNKNOWN
    }

    companion object {
        internal fun parseResult(result: String): Result {
            return when (result) {
                "authentication.success" -> Result.SUCCESS
                "authentication.fail" -> Result.FAIL
                "operation.timeout" -> Result.TIMEOUT
                "operation.canceled" -> Result.CANCELED
                "operation.methodNotAvailable" -> Result.METHOD_NOT_AVAILABLE
                else -> Result.UNKNOWN // to be forward compatible
            }
        }
    }
}

/**
 * Created when a new inbox message was triggered.
 */
class PushMessageInboxReceived(
    /** Id of the inbox message. */
    val id: String,

    /** Original data on which was the push message constructed. */
    originalData: Map<String, String>
): PushMessage(originalData)
