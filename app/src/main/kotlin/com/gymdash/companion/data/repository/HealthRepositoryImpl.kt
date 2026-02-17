package com.gymdash.companion.data.repository

import android.os.Build
import com.gymdash.companion.data.healthconnect.HealthConnectDataSource
import com.gymdash.companion.data.local.datastore.SyncPreferences
import com.gymdash.companion.data.local.db.dao.SyncLogDao
import com.gymdash.companion.data.local.db.dao.SyncLogEntity
import com.gymdash.companion.data.mapper.HealthDataMapper
import com.gymdash.companion.data.remote.api.GymDashApi
import com.gymdash.companion.data.remote.dto.HealthSyncRequest
import com.gymdash.companion.domain.model.SyncResult
import com.gymdash.companion.domain.repository.HealthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.Instant
import javax.inject.Inject

class HealthRepositoryImpl @Inject constructor(
    private val healthConnectDataSource: HealthConnectDataSource,
    private val gymDashApi: GymDashApi,
    private val mapper: HealthDataMapper,
    private val syncLogDao: SyncLogDao,
    private val preferences: SyncPreferences
) : HealthRepository {

    override suspend fun syncHealthData(): SyncResult {
        val token = preferences.authToken.first()
            ?: return SyncResult.Error("Not authenticated")

        return try {
            val changesToken = preferences.lastChangesToken.first()
            val records = if (changesToken != null) {
                val result = healthConnectDataSource.getChanges(changesToken)
                preferences.setLastChangesToken(result.nextToken)
                result.records
            } else {
                val newToken = healthConnectDataSource.getChangesToken()
                preferences.setLastChangesToken(newToken)
                // First sync: read last 7 days
                val now = Instant.now()
                val sevenDaysAgo = now.minusSeconds(7 * 24 * 60 * 60)
                HealthConnectDataSource.RECORD_TYPES.flatMap { recordType ->
                    healthConnectDataSource.readRecords(recordType, sevenDaysAgo, now)
                }
            }

            val metrics = mapper.mapRecords(records)
            if (metrics.isEmpty()) {
                return SyncResult.Success(accepted = 0, rejected = 0)
            }

            val request = HealthSyncRequest(
                deviceId = Build.MODEL,
                syncTimestamp = Instant.now().toString(),
                metrics = metrics
            )

            val response = gymDashApi.syncHealthData(token, request)
            val now = System.currentTimeMillis()

            syncLogDao.insert(
                SyncLogEntity(
                    syncId = response.syncId,
                    timestamp = now,
                    recordsAccepted = response.recordsAccepted,
                    recordsRejected = response.recordsRejected,
                    status = "success"
                )
            )
            preferences.setLastSyncTimestamp(now)

            SyncResult.Success(
                accepted = response.recordsAccepted,
                rejected = response.recordsRejected
            )
        } catch (e: Exception) {
            syncLogDao.insert(
                SyncLogEntity(
                    syncId = "",
                    timestamp = System.currentTimeMillis(),
                    recordsAccepted = 0,
                    recordsRejected = 0,
                    status = "error",
                    errorMessage = e.message
                )
            )
            SyncResult.Error(e.message ?: "Unknown error")
        }
    }

    override fun getSyncHistory(): Flow<List<SyncLogEntity>> = syncLogDao.getAll()
}
