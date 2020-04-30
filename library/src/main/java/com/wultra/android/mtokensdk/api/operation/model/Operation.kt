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
 * Operation data model class.
 */
data class Operation(val id: String,
                     val name: String,
                     val data: String,
                     val expired: Boolean,
                     val operationCreated: ZonedDateTime,
                     val operationExpires: ZonedDateTime,
                     val allowedSignatureType: AllowedSignatureType,
                     val formData: FormData) {
    /**
     * Check if the operation is a "login" operation.
     */
    fun isLogin(): Boolean {
        return name == "login"
    }
}

/**
 * Model class wrapping allowed signature types.
 */
data class AllowedSignatureType(val type: Type,
                                val variants: List<String> = listOf()) {

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
     * Check if biometry factor is allowed for the signature type of an operation.
     */
    fun isBiometryAllowed(): Boolean {
        return variants.contains("possession_biometry")
    }
}

/**
 * Model class for operation form data.
 */
data class FormData(val title: String,
                    val message: String,
                    val attributes: List<Attribute>)