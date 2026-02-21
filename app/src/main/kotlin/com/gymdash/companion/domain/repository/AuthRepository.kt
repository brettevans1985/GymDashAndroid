package com.gymdash.companion.domain.repository

interface AuthRepository {
    suspend fun login(username: String, password: String): Result<Unit>
    suspend fun logout()
    suspend fun isLoggedIn(): Boolean
}
