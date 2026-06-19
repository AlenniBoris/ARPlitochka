package com.example.arplitka.network.core

sealed interface ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>
    data class Error(val error: NetworkError) : ApiResult<Nothing>
}

suspend fun <T> safeApiCall(call: suspend () -> T): ApiResult<T> {
    return try {
        ApiResult.Success(call())
    } catch (e: Throwable) {
        ApiResult.Error(
            NetworkError(
                message = e.message ?: "Unknown error",
                cause = e
            )
        )
    }
}
