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

import android.annotation.SuppressLint
import android.util.Base64
import com.wultra.android.mtokensdk.common.Logger
import java.math.BigDecimal
import java.text.SimpleDateFormat

/**
 * Parser for QR operation
 */
class QROperationParser {

    companion object {

        // Minimum lines in input string supported by this parser
        private const val minimumAttributeFields = 7

        // Current number of lines in input string, supported by this parser
        private const val currentAttributeFields = 7

        // Maximum number of operation data fields supported in this version.
        private const val maximumDataFields = 5

        @SuppressLint("SimpleDateFormat")
        private val dateFormatter = SimpleDateFormat("yyyyMMdd").also { it.isLenient = false }

        /**
         * Process loaded payload from a scanned offline QR.
         *
         * @param string String parsed from QR code
         *
         * @throws IllegalArgumentException When there is no operation in provided string.
         * @return Parsed operation.
         */
        @Throws(IllegalArgumentException::class)
        fun parse(string: String): QROperation {
            try {
                // Split string by newline
                val attributes = string.split("\n")

                if (attributes.count() < minimumAttributeFields) {
                    throw IllegalArgumentException("QR operation needs to have at least $minimumAttributeFields attributes")
                }

                // Acquire all attributes
                val operationId = attributes[0]
                val title = parseAttributeText(attributes[1])
                val message = parseAttributeText(attributes[2])
                val dataString = attributes[3]
                val flagsString = attributes[4]
                // Signature and nonce are always located at last lines
                val nonce = attributes[attributes.lastIndex - 1]
                val signatureString = attributes[attributes.lastIndex]

                // Validate operationId
                if (operationId.isEmpty()) {
                    throw IllegalArgumentException("QR operation ID is empty!.")
                }

                val signature = parseSignature(signatureString)

                // validate nonce
                val nonceByteArray = Base64.decode(nonce, Base64.DEFAULT)
                if (nonceByteArray.size != 16) {
                    throw IllegalArgumentException("Invalid nonce data")
                }

                // Parse operation data fields
                val formData = parseOperationData(dataString)

                // Rebuild signed data, without pure signature string
                val signedData = string.substring(0, string.length - signature.signatureString.length).toByteArray()

                // Parse flags
                val flags = parseOperationFlags(flagsString)
                val isNewerFormat = attributes.count() > currentAttributeFields

                return QROperation(operationId, title, message, formData, nonce, flags, signedData, signature, isNewerFormat)
            } catch (e: IllegalArgumentException) {
                Logger.e(e.message ?:  "Payload is not a valid QR operation")
                throw e
            }
        }

        private fun parseAttributeText(text: String): String {
            if (text.contains("\\")) {
                return text.replace("\\n", "\n").replace("\\\\", "\\")
            }
            return text
        }

        /**
         * Returns operation signature object if provided string contains valid key type and signature.
         */
        private fun parseSignature(signaturePayload: String): QROperationSignature {
            if (signaturePayload.isEmpty()) {
                throw IllegalArgumentException("Empty offline operation signature")
            }
            val signingKey = QROperationSignature.SigningKey.fromTypeValue(signaturePayload[0]) ?: throw IllegalArgumentException("Invalid offline operation signature key")
            val signatureBase64 = signaturePayload.substring(1)
            val signatureByteArray = Base64.decode(signatureBase64, Base64.DEFAULT)
            if (signatureByteArray.size < 64 || signatureByteArray.size > 255) {
                throw IllegalArgumentException("Invalid offline operation signature data")
            }
            return QROperationSignature(signingKey, signatureByteArray, signatureBase64)
        }

        /**
         * Parses and translates input string into `QROperationFormData` structure.
         */
        private fun parseOperationData(string: String): QROperationData {
            val stringFields = splitOperationData(string)
            if (stringFields.isEmpty()) {
                throw IllegalArgumentException("No fields at all")
            }

            // Get and check version
            val versionString = stringFields.first()
            val versionChar = versionString.firstOrNull() ?: throw IllegalArgumentException("First fields is empty string")
            if (versionChar < 'A' || versionChar > 'Z') {
                throw IllegalArgumentException("Version has to be an one capital letter")
            }
            val version = QROperationData.Version.parse(versionChar)

            val templateId = versionString.substring(1).toIntOrNull() ?: throw IllegalArgumentException("TemplateID is not an integer")

            if (templateId < 0 || templateId > 99) {
                throw IllegalArgumentException("TemplateID is out of range.")
            }

            // Parse operation data fields
            val fields = parseDataFields(stringFields)

            // Everything looks good, so build a final structure now...
            return QROperationData(version, templateId, fields, string)
        }

        /**
         * Splits input string into array of strings, representing array of form fields.
         * It's expected that input string contains asterisk separated list of fields.
         */
        private fun splitOperationData(string: String): ArrayList<String> {
            // Split string by '*'
            val components = string.split( "*")
            val fields = arrayListOf<String>()
            // Handle escaped asterisk \* in array. This situation is easily detectable
            // by backslash at the end of the string.
            var appendNext = false
            for (substring in components) {
                if (appendNext) {
                    // Previous string ended with backslash
                    var prev = fields.lastOrNull()
                    if (prev != null) {
                        // Remove backslash from last stored value and append this new sequence
                        prev = prev.substring(0, prev.lastIndex)
                        prev = "$prev*$substring"
                        // Replace last element with updated string
                        fields[fields.count() - 1] = prev
                    }
                } else {
                    // Just append this string into final array
                    fields.add(substring)
                }
                // Check if current sequence ends with backslash
                appendNext = substring.lastOrNull() == '\\'
            }
            return fields
        }

        /**
         * Parses input string into array of Field enumerations. Returns nil if some field has
         */
        private fun parseDataFields(fields: ArrayList<String>): ArrayList<QROperationData.QROperationDataField> {

            val result = arrayListOf<QROperationData.QROperationDataField>()
            // Skip version, which is first item in the array
            for (stringField in fields.subList(1, fields.lastIndex + 1)) {
                // Parse each field string
                val typeId = stringField.firstOrNull()

                if (typeId == null) {
                    result.add(QROperationData.EmptyField)
                    continue
                }

                when (typeId) {
                    // Amount
                    'A' -> result.add(parseAmount(stringField))
                    // IBAN
                    'I' -> result.add(parseIban(stringField))
                    // Any account
                    'Q' -> result.add(QROperationData.AnyAccountField(parseFieldText(stringField)))
                    // Date
                    'D' -> result.add(parseDate(stringField))
                    // Reference
                    'R' -> result.add(QROperationData.ReferenceField(parseFieldText(stringField)))
                    // Note
                    'N' -> result.add(QROperationData.NoteField(parseFieldText(stringField)))
                    // Text (generic)
                    'T' -> result.add(QROperationData.TextField(parseFieldText(stringField)))
                    // Fallback
                    else -> result.add(QROperationData.FallbackField(parseFieldText(stringField), typeId))
                }
            }

            if (result.count() > maximumDataFields) {
                throw IllegalArgumentException("Too many fields")
            }
            return result
        }

        private fun parseAmount(string: String): QROperationData.AmountField {
            val value = string.substring(1)
            if (value.length < 4) {
                throw IllegalArgumentException("Insufficient length for number+currency")
            }
            val currency = value.substring(value.lastIndex - 2).toUpperCase()
            val amountString = value.substring(0, value.lastIndex - 2)
            val amount = BigDecimal(amountString)
            return QROperationData.AmountField(amount, currency)
        }

        // Parses IBAN[,BIC] into account field enumeration.
        private fun parseIban(string: String): QROperationData.AccountField {
            // Try to split IBAN to IBAN & BIC
            val ibanBic = string.substring(1)
            val components = ibanBic.split(",").filter { it.isNotEmpty() }
            if (components.count() > 2 || components.count() == 0) {
                throw IllegalArgumentException("Unsupported format")
            }
            val iban = components[0]
            val bic = components.elementAtOrNull(1)
            val allowedChars = "01234567890ABCDEFGHIJKLMNOPQRSTUVWXYZ"
            for (c in iban) {
                if (!allowedChars.contains(c)) {
                    throw IllegalArgumentException("Invalid character in IBAN")
                }
            }
            if (bic != null) {
                for (c in bic) {
                    if (!allowedChars.contains(c)) {
                        throw IllegalArgumentException("Invalid character in BIC")
                    }
                }
            }
            return QROperationData.AccountField(iban, bic)
        }

        private fun parseFieldText(string: String): String {
            val text = string.substring(1)
            if (text.contains("\\")) {
                // Replace escaped "\n" and "\\"
                return text.replace("\\n", "\n").replace("\\\\", "\\")
            }
            return text
        }

        private fun parseDate(string: String): QROperationData.DateField {
            val dateString = string.substring(1)
            if (dateString.length != 8) {
               throw  IllegalArgumentException("Date needs to be 8 characters long")
            }
            val date = dateFormatter.parse(dateString)
            return QROperationData.DateField(date)
        }

        private fun parseOperationFlags(string: String): QROperationFlags {
            return QROperationFlags(string.contains("B"))
        }
    }
}