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
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.lang.Exception
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit


class IntegrationUtils {
    companion object {

        val context = ApplicationProvider.getApplicationContext<Context>()
        private val client = OkHttpClient.Builder().build()
        private val gson = Gson()
        private val jsonMediaType = MediaType.parse("application/json; charset=UTF-8")!!

        private val paUrl = getInstrumentationParameter("paServerUrl")
        private val nextStepUrl = getInstrumentationParameter("nextStepServerUrl")
        private val enrollmentUrl = getInstrumentationParameter("enrollmentServerUrl")
        private val operationsUrl = getInstrumentationParameter("operationsServerUrl")
        private val appKey = getInstrumentationParameter("appKey")
        private val appSecret = getInstrumentationParameter("appSecret")
        private val masterPublicKey = getInstrumentationParameter("masterServerPublicKey")
        private val appId = getInstrumentationParameter("appId")
        private val activationName = "mtokenSdkAndroidTests"

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
                    "requestObject": {
                        "activationOtpValidation": "NONE",
                        "applicationId": $appId,
                        "maxFailureCount": 5,
                        "userId": "$activationName"
                    }
                }
                """
            val resp = makeCall<PAObject<PAInitResponseObject>>(body, "$paUrl/rest/v3/activation/init")

            // CREATE ACTIVATION LOCALLY

            val calFuture = CompletableFuture<Any>()
            pa.createActivation("tests", resp.responseObject.activationCode, object : ICreateActivationListener {
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

            val serverCommitBody = """
            {
              "requestObject": {
                "activationId": "${resp.responseObject.activationId}"
              }
            }
            """
            makeCall<PAObject<PACommitObject>>(serverCommitBody, "$paUrl/rest/v3/activation/commit")

            // MAKE ACTIVATION PRIMARY ON THE SERVER

            val primaryBody = """
            {
                "requestObject": {
                    "userId": "$activationName",
                    "authMethod": "POWERAUTH_TOKEN",
                    "config": {
                        "activationId": "${resp.responseObject.activationId}"
                    }
                }
            }
            """
            makeCall<PASimpleObject>(primaryBody, "$nextStepUrl/user/auth-method")

            return Pair(pa, pa.createOperationsService(context, operationsUrl, SSLValidationStrategy.noValidation()))
        }

        @Throws
        fun createOperation(oneFactor: Boolean) {
            val opBody: String
            if (oneFactor) {
                opBody = """
                {
                    "requestObject": {
                        "operationName": "login_sca",
                        "operationData": "A2",
                        "formData": {
                          "title": {
                            "id": "login.title"
                          },
                          "greeting": {
                            "id": "login.greeting"
                          },
                          "summary": {
                            "id": "login.summary"
                          }
                        }
                    }
                }
                """
            } else {
                opBody = """
                {
                  "requestObject": {
                    "operationName": "authorize_payment",
                    "operationId": null,
                    "operationData": "A1*A100CZK*Q238400856/0300**D20170629*NUtility Bill Payment - 05/2017",
                    "params": [],
                    "formData": {
                      "title": {
                        "id": "operation.title",
                        "value": "Charge karta"
                      },
                      "greeting": {
                        "id": "operation.greeting",
                        "value": "Hello"
                      },
                      "summary": {
                        "id": "operation.summary",
                        "value": "Potvrďte platbu."
                      },
                      "config": [],
                      "parameters": [
                        {
                          "type": "AMOUNT",
                          "id": "operation.amount",
                          "label": null,
                          "valueFormatType": "AMOUNT",
                          "formattedValue": null,
                          "amount": 10000000000.99,
                          "currency": "EUR",
                          "currencyId": "operation.currency"
                        },
                        {
                          "type": "HEADING",
                          "id": "operation.heading",
                          "label": "Nadpis",
                          "valueFormatType": "TEXT",
                          "formattedValue": null,
                          "value": "000000-25000377732500037773037773/5800"
                        },
                        {
                          "type": "KEY_VALUE",
                          "id": "operation.account",
                          "label": null,
                          "valueFormatType": "ACCOUNT",
                          "formattedValue": null,
                          "value": "test test test"
                        }
                      ],
                      "dynamicDataLoaded": false,
                      "userInput": {}
                    }
                  }
                }
                """
            }

            // step1: create an operation on the nextstep server
            val op = makeCall<PAObject<PAOperationCreateObject>>(opBody, "$nextStepUrl/operation")

            // step2: assign the operation to the user
            val assignBody = """
            {
              "requestObject": {
                "operationId": "${op.responseObject.operationId}",
                "userId": "$activationName",
                "organizationId": "RETAIL",
                "accountStatus": "ACTIVE"
              }
            }
            """
            makeCall<PASimpleObject>(assignBody, "$nextStepUrl/operation/user/update")

            // step3: if the operation needs to be authorized with
            // a knowledge factor, move it to the "confirmed" state
            if (!oneFactor) {
                val b = """
                {
                  "requestObject": {
                    "operationId": "${op.responseObject.operationId}",
                    "userId": "$activationName",
                    "organizationId": "RETAIL",
                    "authMethod": "USER_ID_ASSIGN",
                    "authStepResult": "CONFIRMED",
                    "authStepResultDescription": null,
                    "params": []
                  }
                }
                """
                makeCall<PASimpleObject>(b, "$nextStepUrl/operation/update")
            }

            // step4: make the op "approvable" by the mobile token
            val appBody = """
            {
              "requestObject": {
                "operationId": "${op.responseObject.operationId}",
                "mobileTokenActive": true
              }
            }
            """
            makeCall<PASimpleObject>(appBody,  "$nextStepUrl/operation/mobileToken/status/update")

            // step5: step auth method to the operation
            val authBody = """
            {
              "requestObject": {
                "operationId": "${op.responseObject.operationId}",
                "chosenAuthMethod": "${if (oneFactor) "LOGIN_SCA" else "POWERAUTH_TOKEN"}"
              }
            }
            """
            makeCall<PASimpleObject>(authBody, "$nextStepUrl/operation/chosenAuthMethod/update")
        }

        @Throws
        private inline fun <reified T> makeCall(payload: String, url: String): T {
            val bodyBytes = payload.toByteArray()
            val body = RequestBody.create(jsonMediaType, bodyBytes)
            val request = Request.Builder()
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

data class PASimpleObject(val status: String)

data class PAObject<T>(val responseObject: T,
                       val status: String)

data class PAInitResponseObject(
        val activationId: String,
        val activationCode: String,
        val activationSignature: String,
        val userId: String,
        val applicationId: Int)

data class PACommitObject(val activated: Boolean, val activationId: String)

data class PAOperationCreateObject(val operationId: String)