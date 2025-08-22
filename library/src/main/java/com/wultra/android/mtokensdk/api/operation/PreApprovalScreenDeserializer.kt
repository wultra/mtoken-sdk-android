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
import com.wultra.android.mtokensdk.api.operation.model.PreApprovalScreen
import com.wultra.android.mtokensdk.api.operation.model.PreApprovalScreenConfirmAction
import com.wultra.android.mtokensdk.api.operation.model.PreApprovalElement
import com.wultra.android.mtokensdk.api.operation.model.PreApprovalControls
import java.lang.reflect.Type

/**
 * Gson deserializer [PreApprovalScreen]
 */
class PreApprovalScreenDeserializer: JsonDeserializer<PreApprovalScreen> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): PreApprovalScreen {
        val jsonObject = json.asJsonObject
        val type = try { PreApprovalScreen.Type.valueOf(jsonObject.get("type").asString ?: "UNKNOWN") } catch (e: Throwable) { PreApprovalScreen.Type.UNKNOWN }
        val heading = jsonObject.get("heading").asString
        val message = jsonObject.get("message").asString
        val items = jsonObject.get("items")?.takeIf { it.isJsonArray }?.asJsonArray?.map { it.asString }
        val approvalType: PreApprovalScreenConfirmAction? = jsonObject.get("approvalType")
            ?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }
            ?.asString
            ?.let { runCatching { PreApprovalScreenConfirmAction.valueOf(it) }.getOrNull() }

        val id: String? = jsonObject.get("id")?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString
        val backButton: Boolean? = jsonObject.get("backButton")?.takeIf { it.isJsonPrimitive }?.asBoolean
        val image: String? = jsonObject.get("image")?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString

        val elements: List<PreApprovalElement>? = jsonObject.get("elements")
            ?.takeIf { it.isJsonArray }
            ?.let { arr ->
                arr.asJsonArray.map { el ->
                    context.deserialize(el, PreApprovalElement::class.java)
                }
            }

        val controls: PreApprovalControls? = jsonObject.get("controls")
            ?.takeIf { it.isJsonObject }
            ?.let { el -> context.deserialize(el, PreApprovalControls::class.java) }

        return PreApprovalScreen(
            type, heading, message, items, approvalType,
            id, backButton, image, elements, controls
        )
    }
}
