package com.fvoice.app.core.task

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

class ProcessCancellationToken {

    private val _isCancelled = AtomicBoolean(false)
    private val _cancelled = MutableStateFlow(false)
    val cancelled: StateFlow<Boolean> = _cancelled.asStateFlow()

    fun cancel() {
        if (_isCancelled.compareAndSet(false, true)) {
            _cancelled.value = true
        }
    }

    fun isCancelled(): Boolean = _isCancelled.get()

    fun throwIfCancelled() {
        if (_isCancelled.get()) {
            throw com.fvoice.app.core.exception.FVoiceException.CancelledByUser()
        }
    }
}
