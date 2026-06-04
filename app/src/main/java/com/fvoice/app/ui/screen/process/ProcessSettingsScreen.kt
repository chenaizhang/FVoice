package com.fvoice.app.ui.screen.process

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
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
import androidx.compose.material3.RadioButton
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
import com.fvoice.app.R
import com.fvoice.app.data.model.DenoiseStrength
import com.fvoice.app.data.model.UiMode
import com.fvoice.app.data.model.ProcessMode
import com.fvoice.app.ui.component.FVoiceMiuixCard
import com.fvoice.app.ui.component.FVoiceMiuixPage
import com.fvoice.app.ui.component.FVoiceMiuixSegmentedControl
import com.fvoice.app.ui.component.FVoiceMiuixTitle
import com.fvoice.app.ui.theme.LocalUiMode
import top.yukonga.miuix.kmp.basic.Button as MiuixButton
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.RadioButton as MiuixRadioButton
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProcessSettingsScreen(
    onStartProcess: () -> Unit,
    onBack: () -> Unit
) {
    var selectedMode by remember { mutableStateOf(ProcessMode.DENOISE_AND_TRANSCRIBE) }
    var selectedStrength by remember { mutableStateOf(DenoiseStrength.STANDARD) }

    if (LocalUiMode.current == UiMode.Miuix) {
        FVoiceMiuixPage(
            title = stringResource(R.string.process_settings_title),
            navigationIcon = {
                MiuixIconButton(onClick = onBack) {
                    MiuixIcon(MiuixIcons.Back, contentDescription = null)
                }
            }
        ) {
            item {
                MiuixText(
                    text = stringResource(R.string.process_settings_subtitle),
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp, end = 4.dp),
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    fontWeight = FontWeight.Medium
                )
            }

            item { FVoiceMiuixTitle(stringResource(R.string.process_mode), Modifier.padding(top = 12.dp)) }

            item {
                FVoiceMiuixCard {
                    ProcessMode.values().forEach { mode ->
                        ProcessModeMiuixRow(
                            title = mode.label,
                            selected = selectedMode == mode,
                            onClick = { selectedMode = mode }
                        )
                    }
                }
            }

            item { FVoiceMiuixTitle(stringResource(R.string.denoise_strength), Modifier.padding(top = 12.dp)) }

            item {
                FVoiceMiuixSegmentedControl(
                    options = DenoiseStrength.values().map { it.label },
                    selectedIndex = DenoiseStrength.values().indexOf(selectedStrength).coerceAtLeast(0),
                    onSelected = { selectedStrength = DenoiseStrength.values()[it] },
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            item {
                MiuixButton(
                    onClick = onStartProcess,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    MiuixText(stringResource(R.string.start_process))
                }
            }
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.process_settings_title)) },
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
                text = stringResource(R.string.process_settings_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Process Mode Selection
            Text(
                text = stringResource(R.string.process_mode),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(8.dp)) {
                    ProcessMode.values().forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = selectedMode == mode,
                                    onClick = { selectedMode = mode }
                                )
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedMode == mode,
                                onClick = { selectedMode = mode }
                            )
                            Column(modifier = Modifier.padding(start = 8.dp)) {
                                Text(text = mode.label, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // Denoise Strength
            Text(
                text = stringResource(R.string.denoise_strength),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DenoiseStrength.values().forEach { strength ->
                    FilterChip(
                        selected = selectedStrength == strength,
                        onClick = { selectedStrength = strength },
                        label = { Text(strength.label) }
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onStartProcess,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.start_process))
            }
        }
    }
}

@Composable
private fun ProcessModeMiuixRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MiuixRadioButton(
            selected = selected,
            onClick = onClick
        )
        Column(modifier = Modifier.weight(1f)) {
            MiuixText(
                text = title,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) {
                    MiuixTheme.colorScheme.primary
                } else {
                    MiuixTheme.colorScheme.onSurface
                }
            )
            MiuixText(
                text = stringResource(R.string.process_mode_summary),
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
