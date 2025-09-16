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
import com.google.gson.*
import com.wultra.android.mtokensdk.api.operation.model.preapproval.PreApprovalControls
import com.wultra.android.mtokensdk.api.operation.model.preapproval.PreApprovalElement
import com.wultra.android.mtokensdk.api.operation.model.preapproval.PreApprovalElementListItem
import com.wultra.android.mtokensdk.api.operation.model.preapproval.PreApprovalScreen
import java.lang.reflect.Type

/**
 * Gson deserializer [PreApprovalScreen].
 *
 * Legacy handling:
 *  - If "items" or "approvalType" are present, build the new-model screen ONLY from those legacy bits:
 *      * items[] -> elements = [ LISTITEM(text) ... ]
 *      * approvalType == "SLIDER" -> controls.approve.type = SLIDER
 *
 * New model:
 *  - Reads id/backButton/image/elements/controls normally.
 */
class PreApprovalScreenDeserializer : JsonDeserializer<PreApprovalScreen> {

    override fun deserialize(json: JsonElement, typeOfT: Type, ctx: JsonDeserializationContext): PreApprovalScreen {
        val obj = json.asJsonObject

        val type = obj.getAsStringSafe("type")
            ?.let { runCatching { PreApprovalScreen.Type.valueOf(it) }.getOrDefault(PreApprovalScreen.Type.UNKNOWN) }
            ?: PreApprovalScreen.Type.UNKNOWN

        val heading = obj.getAsStringSafe("heading") ?: ""
        val message = obj.getAsStringSafe("message") ?: ""

        // Presence flags
        val hasNewModel = obj.has("elements") || obj.has("controls") || obj.has("id") || obj.has("backButton") || obj.has("image")
        val hasLegacyItems = obj.get("items")?.isJsonArray == true
        val hasLegacyApproval = obj.get("approvalType")?.asJsonPrimitiveOrNull()?.isString == true

        if (!hasNewModel && (hasLegacyItems || hasLegacyApproval)) {
            // ----- Legacy → new-model mapping -----
            val elements: List<PreApprovalElement>? = when {
                // 1) items key is present AND is JSON array
                obj.has("items") && obj.get("items")?.isJsonArray == true -> {
                    val list = obj.get("items")!!.asJsonArray
                        .mapNotNull { it.asJsonPrimitiveOrNull()?.asString }
                        .map { text -> PreApprovalElementListItem(text = text) }
                    list.ifEmpty { null }
                }
                // 2) items key absent or not an array → treat as null
                else -> null
            }

            val controls: PreApprovalControls? =
                if (obj.getAsStringSafe("approvalType") == "SLIDER") {
                    PreApprovalControls(approve = PreApprovalControls.Approve(PreApprovalControls.ApproveType.SLIDER))
                } else { null }

            return PreApprovalScreen(
                type = type,
                heading = heading,
                message = message,
                elements = elements,
                controls = controls
            )
        }

        // ----- New-model parsing (preferred when present) -----
        val id = obj.getAsStringSafe("id")
        val backButton = obj.getAsBooleanSafe("backButton")
        val image = obj.getAsStringSafe("image")

        val elements: List<PreApprovalElement>? = if (obj.has("elements") && obj.get("elements")?.isJsonArray == true) {
            val list = obj.get("elements")!!.asJsonArray
                .map { el -> ctx.deserialize<PreApprovalElement>(el, PreApprovalElement::class.java) }
            list.ifEmpty { null }
        } else {
            null
        }

        val controls: PreApprovalControls? = obj.get("controls")
            ?.takeIf { it.isJsonObject }
            ?.let { el -> ctx.deserialize(el, PreApprovalControls::class.java) }

        return PreApprovalScreen(
            type = type,
            heading = heading,
            message = message,
            id = id,
            backButton = backButton,
            image = image,
            elements = elements,
            controls = controls
        )
    }

    // helpers
    private fun JsonObject.getAsStringSafe(name: String): String? =
        this.get(name)?.asJsonPrimitiveOrNull()?.takeIf { it.isString }?.asString

    private fun JsonObject.getAsBooleanSafe(name: String): Boolean? =
        this.get(name)?.asJsonPrimitiveOrNull()?.takeIf { it.isBoolean }?.asBoolean

    private fun JsonElement.asJsonPrimitiveOrNull(): JsonPrimitive? =
        takeIf { it.isJsonPrimitive }?.asJsonPrimitive
}
