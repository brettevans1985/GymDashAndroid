package com.gymdash.companion.data.remote.api

import retrofit2.http.Body
import retrofit2.http.POST

data class LoginRequest(
    val email: String,
    val password: String,
    val rememberMe: Boolean = false
)

data class LoginResponse(
    val token: String,
    val expiresAt: String,
    val userId: Int
)

interface AuthApi {

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse
}
