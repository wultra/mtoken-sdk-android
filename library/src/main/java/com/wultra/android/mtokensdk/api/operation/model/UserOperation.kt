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

import com.google.gson.annotations.SerializedName
import com.wultra.android.mtokensdk.operation.expiration.ExpirableOperation
import org.threeten.bp.ZonedDateTime

/**
 * [UserOperation] is an object returned from the backend that can be either approved or rejected.
 * It is usually visually presented to the user as a non-editable form with information, about
 * the real-world operation (for example login or payment).
 */
open class UserOperation(
    /** Unique operation identifier */
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

    /** Date and time when the operation was created. */
    @SerializedName("operationCreated")
    val created: ZonedDateTime,

    /** Date and time when the operation will expire. */
    @SerializedName("operationExpires")
    override val expires: ZonedDateTime,

    /** Data that should be presented to the user. */
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
    val ui: OperationUIData?,

    /**
     * Proximity Check Data to be passed when OTP is handed to the app
     */
    @SerializedName("proximityCheck")
    override var proximityCheck: ProximityCheck? = null,

    /**
     *  Enum-like reason why the status has changed.
     *
     *  Max 32 characters are expected. Possible values depend on the backend implementation and configuration.
     */
    @SerializedName("statusReason")
    val statusReason: String?,

    /**
     * Processing status of the operation
     */
    @SerializedName("status")
    val status: UserOperationStatus,
) : IOperation, ExpirableOperation

/**
 * Model class wrapping allowed signature types.
 */
data class AllowedSignatureType(

    /** If operation should be signed with 1 or 2 factor authentication */
    @SerializedName("type")
    val type: Type,

    /** What factors ("password" or/and "biometrics") can be used for signing this operation. */
    @SerializedName("variants")
    val factors: List<Factor> = emptyList()
) {

    /** Check if biometric factor is allowed for the signature type of an operation. */
    fun isBiometricsAllowed(): Boolean {
        return factors.contains(Factor.POSSESSION_BIOMETRY)
    }

    @Deprecated(replaceWith = ReplaceWith("isBiometricsAllowed()"), message = "Use isBiometricsAllowed() instead")
    fun isBiometryAllowed() = isBiometricsAllowed()

    /** Signature types. */
    enum class Type(val type: String) {

        @SerializedName("1FA")
        MULTIFACTOR_1FA("1FA"),

        @SerializedName("2FA")
        MULTIFACTOR_2FA("2FA"),

        // 3FA is not used in mtoken
        // @SerializedName("3FA")
        // MULTIFACTOR_3FA("3FA"),

        @SerializedName("ECDSA")
        ASYMMETRIC_ECDSA("ECDSA")
    }

    /** Signature factor */
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

    /** Title of the operation */
    @SerializedName("title")
    val title: String,

    /** Message for the user */
    @SerializedName("message")
    val message: String,

    /**
     * Texts for the result of the operation
     *
     * This includes messages for different outcomes of the operation such as success, rejection, and failure.
     */
    @SerializedName("resultTexts")
    val resultTexts: ResultTexts?,

    /**
     * Other attributes.
     *
     * Note that attributes can be presented with different classes (Starting with Attribute*) based on the attribute type.
     */
    @SerializedName("attributes")
    val attributes: List<Attribute>
)

data class OperationUIData(
    /** Confirm and Reject buttons should be flipped both in position and style */
    @SerializedName("flipButtons")
    val flipButtons: Boolean?,

    /** Block approval when on call (for example when on phone or skype call) */
    @SerializedName("blockApprovalOnCall")
    val blockApprovalOnCall: Boolean?,

    /** UI for pre-approval operation screen */
    @SerializedName("preApprovalScreen")
    val preApprovalScreen: PreApprovalScreen?,

    /**
     * UI for post-approval operation screen
     *
     * Type of PostApprovalScreen is presented with different classes (Starting with `PostApprovalScreen*`)
     */
    @SerializedName("postApprovalScreen")
    val postApprovalScreen: PostApprovalScreen?
)

/**
 * Operation OTP data
 *
 * Data shall be assigned to the operation when obtained in the app
 */
data class ProximityCheck(

    /** The actual Time-based one time password */
    val totp: String,

    /** Type of the Proximity check */
    val type: ProximityCheckType,

    /** Timestamp when the operation was scanned (qrCode) or delivered to the device (deeplink) */
    val timestampReceived: ZonedDateTime = ZonedDateTime.now()
)

/**
 * Types of possible Proximity Checks
 */
enum class ProximityCheckType(val value: String) {
    QR_CODE("QR_CODE"),
    DEEPLINK("DEEPLINK")
}

/** Processing status of the operation */
enum class UserOperationStatus {
    /** Operation was approved */
    APPROVED,
    /** Operation was rejected */
    REJECTED,
    /** Operation is pending its resolution */
    PENDING,
    /** Operation was canceled */
    CANCELED,
    /** Operation expired */
    EXPIRED,
    /** Operation failed */
    FAILED
}
