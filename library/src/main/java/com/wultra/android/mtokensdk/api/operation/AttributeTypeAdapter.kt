/*
 * Copyright (c) 2020, Wultra s.r.o. (www.wultra.com).
 *
 * All rights reserved. This source code can be used only for purposes specified
 * by the given license contract signed by the rightful deputy of Wultra s.r.o.
 * This source code can be used only by the owner of the license.
 *
 * Any disputes arising in respect of this agreement (license) shall be brought
 * before the Municipal Court of Prague.
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
        when (type) {
            "AMOUNT" -> return AmountAttribute(id, label, BigDecimal(attrMap["amount"]), attrMap["currency"], attrMap["amountFormatted"], attrMap["currencyFormatted"])
            "KEY_VALUE" -> return KeyValueAttribute(id, label, attrMap["value"])
            "NOTE" -> return NoteAttribute(id, label, attrMap["note"])
            "HEADING" -> return HeadingAttribute(id, label)
            "PARTY_INFO" -> {
                return PartyInfoAttribute(id, label, PartyInfo(partyInfoMap))
            }
        }
        return null
    }

    override fun write(writer: JsonWriter, value: Attribute?) {
        // nothing - we don't care about serialization
    }
}