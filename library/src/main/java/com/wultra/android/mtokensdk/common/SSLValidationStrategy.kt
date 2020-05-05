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

package com.wultra.android.mtokensdk.common

import io.getlime.security.powerauth.networking.ssl.PA2ClientSslNoValidationStrategy
import okhttp3.OkHttpClient

/**
 * Strategy for validating SSL certificates.
 * For concrete implementation, use static methods [default], [noValidation] & [sslPinning]
 */
abstract class SSLValidationStrategy {

    companion object {
        /**
         * Default (system) certificate validation
         */
        @JvmStatic
        fun default(): SSLValidationStrategy = DefaultSSLValidationStrategy()

        /**
         * Disables validation of SSL certificate. Use this strategy for HTTP endpoints
         * and self-signed HTTPS endpoints.
         */
        @JvmStatic
        fun noValidation(): SSLValidationStrategy = NoSSLValidationStrategy()

        /**
         * Custom SSL validation strategy.
         */
        @JvmStatic
        fun sslPinning(provider: ISSLPinningProvider): SSLValidationStrategy = PinningSSLValidationStrategy(provider)
    }

    internal abstract fun configure(builder: OkHttpClient.Builder)
}

internal class DefaultSSLValidationStrategy: SSLValidationStrategy() {
    override fun configure(builder: OkHttpClient.Builder) {
        // Nothing to change, use default
    }

}
internal class NoSSLValidationStrategy: SSLValidationStrategy() {
    override fun configure(builder: OkHttpClient.Builder) {
        val pA2ClientValidationStrategy = PA2ClientSslNoValidationStrategy()
        val trustAllCertsTrustManager = TrustAllCertsTrustManager()
        builder.sslSocketFactory(pA2ClientValidationStrategy.sslSocketFactory!!, trustAllCertsTrustManager)
        builder.hostnameVerifier(pA2ClientValidationStrategy.hostnameVerifier!!)
    }

}
internal class PinningSSLValidationStrategy(private val provider: ISSLPinningProvider): SSLValidationStrategy() {
    override fun configure(builder: OkHttpClient.Builder) {
        provider.configureOkHttpClient(builder)
    }
}

