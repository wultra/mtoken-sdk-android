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

package com.wultra.android.mtokensdk.api.operation.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.wultra.android.mtokensdk.operation.expiration.ExpirableOperation
import org.threeten.bp.ZonedDateTime

/**
 * [UserOperation] is an object returned from the backend that can be either approved or rejected.
 * It is usually visually presented to the user as a non-editable form with information, about
 * the real-world operation (for example login or payment).
 */
open class UserOperation(
        /**
         * Unique operation identifier
         */
        @SerializedName("id")
        override val id: String,

        /**
         * System name of the operation (for example login).
         *
         * Name of the operation shouldn't be visible to the user. You can use it to distinguish how
         * the operation will be presented. (for example when the template for login is different than payment).
         */
        @SerializedName("name")
        val name: String,

        /**
         * Actual data that will be signed.
         *
         * This shouldn't be visible to the user.
         */
        @SerializedName("data")
        override val data: String,

        /**
         * Date and time when the operation was created.
         */
        @SerializedName("operationCreated")
        val created: ZonedDateTime,

        /**
         * Date and time when the operation will expire.
         */
        @SerializedName("operationExpires")
        override val expires: ZonedDateTime,

        /**
         * Data that should be presented to the user.
         */
        @SerializedName("formData")
        val formData: FormData,

        /**
         * Allowed signature types.
         *
         * For example in some cases, biometric authentication might not available for security reasons.
         */
        @SerializedName("allowedSignatureType")
        val allowedSignatureType: AllowedSignatureType,

        /**
         * UI data to be shown
         *
         * Accompanying information about the operation additional UI which should be presented such as
         * Pre-Approval Screen or Post-Approval Screen
         */
        @SerializedName("ui")
        val ui: OperationUIData?) : IOperation, ExpirableOperation

/**
 * Model class wrapping allowed signature types.
 */
data class AllowedSignatureType(

        /**
         * If operation should be signed with 1 or 2 factor authentication
         */
        @SerializedName("type")
        val type: Type,

        /**
         * What factors ("password" or/and "biometry") can be used for signing this operation.
         */
        @SerializedName("variants")
        val factors: List<Factor> = emptyList()) {

    /**
     * Check if biometry factor is allowed for the signature type of an operation.
     */
    fun isBiometryAllowed(): Boolean {
        return factors.contains(Factor.POSSESSION_BIOMETRY)
    }

    /**
     * Signature types.
     */
    enum class Type(val type: String) {

        @SerializedName("1FA")
        MULTIFACTOR_1FA("1FA"),

        @SerializedName("2FA")
        MULTIFACTOR_2FA("2FA"),

        // 3FA is not used in mtoken
        // @SerializedName("3FA")
        // MULTIFACTOR_3FA("3FA"),

        @SerializedName("ECDSA")
        ASYMMETRIC_ECDSA("ECDSA");
    }

    /**
     * Signature factor
     */
    enum class Factor(val variant: String) {

        @SerializedName("possession_knowledge")
        POSSESSION_KNOWLEDGE("possession_knowledge"),

        @SerializedName("possession_biometry")
        POSSESSION_BIOMETRY("possession_biometry"),

        @SerializedName("possession")
        POSSESSION("possession")
    }
}

/**
 * Operation data, that should be visible to the user.
 *
 * Note that the data returned from the server are localized based on the [IOperationsService.acceptLanguage] property.
 */
data class FormData(

        /**
         * Title of the operation
         */
        @SerializedName("title")
        val title: String,

        /**
         * Message for the user
         */
        @SerializedName("message")
        val message: String,

        /**
         * Other attributes.
         *
         * Note that attributes can be presented with different classes (Starting with Attribute*) based on the attribute type.
         */
        @SerializedName("attributes")
        val attributes: List<Attribute>)

data class OperationUIData(
        /**
         * Order of the buttons
         */
        @SerializedName("flipButtons")
        val flipButtons: Boolean?,

        /**
         * Block approval during incoming phone call
         */
        @SerializedName("blockApprovalOnCall")
        val blockApprovalOnCall: Boolean?,

        /**
         * Other attributes.
         *
         * Note that attributes can be presented with different classes (Starting with Attribute*) based on the attribute type.
         */
        @SerializedName("preApprovalScreen")
        val preApprovalScreen: PreApprovalScreen?,

        /**
         * Other attributes.
         *
         * Note that attributes can be presented with different classes (Starting with Attribute*) based on the attribute type.
         */
        @SerializedName("preApprovalScreen")
        val postApprovalScreen: PostApprovalScreen?)