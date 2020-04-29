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

package com.wultra.android.mtokensdk.operation

import android.content.Context
import io.getlime.security.powerauth.sdk.PowerAuthAuthentication
import io.getlime.security.powerauth.sdk.PowerAuthAuthorizationHttpHeader
import io.getlime.security.powerauth.sdk.PowerAuthSDK

/**
 * Manager for handling PowerAuth signatures.
 * That means signature header
 * and fingerprint as an authorization method.
 *
 * @author Tomas Kypta, tomas.kypta@wultra.com
 */
class SignatureManager constructor(private val appContext: Context, private val powerAuthSDK: PowerAuthSDK) {

    /**
     * HTTP method for the signature.
     */
    enum class SignatureHttpMethod(val methodName: String) {
        POST("POST")
    }

    /**
     * Get signature header for 1FA (possession).
     *
     * @param method HTTP method
     * @param urlId Id of the url endpoint the signature header is aimed for.
     * @param body Request body the signature header is computed for.
     */
    fun get1FASignatureHeader(method: SignatureHttpMethod,
                              urlId: String,
                              body: ByteArray): PowerAuthAuthorizationHttpHeader {
        val authentication = PowerAuthAuthentication()
        authentication.usePossession = true
        val header = powerAuthSDK.requestSignatureWithAuthentication(appContext, authentication, method.methodName, urlId, body)
        check(header.isValid) { "Cannot create signature header - invalid configuration/activation state or corrupted state data" }
        return header
    }

    /**
     * Get signature header for 2FA (possession + password/biometry).
     *
     * @param method HTTP method
     * @param urlId Id of the url endpoint the signature header is aimed for.
     * @param body Request body the signature header is computed for.
     * @param password Password for computing the signature.
     * @param biometry Biometric key for computing the signature.
     */
    fun get2FASignatureHeader(method: SignatureHttpMethod,
                              urlId: String,
                              body: ByteArray,
                              password: String?,
                              biometry: ByteArray?): PowerAuthAuthorizationHttpHeader {
        val authentication = PowerAuthAuthentication()
        authentication.usePossession = true
        authentication.usePassword = password
        authentication.useBiometry = biometry
        val header = powerAuthSDK.requestSignatureWithAuthentication(appContext, authentication, method.methodName, urlId, body)
        check(header.isValid) { "Cannot create signature header - invalid configuration/activation state or corrupted state data" }
        return header
    }
}