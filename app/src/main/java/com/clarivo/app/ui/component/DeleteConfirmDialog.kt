package com.clarivo.app.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.clarivo.app.R
import com.clarivo.app.data.model.UiMode
import com.clarivo.app.ui.theme.LocalUiMode
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.TextButton as MiuixTextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog

@Composable
fun DeleteConfirmDialog(
    show: Boolean,
    title: String = stringResource(R.string.clear_history_title),
    message: String = stringResource(R.string.clear_history_message),
    confirmText: String = stringResource(R.string.confirm),
    cancelText: String = stringResource(R.string.cancel),
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (!show) return

    when (LocalUiMode.current) {
        UiMode.Miuix -> {
            WindowDialog(
                show = show,
                modifier = Modifier.windowInsetsPadding(
                    WindowInsets.systemBars.only(WindowInsetsSides.Top)
                ),
                title = title,
                onDismissRequest = onDismiss,
                content = {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        MiuixText(
                            text = message,
                            modifier = Modifier.fillMaxWidth(),
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            textAlign = TextAlign.Start,
                        )
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                        ) {
                            MiuixTextButton(
                                text = cancelText,
                                onClick = onDismiss,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(20.dp))
                            MiuixTextButton(
                                text = confirmText,
                                onClick = {
                                    onConfirm()
                                    onDismiss()
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.textButtonColorsPrimary()
                            )
                        }
                    }
                },
            )
        }

        UiMode.Material -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(title) },
                text = { Text(message) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onConfirm()
                            onDismiss()
                        },
                    ) { Text(confirmText) }
                },
                dismissButton = {
                    TextButton(
                        onClick = onDismiss,
                    ) { Text(cancelText) }
                },
            )
        }
    }
}
