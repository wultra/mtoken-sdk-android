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