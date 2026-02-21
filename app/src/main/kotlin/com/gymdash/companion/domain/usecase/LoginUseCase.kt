package com.gymdash.companion.domain.usecase

import com.gymdash.companion.domain.repository.AuthRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(username: String, password: String): Result<Unit> =
        authRepository.login(username, password)
}
