package com.gymdash.companion.domain.usecase

import com.gymdash.companion.data.local.db.dao.SyncLogEntity
import com.gymdash.companion.domain.repository.HealthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetSyncHistoryUseCase @Inject constructor(
    private val healthRepository: HealthRepository
) {
    operator fun invoke(): Flow<List<SyncLogEntity>> = healthRepository.getSyncHistory()
}
