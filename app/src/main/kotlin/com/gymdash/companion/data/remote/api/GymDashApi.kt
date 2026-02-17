package com.gymdash.companion.data.remote.api

import com.gymdash.companion.data.remote.dto.HealthSyncRequest
import com.gymdash.companion.data.remote.dto.HealthSyncResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface GymDashApi {

    @POST("health/sync")
    suspend fun syncHealthData(
        @Header("Authorization") token: String,
        @Body request: HealthSyncRequest
    ): HealthSyncResponse
}
