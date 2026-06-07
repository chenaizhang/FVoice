package com.clarivo.app.ui.component.miuix

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Share
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.clarivo.app.BuildConfig
import com.clarivo.app.R
import com.clarivo.app.ui.component.LoadingDialogHandle
import com.clarivo.app.ui.component.rememberLoadingDialog
import com.clarivo.app.util.LogCollector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun SendLogDialog(
    show: Boolean,
    onDismissRequest: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val logSavedText = stringResource(R.string.log_saved)
    val sendLogText = stringResource(R.string.send_log)
    val loadingDialog = rememberLoadingDialog()

    val exportBugreportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            loadingDialog.show()
            context.contentResolver.openOutputStream(uri)?.use { output ->
                LogCollector.collect(context).inputStream().use {
                    it.copyTo(output)
                }
            }
            loadingDialog.hide()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, logSavedText, Toast.LENGTH_SHORT).show()
            }
        }
    }

    OverlayDialog(
        show = show,
        onDismissRequest = onDismissRequest,
        insideMargin = DpSize(0.dp, 0.dp),
        content = {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 12.dp),
                text = stringResource(R.string.send_log),
                fontSize = MiuixTheme.textStyles.title4.fontSize,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                color = MiuixTheme.colorScheme.onSurface
            )
            ArrowPreference(
                title = stringResource(id = R.string.save_log),
                startAction = {
                    Icon(
                        Icons.Rounded.Save,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 16.dp),
                        tint = MiuixTheme.colorScheme.onSurface
                    )
                },
                onClick = {
                    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH_mm")
                    val current = LocalDateTime.now().format(formatter)
                    exportBugreportLauncher.launch("Clarivo_bugreport_${current}.zip")
                    onDismissRequest()
                },
                insideMargin = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
            )
            ArrowPreference(
                title = stringResource(id = R.string.send_log),
                startAction = {
                    Icon(
                        Icons.Rounded.Share,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 16.dp),
                        tint = MiuixTheme.colorScheme.onSurface
                    )
                },
                onClick = {
                    scope.launch {
                        onDismissRequest()
                        val bugreport = loadingDialog.withLoading {
                            withContext(Dispatchers.IO) {
                                LogCollector.collect(context)
                            }
                        }

                        val uri: Uri =
                            FileProvider.getUriForFile(
                                context,
                                "${BuildConfig.APPLICATION_ID}.fileprovider",
                                bugreport
                            )

                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            putExtra(Intent.EXTRA_STREAM, uri)
                            type = "application/zip"
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }

                        context.startActivity(
                            Intent.createChooser(shareIntent, sendLogText)
                        )
                    }
                },
                insideMargin = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
            )
            TextButton(
                text = stringResource(id = android.R.string.cancel),
                onClick = {
                    onDismissRequest()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 24.dp)
                    .padding(horizontal = 24.dp)
            )
        }
    )
}
