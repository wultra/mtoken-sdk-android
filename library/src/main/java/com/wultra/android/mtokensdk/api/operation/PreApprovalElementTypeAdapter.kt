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

/**
 * Type adapter for deserializing [PreApprovalElement] with concrete subtypes.
 * - Safely handles unknown fields and unknown element types.
 * - Serialization is not used -> write() is a no-op.
 */
internal class PreApprovalElementTypeAdapter : TypeAdapter<PreApprovalElement>() {

    override fun read(reader: JsonReader): PreApprovalElement? {
        var token = reader.peek()
        if (token == JsonToken.NULL) {
            reader.nextNull()
            return null
        }

        // We parse the object into a light map, then build the proper subtype.
        reader.beginObject()

        var typeStr: String? = null
        var id: String? = null
        var icon: String? = null
        var text: String? = null

        // subtype-specific
        var styleStr: String? = null
        var actionStr: String? = null
        var href: String? = null

        while (reader.hasNext()) {
            token = reader.peek()
            if (token == JsonToken.NAME) {
                when (reader.nextName()) {
                    "type" -> typeStr = reader.nextStringOrNull()
                    "id" -> id = reader.nextStringOrNull()
                    "icon" -> icon = reader.nextStringOrNull()
                    "text" -> text = reader.nextStringOrNull()

                    // alert/listItem
                    "style" -> styleStr = reader.nextStringOrNull()

                    // button
                    "action" -> actionStr = reader.nextStringOrNull()
                    "href" -> href = reader.nextStringOrNull()

                    else -> reader.skipValueSafe()
                }
            } else {
                reader.skipValueSafe()
            }
        }

        reader.endObject()

        val type = typeStr?.let {
            runCatching { PreApprovalElement.ElementType.valueOf(it) }.getOrDefault(PreApprovalElement.ElementType.UNKNOWN)
        } ?: PreApprovalElement.ElementType.UNKNOWN

        return when (type) {
            PreApprovalElement.ElementType.LIST_ITEM -> {
                val style = styleStr?.let { runCatching { PreApprovalElement.ElementStyle.valueOf(it) }.getOrNull() }
                PreApprovalElementListItem(id = id, style = style, icon = icon, text = text)
            }
            PreApprovalElement.ElementType.ALERT -> {
                val style = styleStr?.let { runCatching { PreApprovalElement.ElementStyle.valueOf(it) }.getOrNull() }
                PreApprovalElementAlert(id = id, style = style, icon = icon, text = text)
            }
            PreApprovalElement.ElementType.BUTTON -> {
                val action = actionStr?.let { runCatching { PreApprovalElementButton.ButtonAction.valueOf(it) }.getOrNull() }
                PreApprovalElementButton(
                    id = id,
                    action = action,
                    href = href,
                    icon = icon,
                    text = text
                )
            }
            PreApprovalElement.ElementType.UNKNOWN -> {
                // keep base instance; allows forward-compat
                PreApprovalElement(id = id, type = PreApprovalElement.ElementType.UNKNOWN, icon = icon, text = text)
            }
        }
    }

    override fun write(out: JsonWriter, value: PreApprovalElement?) {
        // not used as of now
        out.nullValue()
    }

    // ---- helpers ----

    private fun JsonReader.nextStringOrNull(): String? =
        if (peek() == JsonToken.NULL) { nextNull(); null } else nextString()

    private fun JsonReader.skipValueSafe() {
        when (peek()) {
            JsonToken.BEGIN_ARRAY -> {
                beginArray()
                while (hasNext()) skipValueSafe()
                endArray()
            }
            JsonToken.BEGIN_OBJECT -> {
                beginObject()
                while (hasNext()) {
                    if (peek() == JsonToken.NAME) {
                        nextName()
                        skipValueSafe()
                    } else {
                        skipValue()
                    }
                }
                endObject()
            }
            else -> skipValue()
        }
    }
}
