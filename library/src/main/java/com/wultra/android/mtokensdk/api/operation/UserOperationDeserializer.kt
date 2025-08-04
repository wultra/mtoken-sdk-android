/*
 * Copyright (c) 2025, Wultra s.r.o. (www.wultra.com).
 *
 * All rights reserved. This source code can be used only for purposes specified
 * by the given license contract signed by the rightful deputy of Wultra s.r.o.
 * This source code can be used only by the owner of the license.
 *
 * Any disputes arising in respect of this agreement (license) shall be brought
 * before the Municipal Court of Prague.
 */

package com.wultra.android.mtokensdk.api.operation

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.wultra.android.mtokensdk.api.operation.model.UserOperation
import java.lang.reflect.Type
import org.threeten.bp.ZonedDateTime
import com.wultra.android.mtokensdk.api.operation.model.FormData
import com.wultra.android.mtokensdk.api.operation.model.AllowedSignatureType
import com.wultra.android.mtokensdk.api.operation.model.OperationUIData
import com.wultra.android.mtokensdk.api.operation.model.UserOperationStatus

class UserOperationDeserializer : JsonDeserializer<UserOperation> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): UserOperation {
        val obj = json.asJsonObject

        // Required fields
        val id = obj["id"].asString
        val name = obj["name"].asString
        val data = obj["data"].asString
        val created = context.deserialize<ZonedDateTime>(obj["operationCreated"], ZonedDateTime::class.java)
        val expires = context.deserialize<ZonedDateTime>(obj["operationExpires"], ZonedDateTime::class.java)
        val formData = context.deserialize<FormData>(obj["formData"], FormData::class.java)
        val allowedSignatureType = context.deserialize<AllowedSignatureType>(obj["allowedSignatureType"], AllowedSignatureType::class.java)

        // Optional fields
        val ui = obj.get("ui")?.let { context.deserialize<OperationUIData>(it, OperationUIData::class.java) }
        val statusReason = obj.get("statusReason")?.asString

        // Status handling, fallback to PENDING on legacy systems
        val rawStatus = obj.get("status")?.asString
        val status = if (rawStatus != null) {
            UserOperationStatus.valueOf(rawStatus)
        } else {
            UserOperationStatus.PENDING
        }

        return UserOperation(
            id = id,
            name = name,
            data = data,
            created = created,
            expires = expires,
            formData = formData,
            allowedSignatureType = allowedSignatureType,
            ui = ui,
            proximityCheck = null,
            mobileTokenData = null,
            statusReason = statusReason,
            status = status
        )
    }
}