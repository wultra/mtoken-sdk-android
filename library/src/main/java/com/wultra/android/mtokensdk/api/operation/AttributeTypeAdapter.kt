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

        val attrMap = mutableMapOf<String, Any?>()
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
                    partyInfoMap[name] = reader.nextString()
                } else {
                    if (name != "partyInfo") {
                        attrMap[name] = when (reader.peek()) {
                            JsonToken.STRING -> reader.nextString()
                            JsonToken.NUMBER -> BigDecimal(reader.nextDouble())
                            JsonToken.BOOLEAN -> reader.nextBoolean()
                            JsonToken.NULL -> null
                            else -> null
                        }
                    }
                }
            }
        } while (true)

        fun <T>attr(key: String): T? {
            return attrMap[key] as? T
        }

        val type = try { Attribute.Type.valueOf(attr("type") ?: "UNKNOWN") } catch (e: Throwable) { Attribute.Type.UNKNOWN }
        val id: String? = attr("id")
        val label: String? = attr("label")

        val labelObject = if(id != null && label != null) {
            Attribute.Label(id, label)
        } else {
            null
        }

        return when (type) {
            Attribute.Type.AMOUNT -> AmountAttribute(attr("amount"), attr("currency"), attr("amountFormatted"), attr("currencyFormatted"), labelObject)
            Attribute.Type.KEY_VALUE -> KeyValueAttribute(attr("value"), labelObject)
            Attribute.Type.NOTE -> NoteAttribute(attr("note"), labelObject)
            Attribute.Type.HEADING -> HeadingAttribute(labelObject)
            Attribute.Type.PARTY_INFO -> PartyInfoAttribute(PartyInfoAttribute.PartyInfo(partyInfoMap), labelObject)
            Attribute.Type.AMOUNT_CONVERSION -> ConversionAttribute(
                attr("dynamic") ?: false,
                ConversionAttribute.Money(attr("sourceAmount"), attr("sourceCurrency"), attr("sourceAmountFormatted"), attr("sourceCurrencyFormatted")),
                ConversionAttribute.Money(attr("targetAmount"), attr("targetCurrency"), attr("targetAmountFormatted"), attr("targetCurrencyFormatted")),
                labelObject
            )
            Attribute.Type.UNKNOWN -> Attribute(Attribute.Type.UNKNOWN, labelObject)
        }
    }

    override fun write(writer: JsonWriter, value: Attribute?) {
        // nothing - we don't care about serialization
    }
}