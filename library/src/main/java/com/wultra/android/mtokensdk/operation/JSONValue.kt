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

package com.wultra.android.mtokensdk.operation

import com.google.gson.JsonElement
import com.google.gson.JsonParser

/**
 * JSONValue - helper class for deserializing generic or unknown post-approval screens
 */
sealed class JSONValue {
    data class JSONString(val value: String) : JSONValue()
    data class JSONBool(val value: Boolean) : JSONValue()
    data class JSONObject(val value: Map<String, JSONValue>) : JSONValue()
    data class JSONArray(val value: List<JSONValue>) : JSONValue()
    data class JSONNumber(val value: Number) : JSONValue() 
    object JSONNull : JSONValue()

    operator fun get(key: String): JSONValue? {
        return if (this is JSONObject) {
            value[key]
        } else null
    }

    companion object {
        fun parse(jsonString: String): JSONValue {
            val jsonElement = JsonParser.parseString(jsonString)
            return parseValue(jsonElement)
        }

        fun parse(jsonElement: JsonElement): JSONValue {
            return parseValue(jsonElement)
        }

        private fun parseValue(jsonElement: JsonElement): JSONValue {
            return when {
                jsonElement.isJsonPrimitive && jsonElement.asJsonPrimitive.isString ->
                    JSONString(jsonElement.asString)
                jsonElement.isJsonPrimitive && jsonElement.asJsonPrimitive.isNumber ->



                    JSONNumber(jsonElement.asNumber)
                jsonElement.isJsonPrimitive && jsonElement.asJsonPrimitive.isBoolean ->
                    JSONBool(jsonElement.asBoolean)
                jsonElement.isJsonObject ->
                    JSONObject(jsonElement.asJsonObject.entrySet().associate { (k, v) -> k to parseValue(v) })
                jsonElement.isJsonArray ->
                    JSONArray(jsonElement.asJsonArray.map { parseValue(it) })
                jsonElement.isJsonNull ->
                    JSONNull
                else ->
                    throw IllegalArgumentException("Invalid JSON value")
            }
        }
    }
}
