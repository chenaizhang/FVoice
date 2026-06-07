package com.clarivo.app.ui.screen.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.clarivo.app.R
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference

data class SettingOption<T>(
    val value: T,
    val label: String,
)

@Composable
fun <T> MaterialDropdownPreference(
    title: String,
    selected: T,
    options: List<SettingOption<T>>,
    modifier: Modifier = Modifier,
    summary: String? = options.firstOrNull { it.value == selected }?.label,
    onSelected: (T) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }
    val selectedIndex = options.indexOfFirst { it.value == selected }.coerceAtLeast(0)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { showDialog = true }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (summary != null) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(title) },
            text = {
                Column {
                    options.forEachIndexed { index, option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelected(option.value)
                                    showDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = index == selectedIndex,
                                onClick = {
                                    onSelected(option.value)
                                    showDialog = false
                                }
                            )
                            Text(
                                text = option.label,
                                modifier = Modifier.padding(start = 8.dp),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun <T> MiuixDropdownPreference(
    title: String,
    selected: T,
    options: List<SettingOption<T>>,
    modifier: Modifier = Modifier,
    summary: String? = options.firstOrNull { it.value == selected }?.label,
    startAction: @Composable (() -> Unit)? = null,
    onSelected: (T) -> Unit,
) {
    val selectedIndex = options.indexOfFirst { it.value == selected }.coerceAtLeast(0)

    OverlayDropdownPreference(
        modifier = modifier,
        title = title,
        summary = summary,
        items = options.map { it.label },
        startAction = startAction,
        selectedIndex = selectedIndex,
        onSelectedIndexChange = { index ->
            if (index in options.indices) {
                onSelected(options[index].value)
            }
        }
    )
}
