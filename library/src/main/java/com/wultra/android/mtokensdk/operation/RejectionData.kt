/*
 * Copyright 2024 Wultra s.r.o.
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

/**
 * Wrapper class for operation rejection reason
 * RejectionReason enum or custom String reason can be used
 *
 * @property serialized The rejection reason.
 */
class RejectionData {

    /**
     * The reason of the rejection.
     */
    val serialized: String

    /**
     * Constructs a [RejectionData] with the specified reason.
     *
     * Represents a custom reason for rejection, allowing for flexibility in specifying rejection reasons.
     * @param reason The reason for rejection as a [String], e.g., `POSSIBLE_FRAUD`.
     */
    constructor(reason: String) {
        this.serialized = reason
    }

    /**
     * Constructs a [RejectionData] with the specified [RejectionReason].
     *
     * @param reason The [RejectionReason] for rejection.
     */
    constructor(reason: RejectionReason) {
        this.serialized = reason.reason
    }
}
