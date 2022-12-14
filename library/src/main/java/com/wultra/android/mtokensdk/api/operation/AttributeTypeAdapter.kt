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

package com.wultra.android.mtokensdk.api.operation

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import com.wultra.android.mtokensdk.api.operation.model.*
import java.math.BigDecimal

/**
 * Type adapter for deserializing sealed class Attribute with its hierarchy types.
 */
internal class AttributeTypeAdapter : TypeAdapter<Attribute>() {

    override fun read(reader: JsonReader): Attribute? {
        var token = reader.peek()
        if (token == JsonToken.NULL) {
            reader.nextNull()
            return null
        }
        reader.beginObject()

        val attrMap = mutableMapOf<String, String>()
        var inPartyInfo = false
        val partyInfoMap = mutableMapOf<String, String>()

        do {
            token = reader.peek()
            if (token == JsonToken.NULL) {
                reader.nextNull()
                return null
            }
            if (token == JsonToken.BEGIN_OBJECT) {
                if (attrMap["type"] == "PARTY_INFO") {
                    inPartyInfo = true
                }
                reader.beginObject()
            }
            if (token == JsonToken.END_OBJECT) {
                if (inPartyInfo) {
                    inPartyInfo = false
                    reader.endObject()
                } else {
                    reader.endObject()
                    break
                }
            }
            if (token == JsonToken.NAME) {
                val name = reader.nextName()
                if (inPartyInfo) {
                    partyInfoMap.put(name, reader.nextString())
                } else {
                    if (name != "partyInfo") {
                        attrMap.put(name, reader.nextString())
                    }
                }
            }
        } while (true)
        val type = attrMap["type"]
        val id = attrMap["id"]
        val label = attrMap["label"]

        val labelObject = if(id != null && label != null) {
            Attribute.Label(id, label)
        } else {
            null
        }

        when (type) {
            "AMOUNT" -> return AmountAttribute(BigDecimal(attrMap["amount"]), attrMap["currency"], attrMap["amountFormatted"], attrMap["currencyFormatted"], labelObject)
            "KEY_VALUE" -> return KeyValueAttribute(attrMap["value"], labelObject)
            "NOTE" -> return NoteAttribute(attrMap["note"], labelObject)
            "HEADING" -> return HeadingAttribute(labelObject)
            "PARTY_INFO" -> return PartyInfoAttribute(PartyInfoAttribute.PartyInfo(partyInfoMap), labelObject)
        }
        return null
    }

    override fun write(writer: JsonWriter, value: Attribute?) {
        // nothing - we don't care about serialization
    }
}