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

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.util.Base64
import androidx.annotation.MainThread
import com.wultra.android.mtokensdk.log.WMTLogger
import io.getlime.security.powerauth.sdk.PowerAuthSDK
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * Parser for QR operation data encoded in a scanned QR code.
 *
 * When created with a [PowerAuthSDK] instance, the parser automatically verifies the
 * operation's digital signature during [parse]. If verification fails, the
 * parse throws a [QROperationParseException] with reason [QRParseError.SIGNATURE_VERIFICATION_FAILED].
 * When created without one (using the parameterless constructor), signature verification
 * is left to the caller.
 */
class QROperationParser(private val powerAuth: PowerAuthSDK? = null) {

    companion object {

        /** Minimum lines in input string supported by this parser. */
        private const val MINIMUM_ATTRIBUTE_FIELDS = 7

        /** Current number of lines in the input string, supported by this parser. */
        private const val CURRENT_ATTRIBUTE_FIELDS = 8

        /** Maximum number of operation data fields supported in this version. */
        private const val MAXIMUM_DATA_FIELDS = 6

        /** Characters allowed in IBAN and BIC values. */
        private const val IBAN_BIC_ALLOWED_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"

        /**
         * Process loaded payload from a scanned offline QR.
         *
         * This static method creates a parser without automatic signature verification.
         * The caller is responsible for verifying the operation's signature after parsing,
         * for example, by calling [QROperation.verifySignature].
         *
         * @param string String parsed from QR code
         *
         * @throws QROperationParseException When there is no operation in the provided string.
         * @return Parsed operation.
         */
        @Throws(QROperationParseException::class)
        fun parse(string: String): QROperation {
            return QROperationParser().parse(string)
        }

        /**
         * Asynchronously process loaded payload from a scanned offline QR.
         *
         * Parsing is performed on the provided [executor] (a single-thread executor by default).
         * The [callback] is always invoked on the Android main thread (looper of
         * `Looper.getMainLooper()`), for both the success and the failure paths, so it is safe
         * to update the UI directly from within it. This is the recommended variant when calling
         * the parser from UI callbacks (e.g. a QR scanner) to avoid blocking the main thread.
         *
         * This static method creates a parser without automatic signature verification.
         * The caller is responsible for verifying the operation's signature after parsing,
         * for example, by calling [QROperation.verifySignature].
         *
         * @param string String parsed from QR code.
         * @param executor Executor on which parsing is performed. Defaults to a single-thread executor.
         * @param callback Invoked on the main thread with the parsing [Result]. A successful result
         *   wraps the parsed [QROperation]; a failed result wraps a [QROperationParseException].
         *   Both outcomes are delivered on the main thread.
         */
        fun parseAsync(
            string: String,
            executor: Executor = defaultParserExecutor,
            @MainThread callback: (Result<QROperation>) -> Unit
        ) {
            QROperationParser().parseAsync(string, executor, callback)
        }

        /** Lazily created single-thread executor used as the default for [parseAsync]. */
        private val defaultParserExecutor: Executor by lazy {
            Executors.newSingleThreadExecutor { r ->
                Thread(r, "QROperationParser").apply { isDaemon = true }
            }
        }
    }

    /**
     * Process loaded payload from a scanned offline QR.
     *
     * If this parser was created with a [PowerAuthSDK] instance, the parsed operation's
     * signature is automatically verified before returning. A verification failure produces
     * a [QROperationParseException] with reason [QRParseError.SIGNATURE_VERIFICATION_FAILED].
     *
     * @param string String parsed from QR code
     *
     * @throws QROperationParseException When there is no operation in provided string or signature verification fails.
     * @return Parsed operation.
     */
    @Throws(QROperationParseException::class)
    fun parse(string: String): QROperation {
        // Split string by newline
        val attributes = string.split("\n")

        if (attributes.count() < MINIMUM_ATTRIBUTE_FIELDS) {
            throw parseError(
                QRParseError.INVALID_FORMAT,
                "QR operation needs at least $MINIMUM_ATTRIBUTE_FIELDS attributes, got ${attributes.count()}"
            )
        }

        // Acquire all attributes
        val operationId = attributes[0]
        val title = unescapeAttributeText(attributes[1])
        val message = unescapeAttributeText(attributes[2])
        val dataString = attributes[3]
        val flagsString = attributes[4]
        val totp = if (attributes.size > MINIMUM_ATTRIBUTE_FIELDS) attributes[5] else null

        // Signature and nonce are always located at last lines
        val nonce = attributes[attributes.lastIndex - 1]
        val signatureString = attributes[attributes.lastIndex]

        // Validate operationId
        if (operationId.isEmpty()) {
            throw parseError(QRParseError.EMPTY_OPERATION_ID, "Operation ID is empty")
        }

        val signature = parseSignature(signatureString)

        // Validate nonce
        val nonceByteArray = try {
            Base64.decode(nonce, Base64.DEFAULT)
        } catch (e: Exception) {
            throw parseError(QRParseError.INVALID_NONCE, "Nonce is not a valid Base64 string", e)
        }
        if (nonceByteArray.size != 16) {
            throw parseError(QRParseError.INVALID_NONCE, "Nonce must be 16 bytes, got ${nonceByteArray.size}")
        }

        // Parse operation data fields
        val formData = parseOperationData(dataString)

        // Rebuild signed data (everything except the raw signature Base64)
        val signedData = string.substring(0, string.length - signature.dataSource.length).toByteArray()

        // Parse flags
        val flags = parseOperationFlags(flagsString)
        val isNewerFormat = attributes.count() > CURRENT_ATTRIBUTE_FIELDS

        val operation = QROperation(operationId, title, message, formData, nonce, flags, totp, signedData, signature, isNewerFormat)

        // Verify signature when PowerAuthSDK is available
        powerAuth?.let {
            try {
                operation.verifySignature(it)
            } catch (e: Exception) {
                throw parseError(QRParseError.SIGNATURE_VERIFICATION_FAILED, "Signature verification failed", e)
            }
        }

        return operation
    }

    /**
     * Asynchronously process loaded payload from a scanned offline QR.
     *
     * Parsing (including signature verification when this parser was created with a
     * [PowerAuthSDK] instance) is performed on the provided [executor] - a single-thread
     * executor by default. The [callback] is always invoked on the Android main thread
     * (looper of `Looper.getMainLooper()`), for both the success and the failure paths, so it
     * is safe to update the UI directly from within it.
     *
     * Use this variant to offload parsing off the UI thread (for example, when invoked
     * directly from a QR scanner callback). The synchronous [parse] method remains
     * available for callers that already run on a background thread.
     *
     * The [Result] passed to the [callback] wraps either the parsed [QROperation] on success
     * or a [QROperationParseException] on failure. Both outcomes are delivered on the main
     * thread; no other exception types are reported through the callback.
     *
     * @param string String parsed from QR code.
     * @param executor Executor on which parsing is performed. Defaults to a single-thread executor.
     * @param callback Invoked on the main thread with the parsing [Result]. A successful result
     *   wraps the parsed [QROperation]; a failed result wraps a [QROperationParseException].
     *   Both outcomes are delivered on the main thread.
     */
    fun parseAsync(
        string: String,
        executor: Executor = defaultParserExecutor,
        @MainThread callback: (Result<QROperation>) -> Unit
    ) {
        val mainHandler = Handler(Looper.getMainLooper())
        executor.execute {
            val result = try {
                Result.success(parse(string))
            } catch (e: QROperationParseException) {
                Result.failure(e)
            }
            mainHandler.post { callback(result) }
        }
    }

    // region Private parsing methods

    private fun unescapeAttributeText(text: String): String {
        if (!text.contains("\\")) return text
        return text.replace("\\n", "\n").replace("\\\\", "\\")
    }

    private fun parseSignature(signaturePayload: String): QROperationSignature {
        if (signaturePayload.isEmpty()) {
            throw parseError(QRParseError.INVALID_SIGNATURE, "Signature string is empty")
        }
        val keyType = QROperationSignature.KeyType.fromTypeValue(signaturePayload[0])
            ?: throw parseError(QRParseError.INVALID_SIGNATURE, "Unknown signing key type '${signaturePayload[0]}'")

        val signatureBase64 = signaturePayload.substring(1)
        val signatureByteArray = try {
            Base64.decode(signatureBase64, Base64.DEFAULT)
        } catch (e: Exception) {
            throw parseError(QRParseError.INVALID_SIGNATURE, "Signature is not a valid Base64 string", e)
        }
        if (!keyType.validate(signatureByteArray)) {
            throw parseError(
                QRParseError.INVALID_SIGNATURE,
                "Signature length (${signatureByteArray.size} bytes) is invalid for key type '$keyType'"
            )
        }
        return QROperationSignature(keyType, signatureByteArray, signatureBase64)
    }

    private fun parseOperationData(string: String): QROperationData {
        val stringFields = splitOperationData(string)
        if (stringFields.isEmpty()) {
            throw parseError(QRParseError.INVALID_OPERATION_DATA, "Operation data string is empty")
        }

        // Get and check version
        val versionString = stringFields.first()
        val versionChar = versionString.firstOrNull()
            ?: throw parseError(QRParseError.INVALID_OPERATION_DATA, "Version string is empty")

        if (versionChar !in 'A'..'Z') {
            throw parseError(QRParseError.INVALID_OPERATION_DATA, "Invalid version character '$versionChar', expected A-Z")
        }

        val version = QROperationData.Version.parse(versionChar)
        val templateId = versionString.substring(1).toIntOrNull()
            ?: throw parseError(QRParseError.INVALID_OPERATION_DATA, "Template ID is not an integer in '$versionString'")

        if (templateId !in 0..99) {
            throw parseError(QRParseError.INVALID_OPERATION_DATA, "Template ID $templateId is out of range 0-99")
        }

        val fields = parseDataFields(stringFields)
        return QROperationData(version, templateId, fields, string)
    }

    /**
     * Splits an input string into fields, handling escaped asterisks (`\*`).
     */
    private fun splitOperationData(string: String): ArrayList<String> {
        val components = string.split("*")
        val fields = arrayListOf<String>()
        var appendNext = false
        for (substring in components) {
            if (appendNext) {
                val prev = fields.lastOrNull()
                if (prev != null) {
                    // Remove trailing backslash from previous and rejoin with asterisk
                    fields[fields.lastIndex] = "${prev.substring(0, prev.lastIndex)}*$substring"
                }
            } else {
                fields.add(substring)
            }
            appendNext = substring.lastOrNull() == '\\'
        }
        return fields
    }

    private fun parseDataFields(fields: ArrayList<String>): ArrayList<QROperationData.QROperationDataField> {
        val result = arrayListOf<QROperationData.QROperationDataField>()
        for (stringField in fields.subList(1, fields.size)) {
            val typeId = stringField.firstOrNull()
            if (typeId == null) {
                result.add(QROperationData.EmptyField)
                continue
            }
            result.add(
                when (typeId) {
                    'A' -> parseAmount(stringField)
                    'I' -> parseIban(stringField)
                    'Q' -> QROperationData.AnyAccountField(unescapeFieldText(stringField))
                    'D' -> parseDate(stringField)
                    'R' -> QROperationData.ReferenceField(unescapeFieldText(stringField))
                    'N' -> QROperationData.NoteField(unescapeFieldText(stringField))
                    'T' -> QROperationData.TextField(unescapeFieldText(stringField))
                    else -> QROperationData.FallbackField(unescapeFieldText(stringField), typeId)
                }
            )
        }
        if (result.size > MAXIMUM_DATA_FIELDS) {
            throw parseError(QRParseError.INVALID_OPERATION_DATA, "Too many data fields (${result.size}), maximum is $MAXIMUM_DATA_FIELDS")
        }
        return result
    }

    private fun parseAmount(string: String): QROperationData.AmountField {
        val value = string.substring(1)
        if (value.length < 4) {
            throw parseError(QRParseError.INVALID_OPERATION_DATA, "Insufficient length for amount+currency in '$string'")
        }
        val currency = value.substring(value.lastIndex - 2).uppercase()
        val amountString = value.substring(0, value.lastIndex - 2)
        val amount = try {
            BigDecimal(amountString)
        } catch (e: NumberFormatException) {
            throw parseError(QRParseError.INVALID_OPERATION_DATA, "Invalid amount number '$amountString'", e)
        }
        return QROperationData.AmountField(amount, currency)
    }

    private fun parseIban(string: String): QROperationData.AccountField {
        val ibanBic = string.substring(1)
        val components = ibanBic.split(",").filter { it.isNotEmpty() }
        if (components.isEmpty() || components.size > 2) {
            throw parseError(QRParseError.INVALID_OPERATION_DATA, "Unsupported IBAN format in '$string'")
        }
        val iban = components[0]
        val bic = components.elementAtOrNull(1)
        validateIbanChars(iban, "IBAN")
        bic?.let { validateIbanChars(it, "BIC") }
        return QROperationData.AccountField(iban, bic)
    }

    private fun validateIbanChars(value: String, fieldName: String) {
        for (c in value) {
            if (c !in IBAN_BIC_ALLOWED_CHARS) {
                throw parseError(QRParseError.INVALID_OPERATION_DATA, "Invalid character '$c' in $fieldName")
            }
        }
    }

    private fun unescapeFieldText(string: String): String {
        val text = string.substring(1)
        if (!text.contains("\\")) return text
        return text.replace("\\n", "\n").replace("\\\\", "\\")
    }

    @SuppressLint("SimpleDateFormat")
    private fun parseDate(string: String): QROperationData.DateField {
        val dateString = string.substring(1)
        if (dateString.length != 8) {
            throw parseError(QRParseError.INVALID_OPERATION_DATA, "Date '$dateString' must be 8 characters (YYYYMMDD)")
        }
        val formatter = SimpleDateFormat("yyyyMMdd", Locale.US).also { it.isLenient = false }
        val date = try {
            formatter.parse(dateString)
        } catch (e: Exception) {
            throw parseError(QRParseError.INVALID_OPERATION_DATA, "Unparseable date '$dateString'", e)
        } ?: throw parseError(QRParseError.INVALID_OPERATION_DATA, "Unparseable date '$dateString'")
        return QROperationData.DateField(date)
    }

    private fun parseOperationFlags(string: String): QROperationFlags {
        return QROperationFlags(
            biometricsAllowed = 'B' in string,
            flipButtons = 'X' in string,
            fraudWarning = 'F' in string,
            blockWhenOnCall = 'C' in string
        )
    }

    private fun parseError(
        reason: QRParseError,
        message: String,
        cause: Throwable? = null
    ): QROperationParseException {
        WMTLogger.e("QROperationParser: $message")
        return QROperationParseException(reason, message, cause)
    }
}
