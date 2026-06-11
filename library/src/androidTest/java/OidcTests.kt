/*
 * Copyright 2025 Wultra s.r.o.
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

import com.wultra.android.mtokensdk.oidc.OIDCService
import com.wultra.android.mtokensdk.oidc.models.OIDCAuthorizationRequest
import com.wultra.android.mtokensdk.oidc.models.OIDCConfig
import com.wultra.android.powerauth.networking.error.ApiErrorException
import com.wultra.android.powerauth.networking.error.ApiHttpException
import io.getlime.security.powerauth.sdk.PowerAuthSDK
import org.junit.*
import org.junit.Assume.assumeTrue
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class OidcTests {

    private lateinit var pa: PowerAuthSDK
    lateinit var oidc: OIDCService

    @Before
    fun setup() {
        val result = IntegrationUtils.prepareForOIDC()
        pa = result.first
        oidc = result.second
    }

    @After
    fun tearDown() {
        if (!::pa.isInitialized) {
            return
        }
        IntegrationUtils.removeRegistration(pa.activationIdentifier)
        pa.removeActivationLocal(IntegrationUtils.context)
    }

    @Test
    fun testGetConfigFails() {
        val nonValidProviderId = "xxx"

        val future = CompletableFuture<Throwable>()
        oidc.getConfig(nonValidProviderId) { result ->
            result
                .onSuccess { Assert.fail("ProviderId shouldn't exist") }
                .onFailure { error ->
                    future.complete(error)
                }
        }

        val error = future.get(20, TimeUnit.SECONDS)

        // Assert the type of the error or its message
        Assert.assertNotNull("Error should not be null", error)
        Assert.assertEquals(
            "Expected error code 400 for nonexistent provider",
            400,
            ((error as ApiErrorException).cause as ApiHttpException).code
        )
    }

    @Test
    fun testGetConfigSucceed() {
        val validProvider = IntegrationUtils.getOIDCProps().providerId

        // Skip the test if the provider ID is empty
        assumeTrue("Skipping test: OIDC providerId is not set", validProvider.isNotEmpty())

        val future = CompletableFuture<OIDCConfig>()
        oidc.getConfig(validProvider) { result ->
            result
                .onSuccess { future.complete(it) }
                .onFailure { future.completeExceptionally(it) }
        }

        val config = future.get(20, TimeUnit.SECONDS)

        Assert.assertNotNull(config.authorizeUri)
        Assert.assertNotNull(config.providerId)
        Assert.assertNotNull(config.scopes)
        Assert.assertNotNull(config.clientId)
        Assert.assertNotNull(config.redirectUri)
        Assert.assertTrue(!config.pkceEnabled)
    }

    @Test
    fun testGetConfigPKCESucceed() {
        val validPKCEProviderId = IntegrationUtils.getOIDCProps().providerIdPkce

        // Skip the test if the provider ID is empty
        assumeTrue("Skipping test: OIDC providerId is not set", validPKCEProviderId.isNotEmpty())

        val future = CompletableFuture<OIDCConfig>()
        oidc.getConfig(validPKCEProviderId) { result ->
            result
                .onSuccess { future.complete(it) }
                .onFailure { future.completeExceptionally(it) }
        }

        val config = future.get(20, TimeUnit.SECONDS)

        Assert.assertTrue(config.pkceEnabled)
    }

    @Test
    fun testOidcPreparesAuthorizationData() {
        val providerIdPkce = IntegrationUtils.getOIDCProps().providerIdPkce

        // Skip the test if the provider ID is empty
        assumeTrue("Skipping test: OIDC providerIdPkce is not set", providerIdPkce.isNotEmpty())

        val configFuture = CompletableFuture<OIDCConfig>()
        oidc.getConfig(providerIdPkce) { result ->
            result
                .onSuccess { configFuture.complete(it) }
                .onFailure { configFuture.completeExceptionally(it) }
        }

        val config = configFuture.get(20, TimeUnit.SECONDS)
        Assert.assertNotNull("Config should not be null", config)

        val oidcAuthDataFuture = CompletableFuture<OIDCAuthorizationRequest>()
        oidc.prepareAuthorizationData(config) { data ->
            data
                .onSuccess { oidcAuthDataFuture.complete(it) }
                .onFailure { oidcAuthDataFuture.completeExceptionally(it) }
        }

        val oidcAuthData = oidcAuthDataFuture.get(5, TimeUnit.SECONDS)
        Assert.assertEquals(providerIdPkce, oidcAuthData.providerId)
        Assert.assertNotNull("Authorization URI should not be null", oidcAuthData.authorizeUri)
        Assert.assertNotNull("State should not be null", oidcAuthData.state)
        Assert.assertNotNull("Nonce should not be null", oidcAuthData.nonce)
        Assert.assertNotNull("Code verifier should not be null", oidcAuthData.codeVerifier)
    }

    /**
     * The entire OIDC activation flow is highly dependent on third-party implementations,
     * making it challenging to provide a universal solution. This flow was tested using
     * our configuration with `auth0.com`. Below is a summary of the process outside our system:
     *
     * 1. get authorize uri -> from redirect get uri and authState
     * 2. post login uri with body - username, password, authState - from redirect get resume uri
     * 3. get resume uri and get deeplink uri with code
     *
     * While `OkHttpClient` is widely used throughout this SDK, handling complex flows
     * involving multiple redirects and mixed HTTP methods (e.g., GET → redirect → POST →
     * redirect → GET) proved challenging to implement. To simplify this process
     * the `Ktor Client` was used instead.
     *
     * Following Ktor Dependencies were used
     *
     * implementation("io.ktor:ktor-client-core:2.3.4")
     * implementation("io.ktor:ktor-client-okhttp:2.3.4")
     * implementation("io.ktor:ktor-client-content-negotiation:2.3.4")
     * implementation("io.ktor:ktor-client-cio:2.3.0")
     *
     * You must also provide username and password of your testing auth0 account
     */
//    @Test
//    fun testOidcActivationFlow() {
//        val username =
//        val password =
//        val props = IntegrationUtils.getOidcProps()
//        if (props.providerIdPkce == "") {
//            WMTLogger.w("If you want to test OIDC provide a valid providerId in the integration-tests.properties")
//            return
//        }
//        val configFuture = CompletableFuture<OidcConfig>()
//        oidc.getConfig(props.providerIdPkce) { result ->
//            result
//                .onSuccess { configFuture.complete(it) }
//                .onFailure { configFuture.completeExceptionally(it) }
//        }
//        val config = configFuture.get(20, TimeUnit.SECONDS)
//        val oidcAuthDataFuture = CompletableFuture<OIDCAuthorizationRequest>()
//        oidc.prepareAuthorizationData(config) { data ->
//            data
//                .onSuccess { oidcAuthDataFuture.complete(it) }
//                .onFailure { oidcAuthDataFuture.completeExceptionally(it) }
//        }
//        val oidcAuthData = oidcAuthDataFuture.get(5, TimeUnit.SECONDS)
//
//        // Perform the login step
//        try {
//            val redirectUri = loginWithAuth0Ktor(oidcAuthData.authorizeUri, username, password)
//            Assert.assertNotNull(redirectUri)
//
//            val paActivationAttributes = OIDCUtils.processDeeplink(redirectUri, oidcAuthData)
//
//            if (paActivationAttributes != null) {
//                pa.createOIDCActivation(
//                    paActivationAttributes,
//                    object : ICreateActivationListener {
//                        override fun onActivationCreateSucceed(result: CreateActivationResult) {
//                            Assert.assertNotNull(result)
//                        }
//
//                        override fun onActivationCreateFailed(t: Throwable) {
//                            Assert.fail("Failed to create activation with exception: ${t.message}")
//                        }
//                    }
//                )
//            } else {
//                Assert.fail("Failed to make PowerAuth activation attributes.")
//            }
//        } catch (e: Exception) {
//            Assert.fail("Failed with exception: ${e.message}")
//        }
//    }
//
//    private fun createKtorClient(): HttpClient {
//        val cookieStorage = AcceptAllCookiesStorage() // Automatically accept all cookies
//        return HttpClient(CIO) {
//            install(HttpCookies) {
//                storage = cookieStorage
//            }
//            install(HttpTimeout) {
//                requestTimeoutMillis = 30000
//            }
//            followRedirects = false
//        }
//    }
//
//    private fun loginWithAuth0Ktor(authorizeUri: Uri, username: String, password: String): Uri {
//        // Initialize the Ktor client with the CIO engine
//        val client = createKtorClient()
//
//        try {
//            // Step 1: Call the Authorization URL to get the login redirect
//            val initialResponse = runBlocking {
//                client.get {
//                    url(authorizeUri.toString())
//                    header(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
//                }
//            }
//
//            val authRedirectUri = initialResponse.headers[HttpHeaders.Location]?.let { Uri.parse(it) }
//                ?: throw IllegalStateException("Redirect login URI not found.")
//
//            val authState = authRedirectUri.getQueryParameter("state")
//                ?: throw IllegalStateException("State parameter not found in redirect login URI.")
//
//            val baseLoginUrl = Uri.Builder()
//                .scheme(authorizeUri.scheme)
//                .authority(authorizeUri.host)
//                .build()
//
//            // Step 2: Perform the Login
//            val loginResponse = runBlocking {
//                client.request {
//                    url(baseLoginUrl.toString() + authRedirectUri)
//                    method = HttpMethod.Post
//                    header(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
//                    setBody(FormDataContent(Parameters.build {
//                        append("username", username)
//                        append("password", password)
//                        append("state", authState)
//                    }))
//                }
//            }
//
//            if (loginResponse.status.value in 300..399) {
//                println("Redirect response received.")
//            } else {
//                throw IllegalStateException("Login failed with HTTP ${loginResponse.status.value}")
//            }
//
//            val finalRedirectUri = loginResponse.headers[HttpHeaders.Location]?.let { Uri.parse(it) }
//                ?: throw IllegalStateException("Final redirect URI not found.")
//
//            // Step 3: Resume Login to Extract the Code
//            val resumeResponse = runBlocking {
//                client.get {
//                    url(baseLoginUrl.toString() + finalRedirectUri)
//                }
//            }
//
//            val resumeRedirectUri = resumeResponse.headers[HttpHeaders.Location]?.let { Uri.parse(it) }
//                ?: throw IllegalStateException("Resume redirect URI not found.")
//
//            // Extract the authorization code
//            return resumeRedirectUri
//        } finally {
//            client.close() // Clean up the client resources
//        }
//    }
}
