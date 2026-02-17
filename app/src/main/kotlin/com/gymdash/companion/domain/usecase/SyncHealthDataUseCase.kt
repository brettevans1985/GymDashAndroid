package com.gymdash.companion.domain.usecase

import com.gymdash.companion.domain.model.SyncResult
import com.gymdash.companion.domain.repository.HealthRepository
import javax.inject.Inject

class SyncHealthDataUseCase @Inject constructor(
    private val healthRepository: HealthRepository
) {
    suspend operator fun invoke(): SyncResult = healthRepository.syncHealthData()
}
