package com.example.arplitka.shared.core.domain.presentation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch

class SingleFlowEvent<T>(private val scope: CoroutineScope) {
    private val channel = Channel<T>(Channel.UNLIMITED)

    val flow = channel
        .receiveAsFlow()
        .shareIn(scope, SharingStarted.WhileSubscribed(), 0)

    fun emit(value: T) {
        scope.launch {
            channel.send(value)
        }
    }
}
