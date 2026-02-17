package com.gymdash.companion.data.local.db.dao

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SyncLogDaoTest {

    @Test
    fun `SyncLogEntity can be constructed with defaults`() {
        val entity = SyncLogEntity(
            syncId = "sync-123",
            timestamp = System.currentTimeMillis(),
            recordsAccepted = 10,
            recordsRejected = 2,
            status = "success"
        )

        assertEquals("sync-123", entity.syncId)
        assertEquals(10, entity.recordsAccepted)
        assertEquals(2, entity.recordsRejected)
        assertEquals("success", entity.status)
        assertNull(entity.errorMessage)
        assertEquals(0L, entity.id)
    }

    @Test
    fun `SyncLogEntity can be constructed with error`() {
        val entity = SyncLogEntity(
            syncId = "",
            timestamp = System.currentTimeMillis(),
            recordsAccepted = 0,
            recordsRejected = 0,
            status = "error",
            errorMessage = "Network timeout"
        )

        assertEquals("error", entity.status)
        assertNotNull(entity.errorMessage)
        assertEquals("Network timeout", entity.errorMessage)
    }
}
