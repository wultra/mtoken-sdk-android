/*
 * Copyright (c) 2022, Wultra s.r.o. (www.wultra.com).
 *
 * All rights reserved. This source code can be used only for purposes specified
 * by the given license contract signed by the rightful deputy of Wultra s.r.o.
 * This source code can be used only by the owner of the license.
 *
 * Any disputes arising in respect of this agreement (license) shall be brought
 * before the Municipal Court of Prague.
 */

package com.wultra.android.mtokensdk.operation

import com.google.gson.GsonBuilder
import com.wultra.android.mtokensdk.api.operation.AttributeTypeAdapter
import com.wultra.android.mtokensdk.api.operation.OperationHistoryEntryDeserializer
import com.wultra.android.mtokensdk.api.operation.ZonedDateTimeDeserializer
import com.wultra.android.mtokensdk.api.operation.model.Attribute
import com.wultra.android.mtokensdk.api.operation.model.OperationHistoryEntry
import org.threeten.bp.ZonedDateTime

class OperationsUtils {
    companion object {
        /**
         * Default GSON builder that is used when null is passed by the integrator.
         *
         * This builder provides parsing logic for:
         *  - attributes in UserOperation
         *  - time deserializer
         *  - operation history entry serializer
         *
         *  If you plan to provide your own adapters or deserializer, we recommend adding it to this
         *  default builder.
         */
        fun defaultGsonBuilder(): GsonBuilder {
            val builder = GsonBuilder()
            builder.registerTypeHierarchyAdapter(Attribute::class.java, AttributeTypeAdapter())
            builder.registerTypeAdapter(ZonedDateTime::class.java, ZonedDateTimeDeserializer())
            builder.registerTypeAdapter(OperationHistoryEntry::class.java, OperationHistoryEntryDeserializer())
            return builder
        }
    }
}