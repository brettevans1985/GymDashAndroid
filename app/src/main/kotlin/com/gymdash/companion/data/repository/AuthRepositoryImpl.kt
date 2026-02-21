package com.gymdash.companion.data.repository

import com.gymdash.companion.data.local.datastore.SyncPreferences
import com.gymdash.companion.data.remote.api.AuthApi
import com.gymdash.companion.data.remote.api.LoginRequest
import com.gymdash.companion.domain.repository.AuthRepository
import retrofit2.HttpException
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val preferences: SyncPreferences
) : AuthRepository {

    override suspend fun login(username: String, password: String): Result<Unit> = try {
        val response = authApi.login(LoginRequest(username, password))
        preferences.setAuthToken("Bearer ${response.accessToken}")
        preferences.setUserId(response.user.id.toString())
        Result.success(Unit)
    } catch (e: HttpException) {
        val errorBody = e.response()?.errorBody()?.string() ?: e.message()
        Result.failure(Exception(errorBody))
    } catch (e: Exception) {
        Result.failure(e)
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
