/*
 * Copyright (c) 2020, Wultra s.r.o. (www.wultra.com).
 *
 * All rights reserved. This source code can be used only for purposes specified
 * by the given license contract signed by the rightful deputy of Wultra s.r.o.
 * This source code can be used only by the owner of the license.
 *
 * Any disputes arising in respect of this agreement (license) shall be brought
 * before the Municipal Court of Prague.
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
         * Return type can be [PushMessageOperationCreated] or [PushMessageOperationFinished]
         *
         * @param notificationData: data of received notification.
         * @return parsed known [PushMessage] or null
         */
        @JvmStatic
        @JvmName("parseNotification")
        fun parseNotification(notificationData: Map<String, String>): PushMessage? {

            val id = notificationData["operationId"] ?: return null
            val name = notificationData["operationName"] ?: return null

            return when (notificationData["messageType"]) {
                "mtoken.operationInit" -> {
                    PushMessageOperationCreated(id, name)
                }
                "mtoken.operationFinished" -> {
                    notificationData["mtokenOperationResult"]?.let { return PushMessageOperationFinished(id, name, PushMessageOperationFinished.parseResult(it))  }
                }
                else -> {
                    null
                }
            }
        }
    }
}

/**
 * Known push message abstract class.
 */
abstract class PushMessage

/**
 * Created when a new operation was triggered.
 */
data class PushMessageOperationCreated(val id: String, val name: String): PushMessage()

/**
 * Created when an operation was finished, successfully or non-successfully.
 */
data class PushMessageOperationFinished(val id: String, val name: String, val result: Result): PushMessage() {
    enum class Result {
        /**
         * Operation was successfully confirmed.
         */
        SUCCESS,

        /**
         * Operation failed to confirm.
         */
        FAIL,

        /**
         * Operation expired
         */
        TIMEOUT,

        /**
         * Operation was cancelled by the user.
         */
        CANCELED,

        /**
         * mToken authentication method was removed from the user.
         * This is very rare case.
         */
        METHOD_NOT_AVAILABLE,

        /**
         * Unknown result.
         */
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