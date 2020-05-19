/*
 * Copyright (c) 2020, Wultra s.r.o. (www.wultra.com).
 *
 * All rights reserved. This source code can be used only for purposes specified
 * by the given license contract signed by the rightful deputy of Wultra s.r.o.
 * This source code can be used only by the owner of the license.
 *
 * Any disputes arising in respect of this agreement (license) shall be brought
 * before the Municipal Court of Prague.
 */

package com.wultra.android.mtokensdk.api.operation.model

import com.google.gson.annotations.SerializedName
import org.threeten.bp.ZonedDateTime

/**
 * [UserOperation] is an object returned from the backend that can be either approved or rejected.
 * It is usually visually presented to the user as a non-editable form with information, about
 * the real-world operation (for example login or payment).
 */
data class UserOperation(
        /**
         * Unique operation identifier
         */
        val id: String,

        /**
         * System name of the operation (for example login).
         *
         * Name of the operation shouldn't be visible to the user. You can use it to distinguish how
         * the operation will be presented. (for example when the template for login is different than payment).
         */
        val name: String,

        /**
         * Actual data that will be signed.
         *
         * This shouldn't be visible to the user.
         */
        val data: String,

        /**
         * Date and time when the operation was created.
         */
        val created: ZonedDateTime,

        /**
         * Date and time when the operation will expire.
         */
        val expires: ZonedDateTime,

        /**
         * Data that should be presented to the user.
         */
        val formData: FormData,

        /**
         * Allowed signature types.
         *
         * For example in some cases, biometric authentication might not available for security reasons.
         */
        val allowedSignatureType: AllowedSignatureType)

/**
 * Model class wrapping allowed signature types.
 */
data class AllowedSignatureType(

        /**
         * If operation should be signed with 1 or 2 factor authentication
         */
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

        @SerializedName("ECDSA")
        ASSYMETRIC_ECDSA("ECDSA");
    }

    /**
     * Signature factor
     */
    enum class Factor(val variant: String) {

        @SerializedName("possession_knowledge")
        POSSESSION_KNOWLEDGE("possession_knowledge"),

        @SerializedName("possession_biometry")
        POSSESSION_BIOMETRY("possession_biometry")
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
        val title: String,

        /**
         * Message for the user
         */
        val message: String,

        /**
         * Other attributes.
         *
         * Note that attributes can be presented with different classes (Starting with Attribute*) based on the attribute type.
         */
        val attributes: List<Attribute>)