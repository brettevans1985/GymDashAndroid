package com.gymdash.companion.data.repository

import com.gymdash.companion.data.local.datastore.SyncPreferences
import com.gymdash.companion.data.remote.api.AuthApi
import com.gymdash.companion.data.remote.api.LoginRequest
import com.gymdash.companion.domain.repository.AuthRepository
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val preferences: SyncPreferences
) : AuthRepository {

    override suspend fun login(email: String, password: String): Result<Unit> = runCatching {
        val response = authApi.login(LoginRequest(email, password))
        preferences.setAuthToken("Bearer ${response.token}")
        preferences.setUserId(response.userId.toString())
    }

    override suspend fun logout() {
        preferences.clearAll()
    }

    override suspend fun isLoggedIn(): Boolean {
        var token: String? = null
        preferences.authToken.collect { token = it; return@collect }
        return token != null
    }
}
