package com.example.arplitka.shared.ui.kit.ar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.arplitka.shared.ui.core.model.ExceptionModelUi
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

/**
 * Короткое сообщение поверх AR (аналог toast/snackbar на обеих платформах).
 */
@Composable
fun ArTransientMessage(
    exception: ExceptionModelUi?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    autoHideMillis: Long = 3000L
) {
    ArTransientMessage(
        messageResource = exception?.exceptionStringResource,
        onDismiss = onDismiss,
        modifier = modifier,
        autoHideMillis = autoHideMillis
    )
}

/**
 * Короткое сообщение поверх AR (аналог toast/snackbar на обеих платформах).
 */
@Composable
fun ArTransientMessage(
    messageResource: org.jetbrains.compose.resources.StringResource?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    autoHideMillis: Long = 3000L
) {
    val message = messageResource?.let { stringResource(it) }
    ArTransientMessage(
        message = message,
        onDismiss = onDismiss,
        modifier = modifier,
        autoHideMillis = autoHideMillis
    )
}

/**
 * Короткое сообщение поверх AR (аналог toast/snackbar на обеих платформах).
 */
@Composable
fun ArTransientMessage(
    message: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    autoHideMillis: Long = 3000L
) {
    var visible by remember(message) { mutableStateOf(!message.isNullOrBlank()) }

    LaunchedEffect(message) {
        if (!message.isNullOrBlank()) {
            visible = true
            delay(autoHideMillis)
            visible = false
            onDismiss()
        }
    }

    if (!visible || message.isNullOrBlank()) return

    Text(
        text = message,
        color = Color.White,
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .background(Color(0xFF2D3142).copy(alpha = 0.92f), RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    )
}
