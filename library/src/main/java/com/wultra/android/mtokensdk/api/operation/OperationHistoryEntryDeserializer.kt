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
import com.wultra.android.mtokensdk.api.operation.model.OperationHistoryEntry
import com.wultra.android.mtokensdk.api.operation.model.OperationHistoryEntryStatus
import com.wultra.android.mtokensdk.api.operation.model.UserOperation
import java.lang.reflect.Type


/**
 * Custom deserializer that extracts status from the returned object into its own property
 */
internal class OperationHistoryEntryDeserializer: JsonDeserializer<OperationHistoryEntry> {

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): OperationHistoryEntry {
        val jsonObject = json.asJsonObject
        return OperationHistoryEntry(
            // extract the status into its own property
            OperationHistoryEntryStatus.valueOf(jsonObject.get("status").asString),
            // serialize the rest as a normal UserOperation
            context.deserialize(json, UserOperation::class.java)
        )
    }
}