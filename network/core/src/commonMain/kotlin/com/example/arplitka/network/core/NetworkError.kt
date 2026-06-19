package com.example.arplitka.network.core

sealed class NetworkError : Throwable() {
    abstract val originalMessage: String?
    
    data class Connection(
        override val originalMessage: String?,
        val isTimeout: Boolean = false
    ) : NetworkError()

    data class Server(
        val code: Int,
        override val originalMessage: String?
    ) : NetworkError()

    data class Client(
        val code: Int,
        override val originalMessage: String?
    ) : NetworkError()

    data class Serialization(
        override val originalMessage: String?,
        override val cause: Throwable?
    ) : NetworkError()

    data class Unknown(
        override val originalMessage: String?,
        override val cause: Throwable?
    ) : NetworkError()

    fun toUiString(): String = when (this) {
        is Connection -> if (isTimeout) "Превышено время ожидания" else "Нет интернет-соединения"
        is Server -> "Ошибка сервера ($code)"
        is Client -> "Ошибка запроса ($code)"
        is Serialization -> "Ошибка обработки данных"
        is Unknown -> originalMessage ?: "Произошла неизвестная ошибка"
    }
}
