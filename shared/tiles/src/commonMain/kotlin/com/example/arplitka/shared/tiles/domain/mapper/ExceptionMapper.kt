package com.example.arplitka.shared.tiles.domain.mapper

import com.example.arplitka.shared.core.domain.model.CommonException
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.utils.io.errors.IOException

fun Throwable.toCommonException(): CommonException {
    return when (this) {
        is CommonException -> this
        is IOException -> CommonException.Connection
        is ServerResponseException -> CommonException.Server
        is ClientRequestException -> CommonException.Client
        else -> CommonException.Unknown
    }
}
