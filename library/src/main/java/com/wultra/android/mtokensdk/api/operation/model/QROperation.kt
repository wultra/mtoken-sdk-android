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

import java.math.BigDecimal
import java.util.*

/**
 * QR operation model class.
 */
data class QROperation(
    /** Operation's identifier */
    val operationId: String,

    /** Title associated with the operation. */
    val title: String,

    /** Message associated with the operation */
    val message: String,

    /** Significant data fields associated with the operation */
    val operationData: QROperationData,

    /** Nonce for offline signature calculation, in Base64 format */
    val nonce: String,

    /** Flags associated with the operation */
    val flags: QROperationFlags,

    /** Additional Time-based one time password for proximity check */
    val totp: String?,

    /** Data for signature validation */
    val signedData: ByteArray,

    /** ECDSA signature calculated from [signedData] */
    val signature: QROperationSignature,

    /** QR code uses a string in newer format that this class implements. This may be used as warning in UI */
    val isNewerFormat: Boolean
) {
    fun dataForOfflineSigning(): ByteArray {
        return if (totp == null) {
            "$operationId&${operationData.sourceString}".toByteArray(Charsets.UTF_8)
        } else {
            "$operationId&${operationData.sourceString}&$totp".toByteArray(Charsets.UTF_8)
        }
    }
}

/**
 * Flags associated with the operation
 */
data class QROperationFlags(
    /** If true, then 2FA signature with biometry factor can be used for operation confirmation.*/
    val biometryAllowed: Boolean,

    /** If confirm/reject buttons should be flipped in the UI. This can be useful to test users attention. */
    val flipButtons: Boolean,

    /** When the operation is considered a "potential fraud" on the server, a warning UI should be displayed to the user. */
    val fraudWarning: Boolean,

    /** Block confirmation when call is active. */
    val blockWhenOnCall: Boolean
)

/**
 * defines operation data in QR operation
 */
data class QROperationData(

    /** Version of form data */
    val version: Version,

    /** Template identifier (0 .. 99 in v1) */
    val templateId: Int,

    /** Array with form fields. Version v1 supports up to 5 fields. */
    val fields: ArrayList<QROperationDataField>,

    /** A whole line from which was this structure constructed. */
    val sourceString: String
) {

    enum class Version {
        /** First version of operation data */
        V1,

        /** Type representing all newer versions of operation data (for forward compatibility) */
        VX;

        companion object {
            fun parse(value: Char): Version {
                if (value == 'A') {
                    return V1
                }
                return VX
            }
        }
    }

    /** Amount with currency */
    data class AmountField(val amount: BigDecimal, val currency: String): QROperationDataField()

    /** Account in IBAN format, with optional BIC */
    data class AccountField(val iban: String, val bic: String?): QROperationDataField()

    /** Account in arbitrary textual format */
    data class AnyAccountField(val account: String): QROperationDataField()

    /** Date field */
    data class DateField(val date: Date): QROperationDataField()

    /** Reference field */
    data class ReferenceField(val text: String): QROperationDataField()

    /** Note Field */
    data class NoteField(val text: String): QROperationDataField()

    /** Text Field */
    data class TextField(val text: String): QROperationDataField()

    /**
     * Fallback for forward compatibility. If newer version of operation data
     * contains new field type, then this case can be used for it's representation.
     */
    data class FallbackField(val text: String, val type: Char): QROperationDataField()

    /** Reserved for optional and not used fields */
    object EmptyField: QROperationDataField()

    abstract class QROperationDataField
}

/** Model class for offline QR operation signature. */
data class QROperationSignature(
    /** Defines which key has been used for ECDSA signature calculation.*/
    val signingKey: SigningKey,

    /** Raw signature data */
    @Suppress("ArrayInDataClass")
    val signature: ByteArray,

    /** Signature in Base64 format */
    val signatureString: String
) {

    /** Defines which key was used for ECDSA signature calculation */
    enum class SigningKey(val typeValue: Char) {
        /** Master server key was used for ECDSA signature calculation */
        MASTER('0'),

        /** Personalized server's private key was used for ECDSA signature calculation */
        PERSONALIZED('1');

        companion object {
            private val map = mutableMapOf<Char, SigningKey>()
            init {
                for (type in values()) {
                    map[type.typeValue] = type
                }
            }
            fun fromTypeValue(typeValue: Char): SigningKey? {
                return map[typeValue]
            }
        }
    }

    fun isMaster() = signingKey == SigningKey.MASTER
}
