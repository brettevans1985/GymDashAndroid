package com.gymdash.companion.data.remote

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BaseUrlInterceptor @Inject constructor() : Interceptor {

    @Volatile
    var baseUrl: String = ""

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (baseUrl.isBlank()) return chain.proceed(request)

        val newUrl = baseUrl.trimEnd('/').toHttpUrl()
        val updatedUrl = request.url.newBuilder()
            .scheme(newUrl.scheme)
            .host(newUrl.host)
            .port(newUrl.port)
            .build()

        return chain.proceed(request.newBuilder().url(updatedUrl).build())
    }
}
