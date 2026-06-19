package com.example.arplitka.shared.core.domain.model

sealed interface CommonException {
    data object Connection : CommonException
    data object Server : CommonException
    data object Client : CommonException
    data object Serialization : CommonException
    data object Unknown : CommonException
}
