package com.gymdash.companion.domain.model

sealed class SyncResult {
    data class Success(
        val recordsProcessed: Int,
        val recordsCreated: Int,
        val recordsUpdated: Int
    ) : SyncResult()
    data class Error(val message: String) : SyncResult()
}
