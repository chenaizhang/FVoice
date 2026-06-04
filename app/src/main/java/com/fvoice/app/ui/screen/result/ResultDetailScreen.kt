package com.fvoice.app.ui.screen.result

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fvoice.app.R
import com.fvoice.app.data.model.UiMode
import com.fvoice.app.ui.component.FVoiceMiuixCard
import com.fvoice.app.ui.component.FVoiceMiuixInfoRow
import com.fvoice.app.ui.component.FVoiceMiuixPage
import com.fvoice.app.ui.component.FVoiceMiuixSegmentedControl
import com.fvoice.app.ui.component.FVoiceMiuixTitle
import com.fvoice.app.ui.theme.LocalUiMode
import top.yukonga.miuix.kmp.basic.Button as MiuixButton
import top.yukonga.miuix.kmp.basic.Card as MiuixCard
import top.yukonga.miuix.kmp.basic.CardDefaults as MiuixCardDefaults
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultDetailScreen(
    onBack: () -> Unit
) {
    var selectedExportFormat by remember { mutableStateOf("M4A") }
    val exportFormats = listOf("M4A", "MP4", "TXT", "VTT")

    if (LocalUiMode.current == UiMode.Miuix) {
        ResultDetailMiuix(
            selectedExportFormat = selectedExportFormat,
            exportFormats = exportFormats,
            onExportFormatSelected = { selectedExportFormat = it },
            onBack = onBack
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.result_detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "meeting_interview.mp4 · ${stringResource(R.string.status_completed)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Player placeholder
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.player_placeholder),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.padding(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = false,
                            onClick = {},
                            label = { Text(stringResource(R.string.player_original)) }
                        )
                        FilterChip(
                            selected = true,
                            onClick = {},
                            label = { Text(stringResource(R.string.player_denoised)) }
                        )
                    }
                    Text("00:38 / 12:43", style = MaterialTheme.typography.bodySmall)
                }
            }

            // Transcript text
            Text(
                text = stringResource(R.string.transcript_text),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "00:00:03",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text("${stringResource(R.string.transcript_sample_1)}")
                    Spacer(modifier = Modifier.padding(8.dp))
                    Text(
                        text = "00:00:18",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text("${stringResource(R.string.transcript_sample_2)}")
                }
            }

            // Export card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.export_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.padding(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        exportFormats.forEach { format ->
                            FilterChip(
                                selected = selectedExportFormat == format,
                                onClick = { selectedExportFormat = format },
                                label = { Text(format) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultDetailMiuix(
    selectedExportFormat: String,
    exportFormats: List<String>,
    onExportFormatSelected: (String) -> Unit,
    onBack: () -> Unit,
) {
    var selectedTrack by remember { mutableStateOf(1) }

    FVoiceMiuixPage(
        title = stringResource(R.string.result_detail_title),
        navigationIcon = {
            MiuixIconButton(onClick = onBack) {
                MiuixIcon(MiuixIcons.Back, contentDescription = null)
            }
        }
    ) {
        item {
            ResultSummaryMiuixCard()
        }

        item {
            ResultPlayerMiuixCard(
                selectedTrack = selectedTrack,
                onTrackSelected = { selectedTrack = it }
            )
        }

        item {
            FVoiceMiuixTitle(
                text = stringResource(R.string.transcript_text),
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        item {
            ResultTranscriptMiuixCard()
        }

        item {
            FVoiceMiuixTitle(
                text = stringResource(R.string.export_title),
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        item {
            ResultExportMiuixCard(
                selectedExportFormat = selectedExportFormat,
                exportFormats = exportFormats,
                onExportFormatSelected = onExportFormatSelected
            )
        }
    }
}

@Composable
private fun ResultSummaryMiuixCard() {
    FVoiceMiuixCard {
        FVoiceMiuixInfoRow(
            title = "meeting_interview.mp4",
            summary = "12:43 · ${stringResource(R.string.status_completed)}",
            end = {
                MiuixText(
                    text = stringResource(R.string.status_completed),
                    color = MiuixTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        )
    }
}

@Composable
private fun ResultPlayerMiuixCard(
    selectedTrack: Int,
    onTrackSelected: (Int) -> Unit,
) {
    FVoiceMiuixCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(74.dp)
                    .background(MiuixTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                MiuixText(
                    text = "▶",
                    color = MiuixTheme.colorScheme.primary,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            MiuixText(
                text = "00:38 / 12:43",
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                fontWeight = FontWeight.Medium
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(MiuixTheme.colorScheme.surfaceContainerHigh, CircleShape)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.05f)
                        .height(4.dp)
                        .background(MiuixTheme.colorScheme.primary, CircleShape)
                )
            }
            FVoiceMiuixSegmentedControl(
                options = listOf(
                    stringResource(R.string.player_original),
                    stringResource(R.string.player_denoised)
                ),
                selectedIndex = selectedTrack,
                onSelected = onTrackSelected
            )
        }
    }
}

@Composable
private fun ResultTranscriptMiuixCard() {
    FVoiceMiuixCard {
        TranscriptLineMiuix(time = "00:00:03", text = stringResource(R.string.transcript_sample_1))
        TranscriptLineMiuix(time = "00:00:18", text = stringResource(R.string.transcript_sample_2))
    }
}

@Composable
private fun TranscriptLineMiuix(
    time: String,
    text: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        MiuixText(
            text = time,
            color = MiuixTheme.colorScheme.primary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
        MiuixText(
            text = text,
            color = MiuixTheme.colorScheme.onSurface,
            fontSize = 16.sp
        )
    }
}

@Composable
private fun ResultExportMiuixCard(
    selectedExportFormat: String,
    exportFormats: List<String>,
    onExportFormatSelected: (String) -> Unit,
) {
    MiuixCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        colors = MiuixCardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceContainer)
    ) {
        Column {
            exportFormats.forEachIndexed { index, format ->
                ExportFormatMiuixRow(
                    format = format,
                    selected = selectedExportFormat == format,
                    onClick = { onExportFormatSelected(format) }
                )
                if (index != exportFormats.lastIndex) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MiuixTheme.colorScheme.surfaceContainerHigh)
                    )
                }
            }
            MiuixButton(
                onClick = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                MiuixText(stringResource(R.string.export_title))
            }
        }
    }
}

@Composable
private fun ExportFormatMiuixRow(
    format: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clickable(
                indication = null,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                onClick = onClick
            )
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        MiuixText(
            text = format,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurface
        )
        if (selected) {
            MiuixText(
                text = "OK",
                color = MiuixTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
