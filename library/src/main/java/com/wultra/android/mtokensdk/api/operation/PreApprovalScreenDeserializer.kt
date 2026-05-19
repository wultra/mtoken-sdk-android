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
import com.google.gson.reflect.TypeToken
import com.wultra.android.mtokensdk.api.operation.model.preapproval.PreApprovalControls
import com.wultra.android.mtokensdk.api.operation.model.preapproval.PreApprovalElement
import com.wultra.android.mtokensdk.api.operation.model.preapproval.PreApprovalElementListItem
import com.wultra.android.mtokensdk.api.operation.model.preapproval.PreApprovalScreen
import com.wultra.android.mtokensdk.api.operation.utils.asJsonPrimitiveOrNull
import com.wultra.android.mtokensdk.api.operation.utils.getAsBooleanSafe
import com.wultra.android.mtokensdk.api.operation.utils.getAsStringSafe
import com.wultra.android.mtokensdk.api.operation.utils.parseEnumWithFallback
import com.wultra.android.mtokensdk.api.operation.utils.safeDeserializeArray
import com.wultra.android.mtokensdk.api.operation.utils.safeDeserializeObject
import java.lang.reflect.Type
import kotlin.collections.ifEmpty

/**
 * Gson deserializer for [PreApprovalScreen].
 *
 * Legacy handling:
 *  - If "items" or "approvalType" are present, build the new-model screen ONLY from those legacy bits:
 *      * items[] -> elements = [ LIST_ITEM(text) ... ]
 *      * approvalType == "SLIDER" -> controls.approve.type = SLIDER
 *
 * New model:
 *  - Reads id/backButton/image/elements/controls normally.
 */
class PreApprovalScreenDeserializer : JsonDeserializer<PreApprovalScreen> {

    override fun deserialize(json: JsonElement, typeOfT: Type, ctx: JsonDeserializationContext): PreApprovalScreen {
        val obj = json.asJsonObject
        val type = parseScreenType(obj)
        val heading = obj.getAsStringSafe("heading") ?: ""
        val message = obj.getAsStringSafe("message") ?: ""
        val presence = detectVersion(obj)

        // --- Legacy branch ---
        if (!presence.hasNewModel && (presence.hasLegacyItems || presence.hasLegacyApproval)) {
            return parseLegacyScreen(ctx, obj, type, heading, message)
        }

        // --- New-model branch (preferred) ---
        val id = obj.getAsStringSafe("id")
        val backButton = obj.getAsBooleanSafe("backButton")
        val image = obj.getAsStringSafe("image")

        // Parse the elements array (if present)
        val elements: List<PreApprovalElement>? = safeDeserializeArray(ctx, obj.get("elements"), object : TypeToken<List<PreApprovalElement>>() {}.type)

        // Parse controls object (optional)
        val controls: PreApprovalControls? = safeDeserializeObject(ctx, obj.get("controls"), PreApprovalControls::class.java)

        // --- Build the final screen instance ---
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

    // ---- helpers ----
    private fun parseScreenType(obj: JsonObject): PreApprovalScreen.Type {
        val typeStr = obj.getAsStringSafe("type")
        return parseEnumWithFallback(typeStr, "PreApproval screen type")
    }

    private data class PresenceFlags(
        val hasNewModel: Boolean,
        val hasLegacyItems: Boolean,
        val hasLegacyApproval: Boolean
    )

    private fun detectVersion(obj: JsonObject): PresenceFlags {
        val hasNewModel = obj.has("elements") || obj.has("controls") || obj.has("id") || obj.has("backButton") || obj.has("image")
        val hasLegacyItems = obj.get("items")?.isJsonArray == true
        val hasLegacyApproval = obj.get("approvalType")?.asJsonPrimitiveOrNull()?.isString == true

        return PresenceFlags(hasNewModel, hasLegacyItems, hasLegacyApproval)
    }

    /**
     * Handles the conversion from the old single-screen JSON format into
     * a new-model [PreApprovalScreen].
     */
    private fun parseLegacyScreen(
        ctx: JsonDeserializationContext,
        obj: JsonObject,
        type: PreApprovalScreen.Type,
        heading: String,
        message: String
    ): PreApprovalScreen {
        val image = obj.getAsStringSafe("image") ?: FALLBACK_IMAGE

        // create elements from legacy "items" array
        val rawItems: List<String>? = safeDeserializeArray(ctx, obj.get("items"), object : TypeToken<List<String>>() {}.type)
        val elements: List<PreApprovalElement>? = rawItems
            ?.map { text -> PreApprovalElementListItem(icon = FALLBACK_ICON, text = text) }
            ?.ifEmpty { null }

        // create controls from legacy "approvalType"
        val controls: PreApprovalControls? =
            if (obj.getAsStringSafe("approvalType") == "SLIDER") {
                PreApprovalControls(
                    flip = true,
                    decline = PreApprovalControls.Decline(PreApprovalControls.DeclineType.BACK),
                    approve = PreApprovalControls.Approve(PreApprovalControls.ApproveType.SLIDER)
                )
            } else {
                null
            }

        return PreApprovalScreen(
            type = type,
            heading = heading,
            message = message,
            image = image,
            elements = elements,
            controls = controls
        )
    }

    // Static constants — fallback identifiers for legacy screens
    companion object {
        /** Default placeholder image for legacy Pre-approval screens */
        private const val FALLBACK_IMAGE = "fallback_image"
        /** Default placeholder icon for legacy list items */
        private const val FALLBACK_ICON = "fallback_icon"
    }
}
