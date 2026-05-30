package com.example.arplitka.network.core

data class NetworkError(
    val code: Int? = null,
    val message: String,
    val cause: Throwable? = null
)
