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

        val type = try { Attribute.Type.valueOf(attrMap["type"] as? String ?: "UNKNOWN") } catch (e: Throwable) { Attribute.Type.UNKNOWN }
        val id: String = attrMap["id"] as? String ?: return null
        val label: String = attrMap["label"] as? String ?: return null
        val labelObject = Attribute.Label(id, label)
        val builder = AttributeBuilder(type, labelObject, attrMap, partyInfoMap)
        return builder.build() ?: Attribute(Attribute.Type.UNKNOWN, labelObject)
    }

    private class AttributeBuilder(val type: Attribute.Type, val label: Attribute.Label, val map: Map<String, Any?>, val partyInfoMap: Map<String, String>) {

        inline fun <reified T>attr(key: String): T? = map[key] as? T

        fun build(): Attribute? {
            return when (type) {
                Attribute.Type.AMOUNT -> {
                    val amount: BigDecimal = attr("amount") ?: return null
                    val currency: String = attr("currency") ?: return null
                    AmountAttribute(amount, currency, attr("amountFormatted"), attr("currencyFormatted"), attr("valueFormatted"), label)
                }
                Attribute.Type.KEY_VALUE -> KeyValueAttribute(attr("value") ?: return null, label)
                Attribute.Type.NOTE -> NoteAttribute(attr("note") ?: return null, label)
                Attribute.Type.HEADING -> HeadingAttribute(label)
                Attribute.Type.PARTY_INFO -> PartyInfoAttribute(PartyInfoAttribute.PartyInfo(partyInfoMap), label)
                Attribute.Type.AMOUNT_CONVERSION -> ConversionAttribute(
                    attr("dynamic") ?: return null,
                    ConversionAttribute.Money(attr("sourceAmount") ?: return null, attr("sourceCurrency") ?: return null, attr("sourceAmountFormatted"), attr("sourceCurrencyFormatted"), attr("sourceValueFormatted")),
                    ConversionAttribute.Money(attr("targetAmount") ?: return null, attr("targetCurrency") ?: return null, attr("targetAmountFormatted"), attr("targetCurrencyFormatted"), attr("targetValueFormatted")),
                    label
                )
                Attribute.Type.IMAGE -> ImageAttribute(attr("thumbnailUrl") ?: return null, attr("originalUrl"), label)
                Attribute.Type.UNKNOWN -> Attribute(Attribute.Type.UNKNOWN, label)
            }
        }
    }

    override fun write(writer: JsonWriter, value: Attribute?) {
        // nothing - we don't care about serialization
    }
}
