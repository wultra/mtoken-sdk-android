/*
 * Copyright (c) 2025, Wultra s.r.o. (www.wultra.com).
 *
 * All rights reserved. This source code can be used only for purposes specified
 * by the given license contract signed by the rightful deputy of Wultra s.r.o.
 * This source code can be used only by the owner of the license.
 *
 * Any disputes arising in respect of this agreement (license) shall be brought
 * before the Municipal Court of Prague.
 */

package com.wultra.android.mtokensdk.api.operation.utils

import com.google.gson.*
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.wultra.android.mtokensdk.log.WMTLogger
import java.lang.reflect.Type

/** Safe primitive accessors */
internal fun JsonObject.getAsStringSafe(name: String): String? =
    this.get(name)?.asJsonPrimitiveOrNull()?.takeIf { it.isString }?.asString

internal fun JsonObject.getAsBooleanSafe(name: String): Boolean? =
    this.get(name)?.asJsonPrimitiveOrNull()?.takeIf { it.isBoolean }?.asBoolean

internal fun JsonElement.asJsonPrimitiveOrNull(): JsonPrimitive? =
    takeIf { it.isJsonPrimitive }?.asJsonPrimitive

internal fun JsonElement.asBooleanStrict(): Boolean? =
    takeIf { isJsonPrimitive && asJsonPrimitive.isBoolean }?.asBoolean

// Enum helpers
internal inline fun <reified T : Enum<T>> String.parseEnumOrNull(): T? {
    return try {
        enumValueOf<T>(this)
    } catch (_: IllegalArgumentException) {
        WMTLogger.w("Unknown enum value '$this' for ${T::class.simpleName}")
        null
    } catch (ex: Exception) {
        WMTLogger.w("Failed to parse enum ${T::class.simpleName} from value '$this': ${ex.message}")
        null
    }
}

/** Parses an enum of type [T] from [raw], logging and falling back to UNKNOWN if missing or invalid. */
internal inline fun <reified T : Enum<T>> parseEnumWithFallback(
    rawValue: String?,
    label: String
): T {
    if (rawValue == null) {
        WMTLogger.w("$label not provided — falling back to UNKNOWN")
        return enumValueOfOrUnknown<T>()
    }

    return try {
        enumValueOf<T>(rawValue)
    } catch (_: Exception) {
        WMTLogger.w("Unknown $label '$rawValue' — using UNKNOWN")
        enumValueOfOrUnknown<T>()
    }
}

/** Returns the `UNKNOWN` enum constant of the given type, or throws if not present. */
internal inline fun <reified T : Enum<T>> enumValueOfOrUnknown(): T =
    enumValues<T>().firstOrNull { it.name == "UNKNOWN" }
        ?: error("Enum ${T::class.simpleName} must define UNKNOWN value")

/**
 * Skips any nested value while logging what we are skipping.
 * Helpful when payload contains forward-compatible fields.
 */
internal fun JsonReader.skipValueSafe(key: String? = null) {
    when (peek()) {
        JsonToken.BEGIN_ARRAY -> {
            WMTLogger.w("Skipping unexpected JSON array: '$key'")
            beginArray(); while (hasNext()) skipValueSafe(); endArray()
        }
        JsonToken.BEGIN_OBJECT -> {
            WMTLogger.w("Skipping unexpected JSON object: '$key'")
            beginObject()
            while (hasNext()) {
                if (peek() == JsonToken.NAME) {
                    val name = nextName()
                    skipValueSafe(name)
                } else {
                    skipValue()
                }
            }
            endObject()
        }
        else -> {
            WMTLogger.w("Skipping unexpected JSON value: $key")
            skipValue()
        }
    }
}

/**
 * Read a *shallow* object into a Map<String, String?>.
 * - Only string/null primitives are captured.
 * - Non-string values (arrays/objects/numbers/bools) are logged & skipped!
 */
internal fun JsonReader.readShallowStringMap(): Map<String, String?> {
    val out = mutableMapOf<String, String?>()
    beginObject()
    while (hasNext()) {
        val name = nextName()
        when (peek()) {
            JsonToken.NULL -> { nextNull(); out[name] = null }
            JsonToken.STRING -> out[name] = nextString()
            else -> { // numbers, booleans, arrays, objects → skip but log
                skipValueSafe(name)
            }
        }
    }
    endObject()
    return out
}

internal fun <T> safeDeserializeObject(
    ctx: JsonDeserializationContext,
    element: JsonElement?,
    type: Type
): T? {
    if (element == null || !element.isJsonObject) {
        WMTLogger.w("Expected JSON object for $type but got: $element")
        return null
    }
    return try {
        ctx.deserialize<T>(element, type)
    } catch (_: Throwable) {
        WMTLogger.d("Failed to deserialize $type from JSON: $element")
        null
    }
}

internal fun <T> safeDeserializeArray(
    ctx: JsonDeserializationContext,
    element: JsonElement?,
    type: Type
): T? {
    if (element == null || !element.isJsonArray) {
        WMTLogger.w("Expected JSON array for $type but got: $element")
        return null
    }
    return try {
        val deserialized: T = ctx.deserialize(element, type)
        // Automatically collapse empty arrays to null if the type is a collection
        if (deserialized is Collection<*> && deserialized.isEmpty()) null else deserialized
    } catch (_: Throwable) {
        WMTLogger.d("Failed to deserialize $type from JSON: $element")
        null
    }
}
