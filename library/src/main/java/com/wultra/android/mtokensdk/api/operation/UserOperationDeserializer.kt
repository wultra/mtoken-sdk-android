/*
 * Copyright 2025 Wultra s.r.o.
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

package com.wultra.android.mtokensdk.api.operation

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.wultra.android.mtokensdk.api.operation.model.UserOperation
import java.lang.reflect.Type
import com.wultra.android.mtokensdk.api.operation.model.FormData
import com.wultra.android.mtokensdk.api.operation.model.AllowedSignatureType
import com.wultra.android.mtokensdk.api.operation.model.OperationUIData
import com.wultra.android.mtokensdk.api.operation.model.UserOperationStatus
import java.time.ZonedDateTime

/**
 * Custom deserializer for [UserOperation]
 */
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
        val statusReason: String? = obj.get("statusReason")
            ?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }
            ?.asString

        // Status handling, fallback to PENDING on legacy systems
        val rawStatus: String? = obj.get("status")
            ?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }
            ?.asString

        val status = try {
            rawStatus?.let { UserOperationStatus.valueOf(it) } ?: UserOperationStatus.PENDING
        } catch (_: IllegalArgumentException) {
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
