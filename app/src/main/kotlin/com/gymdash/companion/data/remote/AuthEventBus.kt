package com.gymdash.companion.data.remote

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthEventBus @Inject constructor() {
    private val _authExpired = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val authExpired = _authExpired.asSharedFlow()

    fun emitAuthExpired() {
        _authExpired.tryEmit(Unit)
    }
}
