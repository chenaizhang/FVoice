package com.fvoice.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults as MiuixCardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun FVoiceMiuixSearchFilterCard(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    dropdownTitle: String,
    items: List<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = MiuixCardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceContainer),
            insideMargin = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MiuixTheme.colorScheme.surfaceContainerHigh, CircleShape)
                    .padding(horizontal = 2.dp, vertical = 1.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    modifier = Modifier.padding(start = 14.dp, end = 8.dp),
                    tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = TextStyle(
                        color = MiuixTheme.colorScheme.onSurface,
                        fontSize = 15.sp,
                    ),
                    cursorBrush = SolidColor(MiuixTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    decorationBox = { innerTextField ->
                        Box {
                            if (query.isBlank()) {
                                MiuixText(
                                    text = placeholder,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                    fontSize = 15.sp,
                                )
                            }
                            innerTextField()
                        }
                    },
                )
            }
        }

        FVoiceMiuixFilterCard(
            dropdownTitle = dropdownTitle,
            items = items,
            selectedIndex = selectedIndex,
            onSelectedIndexChange = onSelectedIndexChange,
        )
    }
}

@Composable
fun FVoiceMiuixFilterCard(
    dropdownTitle: String,
    items: List<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = MiuixCardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceContainer),
    ) {
        OverlayDropdownPreference(
            title = dropdownTitle,
            items = items,
            selectedIndex = selectedIndex,
            onSelectedIndexChange = onSelectedIndexChange,
        )
    }
}

@Composable
fun FVoiceMiuixLogEntryCard(
    title: String,
    description: String?,
    timestamp: String?,
    tags: List<String>,
    status: String?,
    statusColor: Color = MiuixTheme.colorScheme.onSurfaceVariantActions,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp),
        onClick = { onClick?.invoke() },
        showIndication = onClick != null,
        insideMargin = PaddingValues(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                MiuixText(
                    text = title,
                    modifier = Modifier.basicMarquee(),
                    fontWeight = FontWeight(550),
                    color = MiuixTheme.colorScheme.onSurface,
                    maxLines = 1,
                    softWrap = false,
                )
                description?.let {
                    MiuixText(
                        text = it,
                        fontSize = 12.sp,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                timestamp?.let {
                    MiuixText(
                        text = it,
                        modifier = Modifier.basicMarquee(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight(550),
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
                FVoiceMiuixStatusTags(tags)
            }
            status?.let {
                MiuixText(
                    text = it,
                    color = statusColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight(550),
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(start = 16.dp),
                )
            }
        }
    }
}

@Composable
fun FVoiceMiuixMessageCard(
    title: String,
    summary: String? = null,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            MiuixText(
                text = title,
                fontSize = 17.sp,
                fontWeight = FontWeight(550),
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
            summary?.let {
                MiuixText(
                    text = it,
                    fontSize = 14.sp,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun FVoiceMiuixStatusTags(tags: List<String>) {
    if (tags.isEmpty()) return
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        val colors = listOf(
            MiuixTheme.colorScheme.primary to MiuixTheme.colorScheme.onPrimary,
            MiuixTheme.colorScheme.secondaryContainer to MiuixTheme.colorScheme.onSecondaryContainer,
            MiuixTheme.colorScheme.tertiaryContainer to MiuixTheme.colorScheme.onTertiaryContainer,
        )
        tags.forEachIndexed { index, tag ->
            val (bg, fg) = colors.getOrElse(index) { colors.last() }
            FVoiceMiuixStatusTag(label = tag, backgroundColor = bg, contentColor = fg)
        }
    }
}

@Composable
fun FVoiceMiuixStatusTag(
    label: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color,
    contentColor: Color,
) {
    Box(
        modifier = modifier.background(
            color = backgroundColor,
            shape = RoundedCornerShape(6.dp),
        ),
    ) {
        MiuixText(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            text = label,
            color = contentColor,
            fontSize = 9.sp,
            fontWeight = FontWeight(750),
            maxLines = 1,
            softWrap = false,
        )
    }
}

@Composable
fun FVoiceMaterialSearchFilterCard(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    dropdownTitle: String,
    items: List<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text(placeholder) },
                leadingIcon = {
                    androidx.compose.material3.Icon(Icons.Outlined.Search, contentDescription = null)
                },
            )
            FVoiceMaterialFilterCard(
                dropdownTitle = dropdownTitle,
                items = items,
                selectedIndex = selectedIndex,
                onSelectedIndexChange = onSelectedIndexChange,
            )
        }
    }
}

@Composable
fun FVoiceMaterialFilterCard(
    dropdownTitle: String,
    items: List<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val expanded = remember { mutableStateOf(false) }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded.value = true }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = dropdownTitle,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = items.getOrNull(selectedIndex).orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            DropdownMenu(
                expanded = expanded.value,
                onDismissRequest = { expanded.value = false },
            ) {
                items.forEachIndexed { index, item ->
                    DropdownMenuItem(
                        text = { Text(item) },
                        onClick = {
                            expanded.value = false
                            onSelectedIndexChange(index)
                        },
                    )
                }
            }
        }
    }
}

val LocalFVoiceSegmentShape = compositionLocalOf { RoundedCornerShape(0.dp) }

@Composable
fun FVoiceMaterialSegmentedColumn(
    modifier: Modifier = Modifier,
    itemCount: Int,
    content: @Composable (Int) -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        repeat(itemCount) { index ->
            CompositionLocalProvider(
                LocalFVoiceSegmentShape provides fVoiceSegmentShape(index, itemCount),
            ) {
                content(index)
            }
        }
    }
}

@Composable
fun FVoiceMaterialLogEntryItem(
    title: String,
    description: String?,
    timestamp: String?,
    tags: List<String>,
    status: String?,
    statusColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = LocalFVoiceSegmentShape.current,
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                description?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                timestamp?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                FVoiceMaterialStatusTags(tags)
            }
            status?.let {
                Text(
                    text = it,
                    color = statusColor,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(start = 16.dp),
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
fun FVoiceMaterialMessageCard(
    title: String,
    summary: String? = null,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, color = MaterialTheme.colorScheme.outline)
            summary?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.outline,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun FVoiceMaterialStatusTags(tags: List<String>) {
    if (tags.isEmpty()) return
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        val colors = listOf(
            MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary,
            MaterialTheme.colorScheme.secondary to MaterialTheme.colorScheme.onSecondary,
            MaterialTheme.colorScheme.tertiary to MaterialTheme.colorScheme.onTertiary,
        )
        tags.forEachIndexed { index, tag ->
            val (bg, fg) = colors.getOrElse(index) { colors.last() }
            FVoiceMaterialStatusTag(label = tag, backgroundColor = bg, contentColor = fg)
        }
    }
}

@Composable
fun FVoiceMaterialStatusTag(
    label: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color,
    contentColor: Color,
) {
    Box(
        modifier = modifier
            .padding(end = 4.dp)
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(4.dp),
            ),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(vertical = 2.dp, horizontal = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = contentColor,
        )
    }
}

private fun fVoiceSegmentShape(index: Int, count: Int): RoundedCornerShape {
    val radius = 18.dp
    val middle = 6.dp
    return when {
        count <= 1 -> RoundedCornerShape(radius)
        index == 0 -> RoundedCornerShape(topStart = radius, topEnd = radius, bottomStart = middle, bottomEnd = middle)
        index == count - 1 -> RoundedCornerShape(topStart = middle, topEnd = middle, bottomStart = radius, bottomEnd = radius)
        else -> RoundedCornerShape(middle)
    }
}
