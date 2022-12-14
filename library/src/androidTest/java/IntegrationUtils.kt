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

package com.wultra.android.mtokensdk.test

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.wultra.android.mtokensdk.operation.IOperationsService
import com.wultra.android.mtokensdk.operation.createOperationsService
import com.wultra.android.powerauth.networking.ssl.SSLValidationStrategy
import io.getlime.security.powerauth.core.ActivationCodeUtil
import io.getlime.security.powerauth.networking.response.CreateActivationResult
import io.getlime.security.powerauth.networking.response.ICreateActivationListener
import io.getlime.security.powerauth.sdk.PowerAuthClientConfiguration
import io.getlime.security.powerauth.sdk.PowerAuthConfiguration
import io.getlime.security.powerauth.sdk.PowerAuthSDK
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.lang.Exception
import java.util.Base64.getEncoder
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit


class IntegrationUtils {
    companion object {

        val context: Context = ApplicationProvider.getApplicationContext()
        private val client = OkHttpClient.Builder().build()
        private val gson = Gson()
        private val jsonMediaType = MediaType.parse("application/json; charset=UTF-8")!!

        private val cloudServerUrl = getInstrumentationParameter("cloudServerUrl")
        private val cloudServerLogin = getInstrumentationParameter("cloudServerLogin")
        private val cloudServerPassword = getInstrumentationParameter("cloudServerPassword")
        private val cloudApplicationId = getInstrumentationParameter("cloudApplicationId")
        private val enrollmentUrl = getInstrumentationParameter("enrollmentServerUrl")
        private val operationsUrl = getInstrumentationParameter("operationsServerUrl")
        private val appKey = getInstrumentationParameter("appKey")
        private val appSecret = getInstrumentationParameter("appSecret")
        private val masterPublicKey = getInstrumentationParameter("masterServerPublicKey")
        private val activationName = UUID.randomUUID().toString()
        private var registrationId = "" // will be filled when activation is created

        @Throws
        fun prepareActivation(pin: String): Pair<PowerAuthSDK, IOperationsService> {

            // CREATE PA INSTANCE

            val cfg = PowerAuthConfiguration.Builder("tests", enrollmentUrl, appKey, appSecret, masterPublicKey).build()
            val clientCfg = PowerAuthClientConfiguration.Builder().allowUnsecuredConnection(true).build()
            val pa = PowerAuthSDK.Builder(cfg).clientConfiguration(clientCfg).build(context)

            // REMOVE LOCAL INSTANCE IF PRESENT

            pa.removeActivationLocal(context)

            // CREATE ACTIVATION ON THE SERVER

            val body = """
                {
                  "userId": "$activationName",
                  "flags": [],
                  "appId": "$cloudApplicationId"
                }
                """.trimIndent()
            val resp = makeCall<RegistrationObject>(body, "$cloudServerUrl/v2/registrations")

            registrationId = resp.registrationId

            // CREATE ACTIVATION LOCALLY

            val calFuture = CompletableFuture<Any>()
            pa.createActivation("tests", resp.activationCode(), object : ICreateActivationListener {
                override fun onActivationCreateFailed(t: Throwable) {
                    calFuture.completeExceptionally(t)
                }

                override fun onActivationCreateSucceed(result: CreateActivationResult) {
                    calFuture.complete(null)
                }
            })
            calFuture.get(10, TimeUnit.SECONDS)

            // COMMIT ACTIVATION LOCALLY

            val result = pa.commitActivationWithPassword(context, pin)
            Log.d("prepare activaiton", "commitActivationWithPassword result: $result")

            // COMMIT ACTIVATION ON THE SERVER
            val bodyCommit = """
                {
                  "externalUserId": "test"
                }
                """.trimIndent()
            makeCall<CommitObject>(bodyCommit, "$cloudServerUrl/v2/registrations/${resp.registrationId}/commit")

            return Pair(pa, pa.createOperationsService(context, operationsUrl, SSLValidationStrategy.default()))
        }

        enum class Factors {
            //F_1FA,
            F_2FA
        }

        @Throws
        fun createOperation(factors: Factors): OperationObject {
            val opBody = when (factors) {
                Factors.F_2FA -> { """
                {
                  "userId": "$activationName",
                  "template": "login",
                   "parameters": {
                     "party.id": "666",
                     "party.name": "Datová schránka",
                         "session.id": "123",
                         "session.ip-address": "192.168.0.1"
                   }
                }
                """.trimIndent()
                }
            }

            // create an operation on the nextstep server
            return makeCall(opBody, "$cloudServerUrl/v2/operations")
        }

        @Throws
        fun getQROperation(operation: OperationObject): QRData {
            return makeCall(null, "$cloudServerUrl/v2/operations/${operation.operationId}/offline/qr?registrationId=$registrationId", "GET")
        }

        @Throws
        fun verifyQROperation(operation: OperationObject, qrData: QRData, otp: String): QROperationVerify {
            val body = """
                {
                  "otp": "$otp",
                  "nonce": "${qrData.nonce}",
                  "registrationId": "$registrationId"
                }
            """.trimIndent()
            return makeCall(body, "$cloudServerUrl/v2/operations/${operation.operationId}/offline/otp")
        }

        @Throws
        private inline fun <reified T> makeCall(payload: String?, url: String, method: String = "POST"): T {
            Log.d("make call payload", payload ?: "")
            Log.d("make call url", url)
            val creds = getEncoder().encodeToString("$cloudServerLogin:$cloudServerPassword".toByteArray())
            val body = if (payload != null) {
                RequestBody.create(jsonMediaType, payload.toByteArray())
            } else {
                null
            }
            val request = Request.Builder()
                    .header("authorization", "Basic $creds")
                    .url(url)
                    .method(method, body)
                    .build()
            val resp = client.newCall(request).execute()
            val stringResp = resp.body()!!.string()
            Log.d("make call response", stringResp)
            return gson.fromJson(stringResp, object: TypeToken<T>(){}.type)
        }

        @Throws
        private fun getInstrumentationParameter(parameterName: String): String {
            return InstrumentationRegistry.getArguments().getString("tests.sdk.$parameterName") ?: throw Exception("Missing $parameterName in configuration.")
        }
    }
}

data class RegistrationObject(val activationQrCodeData: String, val registrationId: String) {
    fun activationCode(): String = ActivationCodeUtil.parseFromActivationCode(activationQrCodeData)!!.activationCode
}

data class CommitObject(val status: String)

data class OperationObject(val operationId: String,
                           val userId: String,
                           val status : String,
                           val operationType: String,
                           //val parameters: [] // not needed for test right now
                           val failureCount: Int,
                           val maxFailureCount: Int,
                           val timestampCreated: Double,
                           val timestampExpires: Double)

data class QRData(val operationQrCodeData: String,
                  val nonce: String)

data class QROperationVerify(val otpValid: Boolean,
                             val userId: String,
                             val registrationId: String,
                             val registrationStatus: String,
                             val signatureType: String,
                             val remainingAttempts: Int
                            // val flags: []
                            // val application)
)