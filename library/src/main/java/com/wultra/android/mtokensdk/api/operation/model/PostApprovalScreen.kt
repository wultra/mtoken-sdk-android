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

package com.wultra.android.mtokensdk.api.operation.model

import com.google.gson.annotations.SerializedName
import com.wultra.android.mtokensdk.operation.JSONValue

/**
 *  PostApproval classes define behaviour of screens after approval of the operation
 *  PostApprovalScreen is the base class for Post Approval screens classes
 *
 * `type` define different kind of data which can be passed with operation
 *  and shall be displayed before operation is confirmed
 */

open class PostApprovalScreen(
    /**
     * type of PostApprovalScreen is presented with different classes (Starting with `WMTPreApprovalScreen*`)
     */
    val type: Type
) {

    enum class Type() {
        REVIEW,
        REDIRECT,
        GENERIC,
        UNKNOWN
    }
}

/**
 * Review screen shows the operation attributes
 *
 * @property heading - Heading of the post-approval screen
 * @property message - Message to the user
 * @property payload - Payload with data about action after the operation
 */
class PostApprovalScreenReview(
    val heading: String,
    val message: String,
    val payload: ReviewPostApprovalScreenPayload
) : PostApprovalScreen(type = Type.REVIEW)

/**
 * Redirect screen prepares for merchant redirect
 */
class PostApprovalScreenRedirect(
    val heading: String,
    val message: String,
    val payload: RedirectPostApprovalScreenPayload
) : PostApprovalScreen(type = Type.REDIRECT)

/**
 * Generic screen may contain any object
 */
class PostApprovalScreenGeneric(
    val heading: String,
    val message: String,
    val payload: JSONValue
) : PostApprovalScreen(type = Type.GENERIC)

open class PostApprovalScreenPayload

/**
 *  Redirect payload
 *
 *  @property text - Label of the redirect URL
 *  @property url - URL to redirect, might be a website or application
 *  @property countdown - Time in seconds before automatic redirect
 */
class RedirectPostApprovalScreenPayload(
    @SerializedName("redirectText")
    val text: String,
    @SerializedName("redirectUrl")
    val url: String,
    val countdown: Int
) : PostApprovalScreenPayload()

/**
 *  Review payload
 *
 *  @property attributes - List of the operation attributes
 */
class ReviewPostApprovalScreenPayload(
    val attributes: List<Attribute>
) : PostApprovalScreenPayload()


