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

package com.wultra.android.mtokensdk.test

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.wultra.android.mtokensdk.common.SSLValidationStrategy
import com.wultra.android.mtokensdk.operation.IOperationsService
import com.wultra.android.mtokensdk.operation.createOperationsService
import io.getlime.security.powerauth.networking.response.CreateActivationResult
import io.getlime.security.powerauth.networking.response.ICreateActivationListener
import io.getlime.security.powerauth.sdk.PowerAuthClientConfiguration
import io.getlime.security.powerauth.sdk.PowerAuthConfiguration
import io.getlime.security.powerauth.sdk.PowerAuthSDK
import io.getlime.security.powerauth.util.otp.OtpUtil
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

        val context = ApplicationProvider.getApplicationContext<Context>()
        private val client = OkHttpClient.Builder().build()
        private val gson = Gson()
        private val jsonMediaType = MediaType.parse("application/json; charset=UTF-8")!!

        private val cloudServerUrl = getInstrumentationParameter("cloudServerUrl")
        private val cloudServerLogin = getInstrumentationParameter("cloudServerLogin")
        private val cloudServerPassword = getInstrumentationParameter("cloudServerPassword")
        private val enrollmentUrl = getInstrumentationParameter("enrollmentServerUrl")
        private val operationsUrl = getInstrumentationParameter("operationsServerUrl")
        private val appKey = getInstrumentationParameter("appKey")
        private val appSecret = getInstrumentationParameter("appSecret")
        private val masterPublicKey = getInstrumentationParameter("masterServerPublicKey")
        private val activationName = UUID.randomUUID().toString()

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
                  "userId": "$activationName"
                }
                """
            val resp = makeCall<RegistrationObject>(body, "$cloudServerUrl/registration")

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

            pa.commitActivationWithPassword(context, pin)

            // COMMIT ACTIVATION ON THE SERVER
            makeCall<CommitObject>(body, "$cloudServerUrl/registration/commit")

            return Pair(pa, pa.createOperationsService(context, operationsUrl, SSLValidationStrategy.noValidation()))
        }

        enum class Factors {
            //F_1FA,
            F_2FA
        }

        @Throws
        fun createOperation(factors: Factors) {
            val opBody = when (factors) {
                Factors.F_2FA -> { """
                {
                  "userId": "$activationName",
                  "template": "login-tpp",
                   "parameters": {
                     "party.id": "666",
                     "party.name": "Datová schránka",
                         "session.id": "123",
                         "session.ip-address": "192.168.0.1"
                   }
                }
                """
                }
            }

            // create an operation on the nextstep server
            makeCall<OperationObject>(opBody, "$cloudServerUrl/operations")
        }

        @Throws
        private inline fun <reified T> makeCall(payload: String, url: String): T {
            val creds = getEncoder().encodeToString("$cloudServerLogin:$cloudServerPassword".toByteArray())
            val bodyBytes = payload.toByteArray()
            val body = RequestBody.create(jsonMediaType, bodyBytes)
            val request = Request.Builder()
                    .header("authorization", "Basic $creds")
                    .url(url)
                    .post(body)
                    .build()
            val resp = client.newCall(request).execute()
            return gson.fromJson<T>(resp.body()!!.string(), object: TypeToken<T>(){}.type)
        }

        @Throws
        private fun getInstrumentationParameter(parameterName: String): String {
            return InstrumentationRegistry.getArguments().getString("tests.sdk.$parameterName") ?: throw Exception("Missing $parameterName in configuration.")
        }
    }
}

data class RegistrationObject(val activationQrCodeData: String) {
    fun activationCode(): String = OtpUtil.parseFromActivationCode(activationQrCodeData)!!.activationCode
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