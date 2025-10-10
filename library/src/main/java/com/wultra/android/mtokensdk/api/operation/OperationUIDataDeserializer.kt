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

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.wultra.android.mtokensdk.api.operation.model.OperationUIData
import com.wultra.android.mtokensdk.api.operation.model.PostApprovalScreen
import com.wultra.android.mtokensdk.api.operation.model.preapproval.PreApprovalScreen
import com.wultra.android.mtokensdk.api.operation.utils.asBooleanStrict
import com.wultra.android.mtokensdk.api.operation.utils.safeDeserializeArray
import com.wultra.android.mtokensdk.api.operation.utils.safeDeserializeObject
import java.lang.reflect.Type

/**
 * Gson deserializer for [OperationUIData]
 */
class OperationUIDataDeserializer : JsonDeserializer<OperationUIData> {

    override fun deserialize(json: JsonElement?, typeOfT: Type?, ctx: JsonDeserializationContext): OperationUIData? {
        if (json == null || !json.isJsonObject) return null
        val obj = json.asJsonObject

        val flipButtons = obj.get("flipButtons")?.asBooleanStrict()
        val blockApprovalOnCall = obj.get("blockApprovalOnCall")?.asBooleanStrict()

        val preApprovalScreens = parsePreApprovalScreens(obj, ctx)
        val postApprovalScreen = obj.get("postApprovalScreen")
            ?.let { el -> safeDeserializeObject<PostApprovalScreen>(ctx, el, PostApprovalScreen::class.java) }

        return OperationUIData(
            flipButtons = flipButtons,
            blockApprovalOnCall = blockApprovalOnCall,
            preApprovalScreens = preApprovalScreens,
            postApprovalScreen = postApprovalScreen
        )
    }

    private fun parsePreApprovalScreens(
        obj: JsonObject,
        ctx: JsonDeserializationContext
    ): List<PreApprovalScreen>? {
        val listType = object : TypeToken<List<PreApprovalScreen>>() {}.type

        // 1) Prefer plural array
        obj.get("preApprovalScreens")?.let { el ->
            safeDeserializeArray<List<PreApprovalScreen>>(ctx, el, listType)?.let { return it }
        }

        // 2) Legacy singular object → wrap as list
        obj.get("preApprovalScreen")?.let { el ->
            safeDeserializeObject<PreApprovalScreen>(ctx, el, PreApprovalScreen::class.java)?.let { single ->
                return listOf(single)
            }
        }

        // 3) Nothing valid present
        return null
    }
}
