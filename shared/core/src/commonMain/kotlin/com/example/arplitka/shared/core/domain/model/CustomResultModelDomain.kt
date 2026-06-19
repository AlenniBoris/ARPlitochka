package com.example.arplitka.shared.core.domain.model

sealed class CustomResultModelDomain<out T, out E>(
    open val result: T? = null,
    open val exception: E? = null
) {
    data class Success<out T, out E>(override val result: T) : CustomResultModelDomain<T, E>()
    data class Error<out T, out E>(override val exception: E) : CustomResultModelDomain<T, E>()
}
