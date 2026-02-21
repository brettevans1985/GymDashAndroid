package com.gymdash.companion.data.remote.api

import retrofit2.http.Body
import retrofit2.http.POST

data class LoginRequest(
    val username: String,
    val password: String,
    val rememberMe: Boolean = false
)

data class LoginUserDto(
    val id: Int,
    val username: String,
    val email: String
)

data class LoginResponse(
    val accessToken: String,
    val user: LoginUserDto
)

interface AuthApi {

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse
}
