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
    val message: String,
    val exceptionStringResource: StringResource
)

fun CommonException.toUiModel(): ExceptionModelUi {
    val message = when (this) {
        is CommonException.Connection -> "No internet connection or timeout"
        is CommonException.Server -> "Server error. Try again later"
        is CommonException.Client -> "Request error. Check your data"
        is CommonException.Serialization -> "Data processing error"
        is CommonException.Unknown -> "An unknown error occurred"
    }
    
    val resource = when (this) {
        is CommonException.Connection -> Res.string.exception_connection
        is CommonException.Server -> Res.string.exception_server
        is CommonException.Client -> Res.string.exception_client
        is CommonException.Serialization -> Res.string.exception_serialization
        is CommonException.Unknown -> Res.string.exception_unknown
    }

    return ExceptionModelUi(
        message = message,
        exceptionStringResource = resource
    )
}
