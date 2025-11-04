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

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import com.wultra.android.mtokensdk.api.operation.model.preapproval.PreApprovalElement
import com.wultra.android.mtokensdk.api.operation.model.preapproval.PreApprovalElementAlert
import com.wultra.android.mtokensdk.api.operation.model.preapproval.PreApprovalElementButton
import com.wultra.android.mtokensdk.api.operation.model.preapproval.PreApprovalElementListItem
import com.wultra.android.mtokensdk.api.operation.utils.parseEnumOrNull
import com.wultra.android.mtokensdk.api.operation.utils.parseEnumWithFallback
import com.wultra.android.mtokensdk.api.operation.utils.readShallowStringMap

/**
 * Type adapter for deserializing [PreApprovalElement].
 * - Creates concrete subtypes: [PreApprovalElementListItem], [PreApprovalElementAlert], [PreApprovalElementButton]
 * - Safely handles unknown fields and unknown element types.
 * - Serialization is not used -> write() is a no-op.
 */
internal class PreApprovalElementTypeAdapter : TypeAdapter<PreApprovalElement>() {

    override fun read(reader: JsonReader): PreApprovalElement? {
        if (reader.peek() == JsonToken.NULL) { reader.nextNull(); return null }

        // Read once → then branch by type
        val map = reader.readShallowStringMap()

        val type = parseScreenType(map["type"])
        val id = map["id"]
        val icon = map["icon"]
        val text = map["text"]

        return when (type) {
            PreApprovalElement.Type.LIST_ITEM -> {
                val style = map["style"]?.parseEnumOrNull<PreApprovalElement.Style>()
                PreApprovalElementListItem(id = id, style = style, icon = icon, text = text)
            }
            PreApprovalElement.Type.ALERT -> {
                val style = map["style"]?.parseEnumOrNull<PreApprovalElement.Style>()
                PreApprovalElementAlert(id = id, style = style, icon = icon, text = text)
            }
            PreApprovalElement.Type.BUTTON -> {
                val action = map["action"]?.parseEnumOrNull<PreApprovalElementButton.ButtonAction>()
                val actionSettings = map["actionSettings"]
                val href = map["href"]
                PreApprovalElementButton(id = id, action = action, actionSettings = actionSettings, href = href, icon = icon, text = text)
            }
            PreApprovalElement.Type.UNKNOWN -> {
                // keep base instance for forward compatibility
                PreApprovalElement(id = id, type = PreApprovalElement.Type.UNKNOWN, icon = icon, text = text)
            }
        }
    }

    override fun write(out: JsonWriter, value: PreApprovalElement?) {
        // not used as of now
        out.nullValue()
    }

    // ---- helpers ----
    private fun parseScreenType(typeStr: String?): PreApprovalElement.Type {
        return parseEnumWithFallback(typeStr, "PreApproval element type")
    }
}
