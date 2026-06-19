package com.example.arplitka.shared.app.mapper

import com.example.arplitka.shared.core.domain.model.CommonException

fun CommonException.toUiString(): String = when (this) {
    is CommonException.Connection -> "Нет интернет-соединения или превышено время ожидания"
    is CommonException.Server -> "Ошибка сервера. Попробуйте позже"
    is CommonException.Client -> "Ошибка запроса. Проверьте данные"
    is CommonException.Serialization -> "Ошибка обработки данных"
    is CommonException.Unknown -> "Произошла неизвестная ошибка"
}
