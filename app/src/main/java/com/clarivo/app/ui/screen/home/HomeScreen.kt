package com.clarivo.app.ui.screen.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clarivo.app.R
import com.clarivo.app.data.model.UiMode
import com.clarivo.app.permission.PermissionManager
import com.clarivo.app.permission.PermissionState
import com.clarivo.app.ui.component.ClarivoMiuixCard
import com.clarivo.app.ui.component.ClarivoMiuixPage
import com.clarivo.app.ui.navigation3.LocalMainPagerState
import com.clarivo.app.ui.navigation3.LocalNavigator
import com.clarivo.app.ui.navigation3.Route
import com.clarivo.app.ui.theme.LocalUiMode
import com.clarivo.app.viewmodel.HomeViewModel
import top.yukonga.miuix.kmp.basic.Card as MiuixCard
import top.yukonga.miuix.kmp.basic.CardDefaults as MiuixCardDefaults
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onImportAudio: () -> Unit,
    onImportVideo: () -> Unit,
    onNavigateToPermissions: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val permissionManager = remember(context) { PermissionManager(context) }
    val permissionState by permissionManager.state.collectAsState()

    DisposableEffect(lifecycleOwner, permissionManager) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionManager.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Refresh when Home page becomes visible in pager or when returning from detail
    val mainPagerState = LocalMainPagerState.current
    val navigator = LocalNavigator.current
    val isTopLevel by remember {
        derivedStateOf {
            navigator.backStack.lastOrNull() is Route.Main
        }
    }
    androidx.compose.runtime.LaunchedEffect(isTopLevel, mainPagerState.pagerState.settledPage) {
        if (mainPagerState.pagerState.settledPage == 0 && isTopLevel) {
            viewModel.refreshTasks()
        }
    }

    if (LocalUiMode.current == UiMode.Miuix) {
        HomeMiuix(
            permissionState = permissionState,
            onImportAudio = onImportAudio,
            onImportVideo = onImportVideo,
            onNavigateToPermissions = onNavigateToPermissions
        )
    } else {
        HomeMaterial(
            permissionState = permissionState,
            onImportAudio = onImportAudio,
            onImportVideo = onImportVideo,
            onNavigateToPermissions = onNavigateToPermissions
        )
    }
}

@Composable
private fun HomeMaterial(
    permissionState: PermissionState,
    onImportAudio: () -> Unit,
    onImportVideo: () -> Unit,
    onNavigateToPermissions: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.home_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            PermissionCard(
                state = permissionState,
                onClick = onNavigateToPermissions
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.home_start_process),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.home_start_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(onClick = onImportAudio) {
                            Icon(Icons.Default.MusicNote, contentDescription = null)
                            Text(stringResource(R.string.home_import_audio))
                        }
                        OutlinedButton(onClick = onImportVideo) {
                            Icon(Icons.Default.Videocam, contentDescription = null)
                            Text(stringResource(R.string.home_import_video))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeMiuix(
    permissionState: PermissionState,
    onImportAudio: () -> Unit,
    onImportVideo: () -> Unit,
    onNavigateToPermissions: () -> Unit,
) {
    ClarivoMiuixPage(title = stringResource(R.string.app_name)) {
        item {
            PermissionCardMiuix(
                state = permissionState,
                onClick = onNavigateToPermissions
            )
        }

        item { MiuixImportPanel(onImportAudio = onImportAudio, onImportVideo = onImportVideo) }
    }
}

@Composable
private fun MiuixImportPanel(
    onImportAudio: () -> Unit,
    onImportVideo: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MiuixImportChoice(
            title = stringResource(R.string.home_import_audio),
            summary = stringResource(R.string.import_audio_summary),
            icon = Icons.Default.MusicNote,
            onClick = onImportAudio
        )
        MiuixImportChoice(
            title = stringResource(R.string.home_import_video),
            summary = stringResource(R.string.import_video_summary),
            icon = Icons.Default.Videocam,
            onClick = onImportVideo
        )
    }
}

@Composable
private fun MiuixImportChoice(
    title: String,
    summary: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    MiuixCard(
        modifier = Modifier.fillMaxWidth(),
        colors = MiuixCardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceContainer),
        onClick = onClick,
        showIndication = true
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(MiuixTheme.colorScheme.surfaceContainerHigh, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                MiuixIcon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MiuixTheme.colorScheme.primary
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                MiuixText(text = title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                MiuixText(
                    text = summary,
                    fontSize = 13.sp,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
            }
            MiuixText(
                text = "›",
                color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun PermissionCardMiuix(
    state: PermissionState,
    onClick: () -> Unit,
) {
    val requiredGranted = state.requiredGranted
    val iconColor = if (requiredGranted) {
        Color(0xFF36D167)
    } else {
        Color(0xFFF72727)
    }
    val containerColor = if (requiredGranted) {
        Color(0xFFDFFAE4)
    } else {
        Color(0xFFF8E2E2)
    }
    val textColor = Color(0xFF111111)
    val summaryColor = textColor.copy(alpha = 0.72f)
    val summary = if (requiredGranted) {
        stringResource(R.string.permission_ready)
    } else {
        stringResource(R.string.permission_missing)
    }

    MiuixCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        colors = MiuixCardDefaults.defaultColors(color = containerColor),
        onClick = onClick,
        showIndication = true
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(164.dp)
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .offset(x = 70.dp, y = 44.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                MiuixIcon(
                    modifier = Modifier.size(182.dp),
                    imageVector = if (requiredGranted) Icons.Rounded.CheckCircleOutline else Icons.Rounded.Cancel,
                    tint = iconColor,
                    contentDescription = null
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 24.dp, top = 28.dp, end = 148.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    MiuixText(
                        text = if (requiredGranted) {
                            stringResource(R.string.permission_status_ready_title)
                        } else {
                            stringResource(R.string.permission_status_missing_title)
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor
                    )
                    MiuixText(
                        text = summary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = summaryColor
                    )
                }
                MiuixText(
                    text = if (requiredGranted) {
                        stringResource(R.string.permission_granted)
                    } else {
                        stringResource(R.string.permission_action_required)
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = textColor.copy(alpha = 0.78f)
                )
            }
        }
    }
}

@Composable
private fun PermissionCard(
    state: PermissionState,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (state.requiredGranted) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.errorContainer
            },
            contentColor = if (state.requiredGranted) {
                MaterialTheme.colorScheme.onSecondaryContainer
            } else {
                MaterialTheme.colorScheme.onErrorContainer
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.permission_section),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (state.requiredGranted) {
                            stringResource(R.string.permission_ready)
                        } else {
                            stringResource(R.string.permission_missing)
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                AssistChip(
                    onClick = {},
                    colors = AssistChipDefaults.assistChipColors(
                        labelColor = if (state.requiredGranted) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        },
                        leadingIconContentColor = if (state.requiredGranted) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        }
                    ),
                    label = {
                        Text(
                            if (state.requiredGranted) {
                                stringResource(R.string.permission_granted)
                            } else {
                                stringResource(R.string.permission_action_required)
                            }
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = if (state.requiredGranted) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                            contentDescription = null
                        )
                    }
                )
            }
        }
    }
}
