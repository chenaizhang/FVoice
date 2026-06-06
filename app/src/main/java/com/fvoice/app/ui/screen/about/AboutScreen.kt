package com.fvoice.app.ui.screen.about

import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import com.fvoice.app.R
import com.fvoice.app.data.model.UiMode
import com.fvoice.app.ui.component.FVoiceMiuixPage
import com.fvoice.app.ui.navigation3.LocalNavigator
import com.fvoice.app.ui.theme.LocalUiMode
import top.yukonga.miuix.kmp.basic.Card as MiuixCard
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

private const val SOURCE_CODE_URL = "https://github.com/chenaizhang/FVoice"
private const val QQ_CHANNEL_URL = "https://pd.qq.com/s/ckdo2vbex?b=9"

data class AboutUiState(
    val title: String,
    val appName: String,
    val versionName: String,
)

data class AboutScreenActions(
    val onBack: () -> Unit,
)

@Composable
fun AboutScreen() {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val versionName = remember(context) {
        runCatching {
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            info.versionName ?: "1.0.0"
        }.getOrDefault("1.0.0")
    }
    val state = AboutUiState(
        title = stringResource(R.string.settings_about),
        appName = stringResource(R.string.app_name),
        versionName = versionName,
    )
    val actions = AboutScreenActions(
        onBack = dropUnlessResumed { navigator.pop() }
    )

    when (LocalUiMode.current) {
        UiMode.Miuix -> AboutScreenMiuix(state, actions)
        UiMode.Material -> AboutScreenMaterial(state, actions)
    }
}

@Composable
private fun AboutScreenMiuix(
    state: AboutUiState,
    actions: AboutScreenActions,
) {
    val uriHandler = LocalUriHandler.current

    FVoiceMiuixPage(
        title = state.title,
        contentPadding = PaddingValues(0.dp),
        navigationIcon = {
            MiuixIconButton(onClick = actions.onBack) {
                MiuixIcon(MiuixIcons.Back, contentDescription = null)
            }
        },
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_launcher_foreground_vector),
                    contentDescription = state.appName,
                    modifier = Modifier.size(80.dp)
                )
                MiuixText(
                    text = state.appName,
                    style = MiuixTheme.textStyles.title2
                )
                MiuixText(
                    text = state.versionName,
                    style = MiuixTheme.textStyles.subtitle,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
                Spacer(Modifier.height(12.dp))
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
        item { SmallTitle(stringResource(R.string.settings_about)) }
        item {
            MiuixCard(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 12.dp)
            ) {
                ArrowPreference(
                    title = stringResource(R.string.about_source_code_title),
                    summary = stringResource(R.string.about_source_code_summary),
                    onClick = { uriHandler.openUri(SOURCE_CODE_URL) }
                )
                ArrowPreference(
                    title = stringResource(R.string.about_qq_channel_title),
                    summary = stringResource(R.string.about_qq_channel_summary),
                    onClick = { uriHandler.openUri(QQ_CHANNEL_URL) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AboutScreenMaterial(
    state: AboutUiState,
    actions: AboutScreenActions,
) {
    val uriHandler = LocalUriHandler.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.title) },
                navigationIcon = {
                    IconButton(onClick = actions.onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp, bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_launcher_foreground_vector),
                    contentDescription = state.appName,
                    modifier = Modifier.size(80.dp)
                )
                Text(state.appName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(state.versionName, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Card(modifier = Modifier.fillMaxWidth()) {
                Column {
                    AboutRowMaterial(
                        title = stringResource(R.string.about_source_code_title),
                        subtitle = stringResource(R.string.about_source_code_summary),
                        onClick = { uriHandler.openUri(SOURCE_CODE_URL) },
                    )
                    HorizontalDivider()
                    AboutRowMaterial(
                        title = stringResource(R.string.about_qq_channel_title),
                        subtitle = stringResource(R.string.about_qq_channel_summary),
                        onClick = { uriHandler.openUri(QQ_CHANNEL_URL) },
                    )
                }
            }
        }
    }
}

@Composable
private fun AboutRowMaterial(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
    }
}
