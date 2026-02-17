package com.gymdash.companion.domain.model

sealed class SyncResult {
    data class Success(val accepted: Int, val rejected: Int) : SyncResult()
    data class Error(val message: String) : SyncResult()
}
