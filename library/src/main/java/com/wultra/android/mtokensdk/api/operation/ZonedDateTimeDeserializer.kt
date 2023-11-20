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

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter
import java.lang.reflect.Type

/**
 * Gson deserializer for [ZonedDateTime].
 */
internal class ZonedDateTimeDeserializer : JsonDeserializer<ZonedDateTime>, JsonSerializer<ZonedDateTime> {

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): ZonedDateTime {
        val jsonPrimitive = json.asJsonPrimitive
        try {

            // if provided as String - '2011-12-03T10:15:30+01:00[Europe/Paris]'
            if (jsonPrimitive.isString) {
                // fix for incorrect values from server see https://github.com/wultra/powerauth-webflow/issues/432
                val fixedStr = jsonPrimitive.asString.replace(Regex("\\+([0-9][0-9])([0-9][0-9])$"), transform = { matchResult ->
                    "+${matchResult.groupValues[1]}:${matchResult.groupValues[2]}"
                })
                return ZonedDateTime.parse(fixedStr, DateTimeFormatter.ISO_ZONED_DATE_TIME)
            }

            // if provided as Long
            if (jsonPrimitive.isNumber) {
                return ZonedDateTime.ofInstant(Instant.ofEpochMilli(jsonPrimitive.asLong), ZoneId.systemDefault())
            }
        } catch (e: Exception) {
            throw JsonParseException("Unable to parse ZonedDateTime", e)
        }
        throw JsonParseException("Unable to parse ZonedDateTime")
    }

    override fun serialize(src: ZonedDateTime?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        // Custom format for ZonedDateTime
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")

        // Format the ZonedDateTime using the defined format
        val formattedZonedDateTime = src?.format(formatter)

        // Create a JsonPrimitive from the formatted string
        return JsonPrimitive(formattedZonedDateTime)
    }
}
