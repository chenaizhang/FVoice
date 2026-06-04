package com.fvoice.app.ui.screen.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.WebAsset
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.fvoice.app.R
import com.fvoice.app.data.model.UiMode
import com.fvoice.app.permission.PermissionManager
import com.fvoice.app.permission.PermissionState
import com.fvoice.app.ui.component.FVoiceMiuixCard
import com.fvoice.app.ui.component.FVoiceMiuixInfoRow
import com.fvoice.app.ui.theme.LocalUiMode
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.TextButton as MiuixTextButton
import top.yukonga.miuix.kmp.basic.TopAppBar as MiuixTopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun PermissionSettingsScreen(
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val permissionManager = remember(context) { PermissionManager(context) }
    val permissionState by permissionManager.state.collectAsState()
    val settingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        permissionManager.refresh()
    }
    val runtimePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        permissionManager.refresh()
    }

    DisposableEffect(lifecycleOwner, permissionManager) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionManager.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val actions = PermissionActions(
        onRequestStorage = {
            permissionManager.legacyStoragePermission()?.let(runtimePermissionLauncher::launch)
                ?: settingsLauncher.launch(permissionManager.storageSettingsIntent())
        },
        onRequestNotification = {
            permissionManager.notificationRuntimePermission()?.let(runtimePermissionLauncher::launch)
                ?: settingsLauncher.launch(permissionManager.notificationSettingsIntent())
        },
        onRequestMicrophone = { runtimePermissionLauncher.launch(permissionManager.microphonePermission()) },
        onRequestBattery = { settingsLauncher.launch(permissionManager.batteryWhitelistIntent()) },
        onRequestOverlay = { settingsLauncher.launch(permissionManager.overlaySettingsIntent()) },
    )

    when (LocalUiMode.current) {
        UiMode.Miuix -> PermissionSettingsMiuix(
            state = permissionState,
            actions = actions,
            onBack = onBack
        )

        UiMode.Material -> PermissionSettingsMaterial(
            state = permissionState,
            actions = actions,
            onBack = onBack
        )
    }
}

private data class PermissionActions(
    val onRequestStorage: () -> Unit,
    val onRequestNotification: () -> Unit,
    val onRequestMicrophone: () -> Unit,
    val onRequestBattery: () -> Unit,
    val onRequestOverlay: () -> Unit,
)

@Composable
private fun PermissionSettingsMiuix(
    state: PermissionState,
    actions: PermissionActions,
    onBack: () -> Unit,
) {
    val scrollBehavior = MiuixScrollBehavior()

    MiuixScaffold(
        topBar = {
            MiuixTopAppBar(
                title = stringResource(R.string.permission_section),
                navigationIcon = {
                    MiuixIconButton(onClick = onBack) {
                        MiuixIcon(MiuixIcons.Back, contentDescription = null)
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        popupHost = { },
        contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout).only(WindowInsetsSides.Horizontal)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxHeight()
                .scrollEndHaptic()
                .overScrollVertical()
                .padding(horizontal = 12.dp),
            contentPadding = innerPadding,
            overscrollEffect = null,
        ) {
            item { Spacer(Modifier.height(8.dp)) }
            item {
                FVoiceMiuixCard {
                    FVoiceMiuixInfoRow(
                        title = if (state.requiredGranted) {
                            stringResource(R.string.permission_status_ready_title)
                        } else {
                            stringResource(R.string.permission_status_missing_title)
                        },
                        summary = if (state.requiredGranted) {
                            stringResource(R.string.permission_ready)
                        } else {
                            stringResource(R.string.permission_missing)
                        },
                        end = {
                            MiuixIcon(
                                imageVector = if (state.requiredGranted) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                tint = if (state.requiredGranted) {
                                    MiuixTheme.colorScheme.primary
                                } else {
                                    MiuixTheme.colorScheme.error
                                },
                                contentDescription = null
                            )
                        }
                    )
                }
            }
            item {
                FVoiceMiuixCard {
                    PermissionRowMiuix(
                        title = stringResource(R.string.permission_storage),
                        granted = state.storage,
                        required = true,
                        icon = Icons.Default.Folder,
                        onClick = actions.onRequestStorage
                    )
                    PermissionRowMiuix(
                        title = stringResource(R.string.permission_notification),
                        granted = state.notification,
                        required = true,
                        icon = Icons.Default.Notifications,
                        onClick = actions.onRequestNotification
                    )
                    PermissionRowMiuix(
                        title = stringResource(R.string.permission_microphone),
                        granted = state.microphone,
                        required = true,
                        icon = Icons.Default.Mic,
                        onClick = actions.onRequestMicrophone
                    )
                    PermissionRowMiuix(
                        title = stringResource(R.string.permission_battery),
                        granted = state.batteryWhitelist,
                        required = true,
                        icon = Icons.Default.BatterySaver,
                        onClick = actions.onRequestBattery
                    )
                    PermissionRowMiuix(
                        title = stringResource(R.string.permission_overlay),
                        granted = state.overlay,
                        required = false,
                        icon = Icons.Default.WebAsset,
                        onClick = actions.onRequestOverlay
                    )
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun PermissionRowMiuix(
    title: String,
    granted: Boolean,
    required: Boolean,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    FVoiceMiuixInfoRow(
        title = title,
        summary = when {
            granted -> stringResource(R.string.permission_granted)
            required -> stringResource(R.string.permission_required)
            else -> stringResource(R.string.permission_optional)
        },
        icon = icon,
        end = {
            if (granted) {
                MiuixText(
                    text = stringResource(R.string.permission_granted),
                    color = MiuixTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            } else {
                MiuixTextButton(
                    text = stringResource(R.string.permission_grant_action),
                    onClick = onClick
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PermissionSettingsMaterial(
    state: PermissionState,
    actions: PermissionActions,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.permission_section)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (state.requiredGranted) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.errorContainer
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (state.requiredGranted) {
                                stringResource(R.string.permission_status_ready_title)
                            } else {
                                stringResource(R.string.permission_status_missing_title)
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (state.requiredGranted) {
                                stringResource(R.string.permission_ready)
                            } else {
                                stringResource(R.string.permission_missing)
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PermissionRowMaterial(
                            title = stringResource(R.string.permission_storage),
                            granted = state.storage,
                            icon = Icons.Default.Folder,
                            onClick = actions.onRequestStorage
                        )
                        PermissionRowMaterial(
                            title = stringResource(R.string.permission_notification),
                            granted = state.notification,
                            icon = Icons.Default.Notifications,
                            onClick = actions.onRequestNotification
                        )
                        PermissionRowMaterial(
                            title = stringResource(R.string.permission_microphone),
                            granted = state.microphone,
                            icon = Icons.Default.Mic,
                            onClick = actions.onRequestMicrophone
                        )
                        PermissionRowMaterial(
                            title = stringResource(R.string.permission_battery),
                            granted = state.batteryWhitelist,
                            icon = Icons.Default.BatterySaver,
                            onClick = actions.onRequestBattery
                        )
                        PermissionRowMaterial(
                            title = stringResource(R.string.permission_overlay),
                            granted = state.overlay,
                            required = false,
                            icon = Icons.Default.WebAsset,
                            onClick = actions.onRequestOverlay
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionRowMaterial(
    title: String,
    granted: Boolean,
    icon: ImageVector,
    required: Boolean = true,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null)
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = when {
                    granted -> stringResource(R.string.permission_granted)
                    required -> stringResource(R.string.permission_required)
                    else -> stringResource(R.string.permission_optional)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (granted) {
            AssistChip(
                onClick = {},
                label = { Text(stringResource(R.string.permission_granted)) },
                leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null) }
            )
        } else {
            OutlinedButton(onClick = onClick) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
                Text(stringResource(R.string.permission_grant_action))
            }
        }
    }
}
