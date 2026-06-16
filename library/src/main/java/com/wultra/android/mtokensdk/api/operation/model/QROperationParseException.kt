/*
 * Copyright 2026 Wultra s.r.o.
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

/**
 * Exception thrown when parsing a QR operation payload fails.
 *
 * Extends [IllegalArgumentException] to maintain backward compatibility
 * with callers that catch the previously thrown [IllegalArgumentException].
 *
 * @property reason Structured error reason for programmatic handling.
 */
class QROperationParseException(
    val reason: QRParseError,
    message: String,
    cause: Throwable? = null
) : IllegalArgumentException(message, cause)

/**
 * Enumeration of possible parse failure reasons for QR operations.
 */
enum class QRParseError {
    /** The input string has too few attribute fields. */
    INVALID_FORMAT,

    /** The operation ID field is empty. */
    EMPTY_OPERATION_ID,

    /** The nonce is not a valid 16-byte Base64 value. */
    INVALID_NONCE,

    /** The signature field is empty, has an unknown key type, invalid Base64, or wrong length. */
    INVALID_SIGNATURE,

    /** The operation data cannot be parsed (bad version, template ID, or field content). */
    INVALID_OPERATION_DATA,

    /** Digital signature verification against the server's public key failed. */
    SIGNATURE_VERIFICATION_FAILED
}
