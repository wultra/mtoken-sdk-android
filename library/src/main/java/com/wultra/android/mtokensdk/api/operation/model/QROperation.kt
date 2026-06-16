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

import io.getlime.security.powerauth.core.CoreSignatureKeyId
import io.getlime.security.powerauth.exception.PowerAuthErrorException
import io.getlime.security.powerauth.sdk.PowerAuthSDK
import java.math.BigDecimal
import java.util.Date

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

    /** Additional Time-based one-time password for proximity check */
    val totp: String?,

    /** Data for signature validation */
    @Suppress("ArrayInDataClass")
    val signedData: ByteArray,

    /** Signature calculated from [signedData] */
    val signature: QROperationSignature,

    /** QR code uses a string in a newer format that this class implements. This may be used as a warning in UI */
    val isNewerFormat: Boolean
) {
    fun dataForOfflineSigning(): ByteArray {
        return if (totp == null) {
            "$operationId&${operationData.sourceString}".toByteArray(Charsets.UTF_8)
        } else {
            "$operationId&${operationData.sourceString}&$totp".toByteArray(Charsets.UTF_8)
        }
    }

    /**
     * Verifies the signature of the QR operation against the server's public keys
     * held by the provided [PowerAuthSDK] instance.
     *
     * The method picks the correct verification key based on [QROperationSignature.keyType]
     * and validates [QROperationSignature.data] against [signedData].
     *
     * Call this after parsing the QR code and before presenting the operation to the user,
     * so the user is never asked to confirm an operation whose signature cannot be verified.
     *
     * @param powerAuth The [PowerAuthSDK] instance used to verify the signature.
     * @throws PowerAuthErrorException if the signature is invalid or cannot be verified.
     */
    @Throws(PowerAuthErrorException::class)
    fun verifySignature(powerAuth: PowerAuthSDK) {
        powerAuth.verifyDigitalSignature(signature.data, signedData, signature.keyType.powerAuthKeyId)
    }
}

/**
 * Flags associated with the operation
 */
data class QROperationFlags(
    /** If true, then 2FA signature with a biometric factor can be used for operation confirmation.*/
    val biometricsAllowed: Boolean,

    /** If true, confirm / reject buttons are flipped in the UI. This can be useful to test users' attention. */
    val flipButtons: Boolean,

    /** When the operation is considered a "potential fraud" on the server, a warning UI should be displayed to the user. */
    val fraudWarning: Boolean,

    /** Block confirmation when the call is active. */
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
     * Fallback for forward compatibility. If a newer version of operation data
     * contains a new field type, then this case can be used for its representation.
     */
    data class FallbackField(val text: String, val type: Char): QROperationDataField()

    /** Reserved for optional and not used fields */
    object EmptyField: QROperationDataField()

    abstract class QROperationDataField
}

/** Model class for offline QR operation signature. */
data class QROperationSignature(
    /** Defines which key has been used for signature calculation. */
    val keyType: KeyType,

    /** Raw signature data */
    @Suppress("ArrayInDataClass")
    val data: ByteArray,

    /** Original Base64 data source as received from the payload */
    val dataSource: String
) {
    /** Defines which key was used for signature calculation */
    enum class KeyType(val typeValue: Char) {
        /** Master server key was used for signature calculation */
        MASTER('0'),

        /** Personalized server's private key was used for signature calculation */
        PERSONALIZED('1'),

        /** KMAC-based symmetric key for MAC verification */
        MAC_PERSONALIZED('2');

        /** PowerAuth signature key identifier that corresponds to this key type. */
        val powerAuthKeyId: Int
            get() = when (this) {
                MASTER -> CoreSignatureKeyId.MASTER_EC
                PERSONALIZED -> CoreSignatureKeyId.SERVER_EC
                MAC_PERSONALIZED -> CoreSignatureKeyId.MAC_PERSONALIZED
            }

        /** Validates the key length for the given signature data. */
        fun validate(signatureData: ByteArray): Boolean {
            return when (this) {
                MAC_PERSONALIZED -> signatureData.size == 32
                MASTER, PERSONALIZED -> signatureData.size in 64..255
            }
        }

        companion object {
            private val map = entries.associateBy(KeyType::typeValue)
            fun fromTypeValue(typeValue: Char) = map[typeValue]
        }
    }
}
