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
import com.wultra.android.mtokensdk.api.apiCoroutineScope
import io.getlime.security.powerauth.networking.response.IGetTokenListener
import io.getlime.security.powerauth.sdk.PowerAuthAuthentication
import io.getlime.security.powerauth.sdk.PowerAuthToken
import io.getlime.security.powerauth.sdk.PowerAuthTokenStore
import io.getlime.security.powerauth.sdk.PowerAuthAuthorizationHttpHeader
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import java.lang.Exception
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Manager for PowerAuth token header handling.
 *
 * @author Tomas Kypta, tomas.kypta@wultra.com
 */
class TokenManager constructor(private val appContext: Context, private val powerAuthTokenStore: PowerAuthTokenStore) {

    companion object {
        const val TOKEN_NAME = "possession_universal"
    }

    /**
     * Get PowerAuth token header or prepare a new one if it doesn't exist.
     */
    fun getOrPreparePowerAuthTokenHeader(): PowerAuthAuthorizationHttpHeader {
        val powerAuthTokenHeader = runBlocking {
            getPowerAuthTokenHeader()
        }
        if (powerAuthTokenHeader == null || !powerAuthTokenHeader.isValid) {
            throw IllegalStateException("Cannot obtain PowerAuth token")
        } else {
            return powerAuthTokenHeader
        }
    }

    /**
     * Prepare header from either locally stored token or newly requested token from the backend.
     */
     private suspend fun getPowerAuthTokenHeader(): PowerAuthAuthorizationHttpHeader? {
        val localPowerAuthToken = powerAuthTokenStore.getLocalToken(appContext, TOKEN_NAME)
        if (localPowerAuthToken != null) {
            return localPowerAuthToken.generateHeader()
        }
        val powerAuthTokenDeferred = apiCoroutineScope.async { launchRequestPowerAuthAccessToken() }
        val token = powerAuthTokenDeferred.await()
        return token?.generateHeader()
    }

    private suspend fun launchRequestPowerAuthAccessToken(): PowerAuthToken? {
        val powerAuthAuthentication = PowerAuthAuthentication()
        powerAuthAuthentication.usePossession = true
        try {
            return requestPowerAuthAccessToken(authentication = powerAuthAuthentication)
        } catch (e: Exception) {
            return null
        }
    }

    private suspend fun requestPowerAuthAccessToken(authentication: PowerAuthAuthentication): PowerAuthToken =
            suspendCoroutine { cont ->
                powerAuthTokenStore.requestAccessToken(appContext, TOKEN_NAME, authentication, object : IGetTokenListener {
                    override fun onGetTokenSucceeded(token: PowerAuthToken) {
                        cont.resume(token)
                    }

                    override fun onGetTokenFailed(t: Throwable) {
                        cont.resumeWithException(t)
                    }
                })
            }


}