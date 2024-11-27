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

@file:Suppress("DEPRECATION")

package com.wultra.android.mtokensdk.test

import com.wultra.android.mtokensdk.api.operation.model.UserOperation
import com.wultra.android.mtokensdk.operation.*
import io.getlime.security.powerauth.networking.response.IActivationRemoveListener
import io.getlime.security.powerauth.sdk.PowerAuthAuthentication
import io.getlime.security.powerauth.sdk.PowerAuthSDK
import org.junit.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

/**
 * Integration tests are calling a real backend server (based on configuration inside the "${ROOT_FOLDER}/configs/integration-tests.properties" file).
 */
class IntegrationTestsDeprecated {

    companion object {

        lateinit var ops: IOperationsService
        private lateinit var pa: PowerAuthSDK
        const val pin = "1234"

        @BeforeClass
        @JvmStatic
        fun setup() {
            try {
                val result = IntegrationUtils.prepareActivation(pin)
                pa = result.first
                ops = result.second
            } catch (e: Throwable) {
                Assert.fail("Activation preparation failed: $e")
            }
        }

        @AfterClass
        @JvmStatic
        fun tearDown() {
            val auth = PowerAuthAuthentication.possessionWithPassword(pin)
            val future = CompletableFuture<Any>()
            pa.removeActivationWithAuthentication(
                IntegrationUtils.context,
                auth,
                object : IActivationRemoveListener {
                    override fun onActivationRemoveSucceed() {
                        future.complete(null)
                    }
                    override fun onActivationRemoveFailed(t: Throwable) {
                        future.completeExceptionally(t)
                    }
                }
            )
            future.get(20, TimeUnit.SECONDS)
        }
    }

    @Test
    fun testRejectPayment() {
        val op = IntegrationUtils.createOperation(IntegrationUtils.Companion.Factors.F_2FA)
        val future = CompletableFuture<List<UserOperation>>()
        ops.getOperations { result ->
            result.onSuccess { future.complete(it) }
                .onFailure { future.completeExceptionally(it) }
        }
        val operations = future.get(20, TimeUnit.SECONDS)
        val opFromList = operations.firstOrNull { it.id == op.operationId }
        if (opFromList == null) {
            Assert.fail("Operation was not in the list")
            return
        }
        val opFuture = CompletableFuture<Any?>()
        ops.rejectOperation(opFromList, RejectionReason.INCORRECT_DATA) { result ->
            result.onSuccess { opFuture.complete(null) }
                .onFailure { opFuture.completeExceptionally(it) }
        }
        Assert.assertNull(opFuture.get(20, TimeUnit.SECONDS))
    }
}
