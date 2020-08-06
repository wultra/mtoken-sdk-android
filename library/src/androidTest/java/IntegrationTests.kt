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

import com.wultra.android.mtokensdk.api.general.ApiError
import com.wultra.android.mtokensdk.api.operation.model.UserOperation
import com.wultra.android.mtokensdk.operation.*
import io.getlime.security.powerauth.networking.response.IActivationRemoveListener
import io.getlime.security.powerauth.sdk.PowerAuthAuthentication
import io.getlime.security.powerauth.sdk.PowerAuthSDK
import org.junit.AfterClass
import org.junit.Assert
import org.junit.BeforeClass
import org.junit.Test
import java.lang.Exception
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

/**
 * Integration tests are calling a real backend server (based on configuration inside the "${ROOT_FOLDER}/configs/integration-tests.properties" file).
 */
class IntegrationTests {

    companion object {

        lateinit var pa: PowerAuthSDK
        lateinit var ops: IOperationsService
        val pin = "1234"

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
            val auth = PowerAuthAuthentication()
            auth.usePassword = pin
            auth.usePossession = true
            val future = CompletableFuture<Any>()
            pa.removeActivationWithAuthentication(IntegrationUtils.context, auth, object : IActivationRemoveListener {
                override fun onActivationRemoveSucceed() {
                    future.complete(null)
                }
                override fun onActivationRemoveFailed(t: Throwable?) {
                    future.completeExceptionally(t)
                }
            })
            future.get(20, TimeUnit.SECONDS)
        }

    }

    @Test
    fun testEmptyList() {
        val future = CompletableFuture<List<UserOperation>>()
        ops.getOperations(object : IGetOperationListener {
            override fun onSuccess(operations: List<UserOperation>) {
                future.complete(operations)
            }
            override fun onError(error: ApiError) {
                future.completeExceptionally(error.e)
            }
        })
        val oplist = future.get(20, TimeUnit.SECONDS)
        Assert.assertTrue("Test Empty Failed, ${oplist.count()}", oplist.isEmpty())
    }

    @Test
    fun testApproveLogin() {
        IntegrationUtils.createOperation(true)
        val future = CompletableFuture<List<UserOperation>>()
        ops.getOperations(object : IGetOperationListener {
            override fun onSuccess(operations: List<UserOperation>) {
                future.complete(operations)
            }
            override fun onError(error: ApiError) {
                future.completeExceptionally(error.e)
            }
        })
        val operations = future.get(20, TimeUnit.SECONDS)
        Assert.assertTrue("Missing operation", operations.count() == 1)
        val auth = PowerAuthAuthentication()
        auth.usePossession = true
        val opFuture = CompletableFuture<Any?>()
        ops.authorizeOperation(operations.first(), auth, object : IAcceptOperationListener {
            override fun onSuccess() {
                opFuture.complete(null)
            }
            override fun onError(error: ApiError) {
                opFuture.completeExceptionally(error.e)
            }
        })
        Assert.assertNull(opFuture.get(20, TimeUnit.SECONDS))
    }

    @Test
    fun testRejectLogin() {
        IntegrationUtils.createOperation(true)
        val future = CompletableFuture<List<UserOperation>>()
        ops.getOperations(object : IGetOperationListener {
            override fun onSuccess(operations: List<UserOperation>) {
                future.complete(operations)
            }
            override fun onError(error: ApiError) {
                future.completeExceptionally(error.e)
            }
        })
        val operations = future.get(20, TimeUnit.SECONDS)
        Assert.assertTrue("Missing operation", operations.count() == 1)
        val opFuture = CompletableFuture<Any?>()
        ops.rejectOperation(operations.first(), RejectionReason.UNEXPECTED_OPERATION, object : IRejectOperationListener {
            override fun onSuccess() {
                opFuture.complete(null)
            }
            override fun onError(error: ApiError) {
                opFuture.completeExceptionally(error.e)
            }
        })
        Assert.assertNull(opFuture.get(20, TimeUnit.SECONDS))
    }

    @Test
    fun testApprovePayment() {
        IntegrationUtils.createOperation(false)
        val future = CompletableFuture<List<UserOperation>>()
        ops.getOperations(object : IGetOperationListener {
            override fun onSuccess(operations: List<UserOperation>) {
                future.complete(operations)
            }
            override fun onError(error: ApiError) {
                future.completeExceptionally(error.e)
            }
        })
        val operations = future.get(20, TimeUnit.SECONDS)
        Assert.assertTrue("Missing operation", operations.count() == 1)

        val auth = PowerAuthAuthentication()
        auth.usePossession = true
        auth.usePassword = "xxxx" // wrong  password on purpose
        val opFuture = CompletableFuture<Any?>()
        ops.authorizeOperation(operations.first(), auth, object : IAcceptOperationListener {
            override fun onSuccess() {
                opFuture.completeExceptionally(Exception("Operation should not be authorized"))
            }
            override fun onError(error: ApiError) {
                opFuture.complete(null)
            }
        })
        Assert.assertNull(opFuture.get(20, TimeUnit.SECONDS))

        auth.usePassword = pin
        val opFuture2 = CompletableFuture<Any?>()
        ops.authorizeOperation(operations.first(), auth, object : IAcceptOperationListener {
            override fun onSuccess() {
                opFuture2.complete(null)
            }
            override fun onError(error: ApiError) {
                opFuture2.completeExceptionally(error.e)
            }
        })
        Assert.assertNull(opFuture2.get(20, TimeUnit.SECONDS))
    }

    @Test
    fun testRejectPayment() {
        IntegrationUtils.createOperation(false)
        val future = CompletableFuture<List<UserOperation>>()
        ops.getOperations(object : IGetOperationListener {
            override fun onSuccess(operations: List<UserOperation>) {
                future.complete(operations)
            }
            override fun onError(error: ApiError) {
                future.completeExceptionally(error.e)
            }
        })
        val operations = future.get(20, TimeUnit.SECONDS)
        Assert.assertTrue("Missing operation", operations.count() == 1)
        val opFuture = CompletableFuture<Any?>()
        ops.rejectOperation(operations.first(), RejectionReason.UNEXPECTED_OPERATION, object : IRejectOperationListener {
            override fun onSuccess() {
                opFuture.complete(null)
            }
            override fun onError(error: ApiError) {
                opFuture.completeExceptionally(error.e)
            }
        })
        Assert.assertNull(opFuture.get(20, TimeUnit.SECONDS))
    }

    @Test
    fun testOperationPolling() {
        Assert.assertFalse(ops.isPollingOperations())
        var loadingCount  = 0
        val future = CompletableFuture<Any?>()
        ops.listener = object : IOperationsServiceListener {
            override fun operationsLoading(loading: Boolean) {
                if (loading) {
                    loadingCount += 1
                    if (loadingCount == 4) {
                        ops.stopPollingOperations()
                        future.complete(null)
                    }
                }
            }
            override fun operationsLoaded(operations: List<UserOperation>) {

            }
            override fun operationsFailed(error: ApiError) {

            }
        }
        ops.startPollingOperations(1_000)
        Assert.assertNull(future.get(20, TimeUnit.SECONDS))
        ops.stopPollingOperations()
        Assert.assertFalse(ops.isPollingOperations())
    }
}