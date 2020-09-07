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

package com.wultra.android.mtokensdk.api

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.reflect.TypeToken
import com.wultra.android.mtokensdk.api.general.ErrorResponse
import com.wultra.android.mtokensdk.api.operation.AttributeTypeAdapter
import com.wultra.android.mtokensdk.api.operation.ZonedDateTimeDeserializer
import com.wultra.android.mtokensdk.api.operation.model.Attribute
import com.wultra.android.mtokensdk.common.Logger
import okhttp3.*
import org.threeten.bp.ZonedDateTime
import java.io.IOException
import java.lang.Exception

interface IApiCallResponseListener<T> {
    fun onSuccess(result: T)
    fun onFailure(e: Throwable)
}

/**
 * Common API methods for making request via OkHttp and (de)serializing data via Gson.
 */
internal abstract class Api(protected val okHttpClient: OkHttpClient, private val baseUrl: String) {

    var acceptLanguage = "en"

    protected val JSON_MEDIA_TYPE: MediaType = MediaType.parse("application/json; charset=UTF-8")!!

    protected fun constructApiUrl(path: String): String {
        val base = baseUrl.removeSuffix("/")
        return "$base/$path"
    }

    /**
     * Make an API call.
     */
    protected inline fun <reified T> makeCall(request: Request, listener: IApiCallResponseListener<T>) {
        if (!request.url().isHttps) {
            Logger.w("Using HTTP for communication may create a serious security issue! Use HTTPS in production.")
        }
        val call = okHttpClient.newCall(request)
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                listener.onFailure(e)
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val gson = getGson()
                    val typeAdapter = getTypeAdapter<T>(gson)
                    val converter = GsonResponseBodyConverter(gson, typeAdapter)
                    listener.onSuccess(converter.convert(response.body()!!))
                } else {
                    val gson = getGson()
                    val typeAdapter = getTypeAdapter<ErrorResponse>(gson)
                    val converter = GsonResponseBodyConverter(gson, typeAdapter)
                    try {
                        val errorResponse = converter.convert(response.body()!!)
                        listener.onFailure(MTokenHttpException(response, errorResponse))
                    } catch (e: Exception) {
                        // there's no error response
                        listener.onFailure(MTokenHttpException(response))
                    }
                }
            }
        })
    }

    protected inline fun <reified T> getTypeAdapter(gson: Gson): TypeAdapter<T> {
        return gson.getAdapter(TypeToken.get(T::class.java))
    }

    /**
     * This func is inlined only because of ProGuard.
     * For some reason when a method is called only from an inlined method
     * ProGuard gets confused and it can't correctly trace method usage.
     * The obfuscated code then breaks at this method code.
     */
    @Suppress("NOTHING_TO_INLINE")
    protected inline fun getGson(): Gson {
        val builder = GsonBuilder()
        builder.registerTypeHierarchyAdapter(Attribute::class.java, AttributeTypeAdapter())
        builder.registerTypeAdapter(ZonedDateTime::class.java, ZonedDateTimeDeserializer())
        return builder.create()
    }
}