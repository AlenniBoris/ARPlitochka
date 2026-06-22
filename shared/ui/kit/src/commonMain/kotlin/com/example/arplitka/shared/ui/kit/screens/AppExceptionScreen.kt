package com.example.arplitka.shared.ui.kit.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.arplitka.shared.ui.core.model.ExceptionModelUi
import org.jetbrains.compose.resources.stringResource
import arplitka.shared.ui.core.generated.resources.Res
import arplitka.shared.ui.core.generated.resources.try_again_string

@Composable
fun AppExceptionScreen(
    modifier: Modifier = Modifier.fillMaxSize(),
    exception: ExceptionModelUi,
    onTryAgain: () -> Unit = {}
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            modifier = Modifier
                .padding(16.dp)
                .clickable { onTryAgain() },
            text = stringResource(exception.exceptionStringResource),
            style = MaterialTheme.typography.bodyLarge.copy(
                color = Color.Black,
                textAlign = TextAlign.Center
            )
        )

        Text(
            modifier = Modifier
                .padding(16.dp)
                .clickable { onTryAgain() },
            text = stringResource(Res.string.try_again_string),
            style = MaterialTheme.typography.bodyMedium.copy(
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        )
    }
}
