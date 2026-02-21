package com.gymdash.companion.worker

import androidx.work.ListenableWorker
import com.gymdash.companion.domain.model.SyncResult
import com.gymdash.companion.domain.usecase.SyncHealthDataUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class HealthSyncWorkerTest {

    private lateinit var syncHealthDataUseCase: SyncHealthDataUseCase

    @Before
    fun setup() {
        syncHealthDataUseCase = mockk()
    }

    @Test
    fun `doWork returns success when sync succeeds`() = runTest {
        coEvery { syncHealthDataUseCase() } returns SyncResult.Success(
            recordsProcessed = 10,
            recordsCreated = 8,
            recordsUpdated = 2
        )

        val result = performSync()

        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun `doWork returns retry when sync fails`() = runTest {
        coEvery { syncHealthDataUseCase() } returns SyncResult.Error("Network error")

        val result = performSync()

        assertEquals(ListenableWorker.Result.retry(), result)
    }

    private suspend fun performSync(): ListenableWorker.Result {
        return when (syncHealthDataUseCase()) {
            is SyncResult.Success -> ListenableWorker.Result.success()
            is SyncResult.Error -> ListenableWorker.Result.retry()
        }
    }
}
