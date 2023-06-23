/*
 * Copyright 2023 Wultra s.r.o.
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
import com.wultra.android.mtokensdk.api.operation.model.*
import com.wultra.android.mtokensdk.operation.JSONValue
import java.lang.reflect.Type

/**
 * Gson deserializer [PostApprovalScreen]
 *
 * Based on "type" it returns [PostApprovalScreenReview], [PostApprovalScreenRedirect]
 * or [PostApprovalScreenGeneric]
 */
class PostApprovalScreenDeserializer : JsonDeserializer<PostApprovalScreen> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type?,
        context: JsonDeserializationContext
    ): PostApprovalScreen {
        val jsonObject = json.asJsonObject
        val type = jsonObject.get("type").asString
        val heading = jsonObject.get("heading").asString
        val message = jsonObject.get("message").asString

        return when (type) {
            "REVIEW" -> {
                val attributes: List<Attribute> = jsonObject.get("payload")
                    .asJsonObject.getAsJsonArray("attributes")
                    .map {
                        context.deserialize(it, Attribute::class.java)
                    }
                val payload = ReviewPostApprovalScreenPayload(attributes)
                PostApprovalScreenReview(heading, message, payload)
            }
            "MERCHANT_REDIRECT" -> {
                val redirectText = jsonObject.get("payload").asJsonObject.get("redirectText").asString
                val redirectUrl = jsonObject.get("payload").asJsonObject.get("redirectUrl").asString
                val countdown = jsonObject.get("payload").asJsonObject.get("countdown").asInt
                val payload = RedirectPostApprovalScreenPayload(redirectText, redirectUrl, countdown)
                PostApprovalScreenRedirect(heading, message, payload)
            }
            else -> { // "GENERIC"
                val payload = JSONValue.parse(jsonObject.get("payload"))
                PostApprovalScreenGeneric(heading, message, payload)
            }
        }
    }
}