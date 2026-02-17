package com.gymdash.companion.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymdash.companion.data.local.db.dao.SyncLogEntity
import com.gymdash.companion.domain.usecase.GetSyncHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SyncHistoryViewModel @Inject constructor(
    getSyncHistoryUseCase: GetSyncHistoryUseCase
) : ViewModel() {

    val syncHistory: StateFlow<List<SyncLogEntity>> = getSyncHistoryUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )
}
