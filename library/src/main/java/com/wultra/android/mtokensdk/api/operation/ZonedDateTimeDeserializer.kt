/*
 * Copyright (c) 2018, Wultra s.r.o. (www.wultra.com).
 *
 * All rights reserved. This source code can be used only for purposes specified
 * by the given license contract signed by the rightful deputy of Wultra s.r.o.
 * This source code can be used only by the owner of the license.
 *
 * Any disputes arising in respect of this agreement (license) shall be brought
 * before the Municipal Court of Prague.
 */

package com.wultra.android.mtokensdk.api.operation

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter
import java.lang.reflect.Type

/**
 * Gson deserializer for [ZonedDateTime].
 *
 * @author Tomas Kypta, tomas.kypta@wultra.com
 */
class ZonedDateTimeDeserializer : JsonDeserializer<ZonedDateTime> {

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): ZonedDateTime {
        val jsonPrimitive = json.getAsJsonPrimitive()
        try {

            // if provided as String - '2011-12-03T10:15:30+01:00[Europe/Paris]'
            if (jsonPrimitive.isString) {
                // fix for incorrect values from server see https://github.com/wultra/powerauth-webflow/issues/432
                val fixedStr = jsonPrimitive.asString.replace(Regex("\\+([0-9][0-9])([0-9][0-9])$"), transform = { matchResult ->
                    "+${matchResult.groupValues[1]}:${matchResult.groupValues[2]}"
                })
                return ZonedDateTime.parse(fixedStr, DateTimeFormatter.ISO_ZONED_DATE_TIME);
            }

            // if provided as Long
            if (jsonPrimitive.isNumber) {
                return ZonedDateTime.ofInstant(Instant.ofEpochMilli(jsonPrimitive.asLong), ZoneId.systemDefault());
            }

        } catch (e: Exception) {
            throw JsonParseException("Unable to parse ZonedDateTime", e)
        }
        throw JsonParseException("Unable to parse ZonedDateTime");
    }
}