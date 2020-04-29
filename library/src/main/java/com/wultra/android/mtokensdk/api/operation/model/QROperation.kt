/*
 * Copyright (c) 2019, Wultra s.r.o. (www.wultra.com).
 *
 * All rights reserved. This source code can be used only for purposes specified
 * by the given license contract signed by the rightful deputy of Wultra s.r.o.
 * This source code can be used only by the owner of the license.
 *
 * Any disputes arising in respect of this agreement (license) shall be brought
 * before the Municipal Court of Prague.
 */

package com.wultra.android.mtokensdk.api.operation.model

import java.math.BigDecimal
import java.util.*

/**
 * QR operation model class.
 *
 * @author Jan Kobersky, kober@wultra.com
 */
data class QROperation(val operationId: String, // Operation's identifier
                       val title: String, // Title associated with the operation.
                       val message: String, // Message associated with the operation
                       val operationData: QROperationData, // Significant data fields associated with the operation
                       val nonce: String, // Nonce for offline signature calculation, in Base64 format
                       val flags: QROperationFlags, // Flags associated with the operation
                       val signedData: ByteArray, // Data for signature validation
                       val signature: QROperationSignature, // ECDSA signature calculated from 'signedData'
                       val isNewerFormat: Boolean) { // QR code uses a string in newer format that this class implements. This may be used as warning in UI {

    fun dataForOfflineSigning() = "$operationId&${operationData.sourceString}".toByteArray()
}

data class QROperationFlags(val biometryAllowed: Boolean)

data class QROperationData(
        val version: Version, // Version of form data
        val templateId: Int, // Template identifier (0 .. 99 in v1)
        val fields: ArrayList<QROperationDataField>, // Array with form fields. Version v1 supports up to 5 fields.
        val sourceString: String) { // A whole line from which was this structure constructed.

    enum class Version {
        // First version of operation data
        V1,
        // Type representing all newer versions of operation data (for forward compatibility)
        VX;

        companion object  {
            fun parse(value: Char): Version {
                if (value == 'A') {
                    return V1
                }
                return VX
            }
        }

    }

    abstract class QROperationDataField
    // Amount with currency
    data class AmountField(val amount: BigDecimal, val currency: String): QROperationDataField()
    // Account in IBAN format, with optional BIC
    data class AccountField(val iban: String, val bic: String?): QROperationDataField()
    // Account in arbitrary textual format
    data class AnyAccountField(val account: String): QROperationDataField()
    // Date field
    data class DateField(val date: Date): QROperationDataField()
    // Reference field
    data class ReferenceField(val text: String): QROperationDataField()
    // Note Field
    data class NoteField(val text: String): QROperationDataField()
    // Text Field
    data class TextField(val text: String): QROperationDataField()
    // Fallback for forward compatibility. If newer version of operation data
    // contains new field type, then this case can be used for it's representation.
    data class FallbackField(val text: String, val type: Char): QROperationDataField()
    // Reserved for optional and not used fields
    object EmptyField: QROperationDataField()
}

/**
 * Model class for offline QR operation signature.
 */
data class QROperationSignature(val signingKey: SigningKey?, val signature: ByteArray, val signatureString: String) {

    enum class SigningKey(val typeValue: Char) {
        MASTER('0'),
        PERSONALIZED('1');

        companion object {
            val map = mutableMapOf<Char, SigningKey>()
            init {
                for (type in SigningKey.values()) {
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