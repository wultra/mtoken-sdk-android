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
import com.google.gson.*
import com.google.gson.annotations.JsonAdapter
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import com.wultra.android.mtokensdk.inbox.IInboxService
import com.wultra.android.mtokensdk.inbox.createInboxService
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
import java.util.*
import java.util.Base64.getEncoder
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class TimestampAdapter: TypeAdapter<Date>() {
    override fun write(writer: JsonWriter, value: Date?) {
        if (value == null) {
            writer.nullValue()
        } else {
            writer.value(value.time)
        }
    }

    override fun read(reader: JsonReader): Date? {
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull()
            return null
        }
        return Date(reader.nextLong())
    }
}

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
        private val inboxUrl = getInstrumentationParameter("inboxServerUrl")
        private val sdkConfig = getInstrumentationParameter("sdkConfig")
        private var activationName = "" // will be filled when activation is created
        private var registrationId = "" // will be filled when activation is created

        @Throws
        fun prepareActivation(pin: String, userId: String? = null): Triple<PowerAuthSDK, IOperationsService, IInboxService> {

            // Be sure that each activation has its own user
            activationName = userId ?: UUID.randomUUID().toString()

            // Be sure that each activation has its own user
            activationName = UUID.randomUUID().toString()

            // CREATE PA INSTANCE

            val cfg = PowerAuthConfiguration.Builder("tests", enrollmentUrl, sdkConfig).build()
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
                """
                .trimIndent()
            val resp = makeCall<RegistrationObject>(body, "$cloudServerUrl/v2/registrations")

            registrationId = resp.registrationId

            // CREATE ACTIVATION LOCALLY

            val calFuture = CompletableFuture<Any>()
            pa.createActivation(
                "tests",
                resp.activationCode(),
                object : ICreateActivationListener {
                    override fun onActivationCreateFailed(t: Throwable) {
                        calFuture.completeExceptionally(t)
                    }

                    override fun onActivationCreateSucceed(result: CreateActivationResult) {
                        calFuture.complete(null)
                    }
                }
            )
            calFuture.get(10, TimeUnit.SECONDS)

            // COMMIT ACTIVATION LOCALLY

            val result = pa.persistActivationWithPassword(context, pin)
            Log.d("prepare activation", "commitActivationWithPassword result: $result")

            // COMMIT ACTIVATION ON THE SERVER
            val bodyCommit = """
                {
                  "externalUserId": "test"
                }
                """
                .trimIndent()
            makeCall<CommitObject>(bodyCommit, "$cloudServerUrl/v2/registrations/${resp.registrationId}/commit")

            return Triple(
                pa,
                pa.createOperationsService(context, operationsUrl, SSLValidationStrategy.default()),
                pa.createInboxService(context, inboxUrl, SSLValidationStrategy.default())
            )
        }

        @Throws
        fun removeRegistration(activationId: String? = null) {
            val id = activationId ?: registrationId
            if (id.isNotEmpty()) {
                makeCall<StatusResponse>(null, "$cloudServerUrl/v2/registrations/$id", "DELETE")
            }
        }

        enum class Factors {
            // F_1FA,
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
        fun createNonPersonalizedPACOperation(factors: Factors): NonPersonalisedTOTPOperationObject {
            val opBody = when (factors) {
                Factors.F_2FA -> { """
                {
                  "template": "login_preApproval",
                  "proximityCheckEnabled": true,
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
        fun getOperation(operation: NonPersonalisedTOTPOperationObject): NonPersonalisedTOTPOperationObject {
            return makeCall(null, "$cloudServerUrl/v2/operations/${operation.operationId}", "GET")
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
        fun createInboxMessages(count: Int, type: String = "text"): List<NewInboxMessage> {
            val result = mutableListOf<NewInboxMessage>()
            for (i in 1..count) {
                val body = """
                    {
                        "userId":"$activationName",
                        "subject":"Message #$i",
                        "summary":"This is body for message $i",
                        "body":"This is body for message $i",
                        "type":"$type",
                        "silent":true
                    }
                """.trimIndent()
                val newMessage = makeCall<NewInboxMessage>(body, "$cloudServerUrl/v2/inbox/messages")
                result.add(newMessage)
            }
            return result
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
            return gson.fromJson(stringResp, object: TypeToken<T>() {}.type)
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

data class OperationObject(
    val operationId: String,
    val userId: String,
    val status : String,
    val operationType: String,
    // val parameters: [] // not needed for test right now
    val failureCount: Int,
    val maxFailureCount: Int,
    val timestampCreated: Double,
    val timestampExpires: Double
)

data class NonPersonalisedTOTPOperationObject(
    val operationId: String,
    val status: String,
    val operationType: String,
    val failureCount: Int,
    val maxFailureCount: Int,
    val timestampCreated: Double,
    val timestampExpires: Double,
    val proximityOtp: String?
)


data class QRData(
    val operationQrCodeData: String,
    val nonce: String
)

data class QROperationVerify(
    val otpValid: Boolean,
    val userId: String,
    val registrationId: String,
    val registrationStatus: String,
    val signatureType: String,
    val remainingAttempts: Int
    // val flags: []
    // val application)
)

data class NewInboxMessage(
    val id: String,
    val subject: String,
    val summary: String,
    val body: String,
    val read: Boolean,
    val type: String,
    @JsonAdapter(TimestampAdapter::class)
    val timestamp: Date
)

data class StatusResponse(val status: String)
