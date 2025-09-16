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
import com.google.gson.reflect.TypeToken
import com.wultra.android.mtokensdk.api.operation.model.OperationUIData
import com.wultra.android.mtokensdk.api.operation.model.PostApprovalScreen
import com.wultra.android.mtokensdk.api.operation.model.preapproval.PreApprovalScreen
import java.lang.reflect.Type

/**
 * Gson deserializer for [OperationUIData]
 */
class OperationUIDataDeserializer : JsonDeserializer<OperationUIData> {
    override fun deserialize(json: JsonElement?, typeOfT: Type?, ctx: JsonDeserializationContext): OperationUIData? {
        if (json == null || !json.isJsonObject) return null
        val obj = json.asJsonObject

        val flipButtons = obj.get("flipButtons")?.takeIf { it.isJsonPrimitive }?.asBoolean
        val blockApprovalOnCall = obj.get("blockApprovalOnCall")?.takeIf { it.isJsonPrimitive }?.asBoolean

        // Prefer plural. If it exists & is an array, parse and return early for preApprovalScreens.
        val listType = object : TypeToken<List<PreApprovalScreen>>() {}.type
        val preApprovalScreens: List<PreApprovalScreen>? = when {
            obj.has("preApprovalScreens") && obj["preApprovalScreens"].isJsonArray ->
                ctx.deserialize(obj["preApprovalScreens"], listType)
            obj.has("preApprovalScreen") && obj["preApprovalScreen"].isJsonObject ->
                listOf(ctx.deserialize(obj["preApprovalScreen"], PreApprovalScreen::class.java))
            else -> null
        }

        val postApprovalScreen: PostApprovalScreen? = obj.get("postApprovalScreen")
            ?.takeIf { it.isJsonObject }
            ?.let { el -> ctx.deserialize(el, PostApprovalScreen::class.java) }

        return OperationUIData(
            flipButtons = flipButtons,
            blockApprovalOnCall = blockApprovalOnCall,
            preApprovalScreens = preApprovalScreens,
            postApprovalScreen = postApprovalScreen
        )
    }
}
