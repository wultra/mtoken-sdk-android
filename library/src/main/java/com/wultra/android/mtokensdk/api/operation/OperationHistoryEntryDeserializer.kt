/*
 * Copyright (c) 2021, Wultra s.r.o. (www.wultra.com).
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