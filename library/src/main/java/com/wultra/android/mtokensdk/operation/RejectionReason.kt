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

/**
 * Reason for rejecting a pending operation.
 *
 * @property reason Reason identifier.
 */
enum class RejectionReason(val reason: String) {

    /**
     * The data in the operation are incorrect.
     */
    INCORRECT_DATA("INCORRECT_DATA"),

    /**
     * The operation is unexpected. This might indicate a fraudulent operation.
     */
    UNEXPECTED_OPERATION("UNEXPECTED_OPERATION"),

    /**
     * The reason is unknown. User probably doesn't want to indicate the reason.
     */
    UNKNOWN("UNKNOWN")
}