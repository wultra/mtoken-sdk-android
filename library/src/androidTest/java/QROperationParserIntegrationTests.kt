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

package com.wultra.android.mtokensdk.test

import com.wultra.android.mtokensdk.api.operation.model.QROperationParseException
import com.wultra.android.mtokensdk.api.operation.model.QROperationParser
import com.wultra.android.mtokensdk.api.operation.model.QROperationSignature
import com.wultra.android.mtokensdk.api.operation.model.QRParseError
import com.wultra.android.mtokensdk.operation.OperationsService
import io.getlime.security.powerauth.sdk.PowerAuthAlgorithm
import io.getlime.security.powerauth.sdk.PowerAuthAuthentication
import io.getlime.security.powerauth.sdk.PowerAuthSDK
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

/**
 * Integration tests for [QROperationParser] initialized with a [PowerAuthSDK] instance.
 * These tests require a valid, activated PowerAuth activation backed by a running server.
 */
class QROperationParserIntegrationTests {

    private lateinit var pa: PowerAuthSDK
    private lateinit var ops: OperationsService
    private val pin = "1234"

    @Before
    fun setup() {
        val result = IntegrationUtils.prepareActivation(pin)
        pa = result.first
        ops = result.second.operations
    }

    @After
    fun tearDown() {
        if (!::pa.isInitialized) return
        IntegrationUtils.removeRegistration(pa.activationIdentifier)
        pa.removeActivationLocal(IntegrationUtils.context)
    }

    /**
     * Parsing with a valid [PowerAuthSDK] verifies the signature automatically
     * and the full authorize + OTP flow succeeds.
     */
    @Test
    fun testParseWithPowerAuth() {
        val op = IntegrationUtils.createOperation(IntegrationUtils.Companion.Factors.F_2FA)
        val qrData = IntegrationUtils.getQROperation(op)

        val parser = QROperationParser(pa)
        val qrOperation = parser.parse(qrData.operationQrCodeData)

        Assert.assertEquals(op.operationId, qrOperation.operationId)

        val auth = PowerAuthAuthentication.possessionWithPassword(pin)
        val otp = ops.authorizeOfflineOperation(qrOperation, auth)
        val verified = IntegrationUtils.verifyQROperation(op, qrData, otp)
        Assert.assertTrue("OTP should be valid after parsing with PowerAuth-backed parser", verified.otpValid)
    }

    /**
     * When the QR payload is tampered with, the parser with [PowerAuthSDK]
     * must throw [QROperationParseException] with reason [QRParseError.SIGNATURE_VERIFICATION_FAILED].
     */
    @Test
    fun testParseTamperedDataFails() {
        val op = IntegrationUtils.createOperation(IntegrationUtils.Companion.Factors.F_2FA)
        val qrData = IntegrationUtils.getQROperation(op)

        // Tamper with the title line while preserving the QR structure.
        val lines = qrData.operationQrCodeData.split("\n").toMutableList()
        lines[1] = lines[1] + "x"
        val tampered = lines.joinToString("\n")

        val parser = QROperationParser(pa)
        val e = Assert.assertThrows(QROperationParseException::class.java) {
            parser.parse(tampered)
        }
        Assert.assertEquals(QRParseError.SIGNATURE_VERIFICATION_FAILED, e.reason)
    }

    /**
     * Without [PowerAuthSDK], the parser does not verify the signature,
     * so tampered data still parses successfully.
     */
    @Test
    fun testParseWithoutPowerAuthDoesNotVerify() {
        val op = IntegrationUtils.createOperation(IntegrationUtils.Companion.Factors.F_2FA)
        val qrData = IntegrationUtils.getQROperation(op)

        // Tamper with the title line.
        val lines = qrData.operationQrCodeData.split("\n").toMutableList()
        lines[1] = lines[1] + "x"
        val tampered = lines.joinToString("\n")

        val parser = QROperationParser()
        val qrOperation = parser.parse(tampered)
        Assert.assertNotNull("Parser without PowerAuth should not verify signature", qrOperation)

        // Manual verification should fail because of tampering
        Assert.assertThrows(Exception::class.java) {
            qrOperation.verifySignature(pa)
        }
    }

    /**
     * [QROperation.verifySignature] succeeds on a legitimately signed operation
     * when called manually after parsing without auto-verification.
     */
    @Test
    fun testVerifyDigitalSignatureDirectly() {
        val op = IntegrationUtils.createOperation(IntegrationUtils.Companion.Factors.F_2FA)
        val qrData = IntegrationUtils.getQROperation(op)

        // Parse without automatic verification, then verify manually.
        val qrOperation = QROperationParser().parse(qrData.operationQrCodeData)
        qrOperation.verifySignature(pa)
    }
}

/**
 * QR operation parser integration tests with [PowerAuthAlgorithm.LEGACY_P256].
 * The legacy algorithm uses ECDSA personalized keys instead of KMAC,
 * so the parsed signature key type must be [QROperationSignature.KeyType.PERSONALIZED].
 */
class QROperationParserLegacyP256IntegrationTests {

    private lateinit var pa: PowerAuthSDK
    private lateinit var ops: OperationsService
    private val pin = "1234"

    @Before
    fun setup() {
        val result = IntegrationUtils.prepareActivation(pin, algorithm = PowerAuthAlgorithm.LEGACY_P256)
        pa = result.first
        ops = result.second.operations
    }

    @After
    fun tearDown() {
        if (!::pa.isInitialized) return
        IntegrationUtils.removeRegistration(pa.activationIdentifier)
        pa.removeActivationLocal(IntegrationUtils.context)
    }

    /**
     * With [PowerAuthAlgorithm.LEGACY_P256], the QR operation is signed with a personalized
     * ECDSA key. The parser must report [QROperationSignature.KeyType.PERSONALIZED] key type
     * and the full authorize + OTP verification flow must succeed.
     */
    @Test
    fun testParseWithLegacyP256() {
        val op = IntegrationUtils.createOperation(IntegrationUtils.Companion.Factors.F_2FA)
        val qrData = IntegrationUtils.getQROperation(op)

        val parser = QROperationParser(pa)
        val qrOperation = parser.parse(qrData.operationQrCodeData)

        Assert.assertEquals(op.operationId, qrOperation.operationId)
        Assert.assertEquals(
            "Legacy P256 should use personalized ECDSA key, not MAC",
            QROperationSignature.KeyType.PERSONALIZED,
            qrOperation.signature.keyType
        )

        val auth = PowerAuthAuthentication.possessionWithPassword(pin)
        val otp = ops.authorizeOfflineOperation(qrOperation, auth)
        val verified = IntegrationUtils.verifyQROperation(op, qrData, otp)
        Assert.assertTrue("OTP should be valid for legacy P256 QR operation", verified.otpValid)
    }
}
