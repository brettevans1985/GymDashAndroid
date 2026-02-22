package com.gymdash.companion.data.remote

import com.gymdash.companion.data.local.datastore.SyncPreferences
import com.gymdash.companion.data.remote.api.AuthApi
import com.gymdash.companion.data.remote.api.RefreshTokenRequest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val preferences: SyncPreferences,
    private val authEventBus: AuthEventBus,
    private val authApiProvider: Provider<AuthApi>
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())

        if (response.code == 401 && !chain.request().url.encodedPath.contains("/auth/")) {
            val refreshToken = runBlocking { preferences.refreshToken.first() }
            if (refreshToken != null) {
                return try {
                    val refreshResponse = runBlocking {
                        authApiProvider.get().refreshToken(RefreshTokenRequest(refreshToken))
                    }
                    runBlocking {
                        preferences.setAuthToken("Bearer ${refreshResponse.accessToken}")
                        preferences.setRefreshToken(refreshResponse.refreshToken)
                    }
                    // Retry the original request with the new token
                    response.close()
                    val retryRequest = chain.request().newBuilder()
                        .header("Authorization", "Bearer ${refreshResponse.accessToken}")
                        .build()
                    chain.proceed(retryRequest)
                } catch (_: Exception) {
                    runBlocking {
                        preferences.setAuthToken(null)
                        preferences.setRefreshToken(null)
                    }
                    authEventBus.emitAuthExpired()
                    response
                }
            } else {
                runBlocking { preferences.setAuthToken(null) }
                authEventBus.emitAuthExpired()
            }
        }

        return response
    }
}
