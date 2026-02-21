package com.gymdash.companion.domain.model

enum class SyncErrorType {
    NETWORK,
    AUTH_EXPIRED,
    HEALTH_CONNECT,
    SERVER,
    UNKNOWN
}

sealed class SyncResult {
    data class Success(
        val recordsProcessed: Int,
        val recordsCreated: Int,
        val recordsUpdated: Int
    ) : SyncResult()
    data class Error(val message: String, val type: SyncErrorType = SyncErrorType.UNKNOWN) : SyncResult()
}
