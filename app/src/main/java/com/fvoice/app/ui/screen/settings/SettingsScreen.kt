package com.fvoice.app.ui.screen.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fvoice.app.R
import com.fvoice.app.data.model.UiMode
import com.fvoice.app.ui.component.FVoiceMiuixPage
import com.fvoice.app.ui.component.material.SendLogBottomSheet
import com.fvoice.app.ui.component.miuix.SendLogDialog
import com.fvoice.app.ui.theme.LocalUiMode
import com.fvoice.app.viewmodel.SettingsUiState
import com.fvoice.app.viewmodel.SettingsViewModel
import top.yukonga.miuix.kmp.basic.Card as MiuixCard
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToThemeSettings: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToModelManager: (String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentUiMode = LocalUiMode.current
    val context = LocalContext.current
    val showSendLog = remember { mutableStateOf(false) }

    LaunchedEffect(uiState.backupMessage) {
        uiState.backupMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearBackupMessage()
        }
    }

    when (currentUiMode) {
        UiMode.Miuix -> {
            SettingsMiuix(
                uiState = uiState,
                onNavigateToThemeSettings = onNavigateToThemeSettings,
                onNavigateToAbout = onNavigateToAbout,
                onNavigateToModelManager = onNavigateToModelManager,
                onSetUiMode = { viewModel.setUiMode(it) },
                onSetCheckUpdate = { viewModel.setCheckUpdate(it) },
                onSetLanguage = {
                    viewModel.setLanguage(it)
                    (context as? androidx.activity.ComponentActivity)?.recreate()
                },
                onShowSendLog = { showSendLog.value = true }
            )
            SendLogDialog(
                show = showSendLog.value,
                onDismissRequest = { showSendLog.value = false }
            )
        }
        UiMode.Material -> {
            SettingsMaterial(
                uiState = uiState,
                onNavigateToThemeSettings = onNavigateToThemeSettings,
                onNavigateToAbout = onNavigateToAbout,
                onNavigateToModelManager = onNavigateToModelManager,
                onSetUiMode = { viewModel.setUiMode(it) },
                onSetCheckUpdate = { viewModel.setCheckUpdate(it) },
                onSetLanguage = {
                    viewModel.setLanguage(it)
                    (context as? androidx.activity.ComponentActivity)?.recreate()
                },
                onShowSendLog = { showSendLog.value = true }
            )
            if (showSendLog.value) {
                SendLogBottomSheet(
                    onDismiss = { showSendLog.value = false }
                )
            }
        }
    }
}

// ==================== Miuix Implementation ====================

@Composable
fun SettingsMiuix(
    uiState: SettingsUiState,
    onNavigateToThemeSettings: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToModelManager: (String) -> Unit,
    onSetUiMode: (UiMode) -> Unit,
    onSetCheckUpdate: (Boolean) -> Unit,
    onSetLanguage: (String) -> Unit,
    onShowSendLog: () -> Unit,
) {
    FVoiceMiuixPage(title = stringResource(R.string.nav_settings)) {
        item {
            MiuixCard(modifier = Modifier.padding(top = 12.dp)) {
                Column {
                    SwitchPreference(
                        title = stringResource(R.string.settings_check_update),
                        summary = stringResource(R.string.settings_check_update_auto),
                        startAction = {
                            MiuixIcon(
                                Icons.Rounded.Update,
                                modifier = Modifier.padding(end = 6.dp),
                                contentDescription = stringResource(R.string.settings_check_update),
                                tint = MiuixTheme.colorScheme.onBackground
                            )
                        },
                        checked = uiState.checkUpdate,
                        onCheckedChange = onSetCheckUpdate
                    )
                    val languageItems = listOf(
                        SettingOption("system", stringResource(R.string.language_follow_system)),
                        SettingOption("zh", stringResource(R.string.language_chinese)),
                        SettingOption("en", stringResource(R.string.language_english))
                    )
                    MiuixDropdownPreference(
                        title = stringResource(R.string.settings_language),
                        selected = uiState.language,
                        options = languageItems,
                        startAction = {
                            MiuixIcon(
                                Icons.Rounded.Translate,
                                modifier = Modifier.padding(end = 6.dp),
                                contentDescription = stringResource(R.string.settings_language),
                                tint = MiuixTheme.colorScheme.onBackground
                            )
                        },
                        onSelected = onSetLanguage
                    )
                    MiuixUiModePreference(
                        title = stringResource(R.string.settings_ui_mode),
                        summary = stringResource(R.string.settings_ui_mode_summary),
                        uiMode = uiState.uiMode,
                        onSelectedIndexChange = { index ->
                            onSetUiMode(if (index == 0) UiMode.Miuix else UiMode.Material)
                        }
                    )
                    ArrowPreference(
                        title = stringResource(R.string.settings_theme),
                        summary = stringResource(R.string.theme_follow_system),
                        startAction = {
                            MiuixIcon(
                                Icons.Rounded.Palette,
                                modifier = Modifier.padding(end = 6.dp),
                                contentDescription = stringResource(R.string.settings_theme),
                                tint = MiuixTheme.colorScheme.onBackground
                            )
                        },
                        onClick = onNavigateToThemeSettings
                    )
                }
            }
        }

        item {
            MiuixCard(modifier = Modifier.padding(top = 12.dp)) {
                Column {
                    ArrowPreference(
                        title = stringResource(R.string.settings_denoise_model),
                        summary = stringResource(R.string.model_standard),
                        startAction = {
                            MiuixIcon(
                                Icons.Rounded.Mic,
                                modifier = Modifier.padding(end = 6.dp),
                                contentDescription = stringResource(R.string.settings_denoise_model),
                                tint = MiuixTheme.colorScheme.onBackground
                            )
                        },
                        onClick = { onNavigateToModelManager("denoise") }
                    )
                    ArrowPreference(
                        title = stringResource(R.string.settings_transcribe_model),
                        summary = stringResource(R.string.language_chinese_english),
                        startAction = {
                            MiuixIcon(
                                Icons.Rounded.RecordVoiceOver,
                                modifier = Modifier.padding(end = 6.dp),
                                contentDescription = stringResource(R.string.settings_transcribe_model),
                                tint = MiuixTheme.colorScheme.onBackground
                            )
                        },
                        onClick = { onNavigateToModelManager("transcribe") }
                    )
                }
            }
        }

        item {
            MiuixCard(modifier = Modifier.padding(top = 12.dp)) {
                Column {
                    ArrowPreference(
                        title = stringResource(R.string.settings_send_logs),
                        startAction = {
                            MiuixIcon(
                                Icons.Rounded.BugReport,
                                modifier = Modifier.padding(end = 6.dp),
                                contentDescription = stringResource(R.string.settings_send_logs),
                                tint = MiuixTheme.colorScheme.onBackground
                            )
                        },
                        onClick = onShowSendLog
                    )
                    ArrowPreference(
                        title = stringResource(R.string.settings_about),
                        summary = stringResource(R.string.settings_version_license),
                        startAction = {
                            MiuixIcon(
                                Icons.Rounded.Info,
                                modifier = Modifier.padding(end = 6.dp),
                                contentDescription = stringResource(R.string.settings_about),
                                tint = MiuixTheme.colorScheme.onBackground
                            )
                        },
                        onClick = onNavigateToAbout
                    )
                }
            }
        }
    }
}

@Composable
private fun MiuixUiModePreference(
    title: String,
    summary: String,
    uiMode: UiMode,
    onSelectedIndexChange: (Int) -> Unit,
) {
    val selectedIndex = if (uiMode == UiMode.Material) 1 else 0

    OverlayDropdownPreference(
        title = title,
        summary = summary,
        items = UiMode.entries.map { it.name },
        startAction = {
            MiuixIcon(
                Icons.Rounded.Dashboard,
                modifier = Modifier.padding(end = 6.dp),
                contentDescription = title,
                tint = MiuixTheme.colorScheme.onBackground
            )
        },
        selectedIndex = selectedIndex,
        onSelectedIndexChange = onSelectedIndexChange
    )
}

// ==================== Material Implementation ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsMaterial(
    uiState: SettingsUiState,
    onNavigateToThemeSettings: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToModelManager: (String) -> Unit,
    onSetUiMode: (UiMode) -> Unit,
    onSetCheckUpdate: (Boolean) -> Unit,
    onSetLanguage: (String) -> Unit,
    onShowSendLog: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_settings)) }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.padding(top = 8.dp))

            // App Section
            Text(
                text = stringResource(R.string.settings_section_app),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Card {
                Column {
                    MaterialSwitchItem(
                        title = stringResource(R.string.settings_check_update),
                        subtitle = stringResource(R.string.settings_check_update_auto),
                        checked = uiState.checkUpdate,
                        onCheckedChange = onSetCheckUpdate
                    )
                    HorizontalDivider()
                    MaterialDropdownPreference(
                        title = stringResource(R.string.settings_language),
                        selected = uiState.language,
                        options = listOf(
                            SettingOption("system", stringResource(R.string.language_follow_system)),
                            SettingOption("zh", stringResource(R.string.language_chinese)),
                            SettingOption("en", stringResource(R.string.language_english))
                        ),
                        onSelected = onSetLanguage
                    )
                    HorizontalDivider()
                    MaterialDropdownPreference(
                        title = stringResource(R.string.settings_ui_mode),
                        selected = uiState.uiMode,
                        options = UiMode.entries.map { SettingOption(it, it.name) },
                        onSelected = onSetUiMode
                    )
                    HorizontalDivider()
                    MaterialArrowItem(
                        title = stringResource(R.string.settings_theme),
                        subtitle = stringResource(R.string.theme_follow_system),
                        onClick = onNavigateToThemeSettings
                    )
                }
            }

            // Model Section
            Text(
                text = stringResource(R.string.settings_section_model),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Card {
                Column {
                    MaterialArrowItem(
                        title = stringResource(R.string.settings_denoise_model),
                        subtitle = stringResource(R.string.model_standard),
                        onClick = { onNavigateToModelManager("denoise") }
                    )
                    HorizontalDivider()
                    MaterialArrowItem(
                        title = stringResource(R.string.settings_transcribe_model),
                        subtitle = stringResource(R.string.language_chinese_english),
                        onClick = { onNavigateToModelManager("transcribe") }
                    )
                }
            }

            // Feedback Section
            Text(
                text = stringResource(R.string.settings_section_feedback),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Card {
                Column {
                    MaterialArrowItem(
                        title = stringResource(R.string.settings_send_logs),
                        onClick = onShowSendLog
                    )
                    HorizontalDivider()
                    MaterialArrowItem(
                        title = stringResource(R.string.settings_about),
                        subtitle = stringResource(R.string.settings_version_license),
                        onClick = onNavigateToAbout
                    )
                }
            }

            Spacer(modifier = Modifier.padding(bottom = 24.dp))
        }
    }
}

@Composable
fun MaterialSwitchItem(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun MaterialArrowItem(
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
    }
}
