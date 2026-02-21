package com.gymdash.companion.data.local.db.dao

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SyncLogDaoTest {

    @Test
    fun `SyncLogEntity can be constructed with defaults`() {
        val entity = SyncLogEntity(
            timestamp = System.currentTimeMillis(),
            recordsProcessed = 10,
            recordsCreated = 8,
            recordsUpdated = 2,
            status = "success"
        )

        assertEquals(10, entity.recordsProcessed)
        assertEquals(8, entity.recordsCreated)
        assertEquals(2, entity.recordsUpdated)
        assertEquals("success", entity.status)
        assertNull(entity.errorMessage)
        assertEquals(0L, entity.id)
    }

    @Test
    fun `SyncLogEntity can be constructed with error`() {
        val entity = SyncLogEntity(
            timestamp = System.currentTimeMillis(),
            recordsProcessed = 0,
            recordsCreated = 0,
            recordsUpdated = 0,
            status = "error",
            errorMessage = "Network timeout"
        )

        assertEquals("error", entity.status)
        assertNotNull(entity.errorMessage)
        assertEquals("Network timeout", entity.errorMessage)
    }
}
