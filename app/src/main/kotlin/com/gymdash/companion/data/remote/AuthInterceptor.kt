package com.gymdash.companion.data.remote

import com.gymdash.companion.data.local.datastore.SyncPreferences
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val preferences: SyncPreferences,
    private val authEventBus: AuthEventBus
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())

        if (response.code == 401) {
            runBlocking {
                preferences.setAuthToken(null)
            }
            authEventBus.emitAuthExpired()
        }

        return response
    }
}
