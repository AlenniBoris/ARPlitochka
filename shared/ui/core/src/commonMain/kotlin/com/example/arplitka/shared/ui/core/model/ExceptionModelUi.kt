package com.example.arplitka.shared.ui.core.model

import com.example.arplitka.shared.core.domain.model.CommonException
import org.jetbrains.compose.resources.StringResource
import arplitka.shared.ui.core.generated.resources.Res
import arplitka.shared.ui.core.generated.resources.exception_connection
import arplitka.shared.ui.core.generated.resources.exception_server
import arplitka.shared.ui.core.generated.resources.exception_client
import arplitka.shared.ui.core.generated.resources.exception_serialization
import arplitka.shared.ui.core.generated.resources.exception_unknown

data class ExceptionModelUi(
    val exceptionStringResource: StringResource
)

fun CommonException.toUiModel(): ExceptionModelUi = ExceptionModelUi(
    exceptionStringResource = when (this) {
        is CommonException.Connection -> Res.string.exception_connection
        is CommonException.Server -> Res.string.exception_server
        is CommonException.Client -> Res.string.exception_client
        is CommonException.Serialization -> Res.string.exception_serialization
        is CommonException.Unknown -> Res.string.exception_unknown
    }
)
