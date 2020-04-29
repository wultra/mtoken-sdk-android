/*
 * Copyright (c) 2018, Wultra s.r.o. (www.wultra.com).
 *
 * All rights reserved. This source code can be used only for purposes specified
 * by the given license contract signed by the rightful deputy of Wultra s.r.o.
 * This source code can be used only by the owner of the license.
 *
 * Any disputes arising in respect of this agreement (license) shall be brought
 * before the Municipal Court of Prague.
 */

package com.wultra.android.mtokensdk.api.operation

import com.wultra.android.mtokensdk.api.Api
import com.wultra.android.mtokensdk.api.GsonRequestBodyBytes
import com.wultra.android.mtokensdk.api.general.StatusResponse
import com.wultra.android.mtokensdk.api.operation.model.AllowedSignatureType
import com.wultra.android.mtokensdk.api.operation.model.AuthorizeRequest
import com.wultra.android.mtokensdk.api.operation.model.OperationListResponse
import com.wultra.android.mtokensdk.api.operation.model.RejectRequest
import com.wultra.android.mtokensdk.operation.SignatureManager
import com.wultra.android.mtokensdk.operation.TokenManager
import kotlinx.coroutines.Deferred
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody

/**
 * API for operations requests.
 *
 * @author Tomas Kypta, tomas.kypta@wultra.com
 */
class OperationApi constructor(okHttpClient: OkHttpClient,
                               private val tokenManager: TokenManager,
                               private val signatureManager: SignatureManager) : Api(okHttpClient) {

    // TODO: configurable
    var acceptLanguage = "en"

    companion object {

        // TODO: configurable
        val baseUrl = "http://localhost"

        val LIST_URL = constructApiUrl(baseUrl,"api/auth/token/app/operation/list")
        val AUTHORIZE_URL = constructApiUrl(baseUrl,"api/auth/token/app/operation/authorize")
        const val AUTHORIZE_URL_ID = "/operation/authorize"
        val REJECT_URL = constructApiUrl(baseUrl,"api/auth/token/app/operation/cancel")
        const val REJECT_URL_ID = "/operation/cancel"
        const val OFFLINE_AUTHORIZE_URL_ID = "/operation/authorize/offline"
    }

    /**
     * List pending operations.
     */
    fun list(): Deferred<OperationListResponse> {
        val json: String = "{}"
        val body = RequestBody.create(JSON_MEDIA_TYPE, json)
        val tokenHeader = tokenManager.getOrPreparePowerAuthTokenHeader()
        val request = Request.Builder()
                .url(LIST_URL)
                .post(body)
                .header("Accept-Language", acceptLanguage)
                .header(tokenHeader.key, tokenHeader.value)
                .build()

        return makeCall(request)
    }

    /**
     * Reject an operation.
     */
    fun reject(rejectRequest: RejectRequest): Deferred<StatusResponse> {
        val gson = getGson()
        val typeAdapter = getTypeAdapter<RejectRequest>(gson)
        val bodyBytes = GsonRequestBodyBytes(gson, typeAdapter).convert(rejectRequest)
        val authorizationHeader = signatureManager.get1FASignatureHeader(
                SignatureManager.SignatureHttpMethod.POST, REJECT_URL_ID, bodyBytes)
        val body = RequestBody.create(JSON_MEDIA_TYPE, bodyBytes)
        val request = Request.Builder()
                .url(REJECT_URL)
                .post(body)
                .header(authorizationHeader.key, authorizationHeader.value)
                .build()
        return makeCall(request)
    }

    /**
     * Authorize an operation.
     */
    fun authorize(authorizeRequest: AuthorizeRequest, signatureType: AllowedSignatureType.Type, password: String?, biometry: ByteArray?): Deferred<StatusResponse> {
        val gson = getGson()
        val typeAdapter = getTypeAdapter<AuthorizeRequest>(gson)
        val bodyBytes = GsonRequestBodyBytes(gson, typeAdapter).convert(authorizeRequest)
        val authorizationHeader = when (signatureType) {
            AllowedSignatureType.Type.MULTIFACTOR_1FA -> {
                signatureManager.get1FASignatureHeader(SignatureManager.SignatureHttpMethod.POST,
                        AUTHORIZE_URL_ID, bodyBytes)
            }
            AllowedSignatureType.Type.MULTIFACTOR_2FA -> {
                signatureManager.get2FASignatureHeader(
                        SignatureManager.SignatureHttpMethod.POST, AUTHORIZE_URL_ID, bodyBytes,
                        password, biometry)
            }
            else -> {
                throw IllegalArgumentException("Unsupported signature type: ${signatureType.type}")
            }
        }
        val body = RequestBody.create(JSON_MEDIA_TYPE, bodyBytes)
        val request = Request.Builder()
                .url(AUTHORIZE_URL)
                .post(body)
                .header(authorizationHeader.key, authorizationHeader.value)
                .build()
        return makeCall(request)
    }
}