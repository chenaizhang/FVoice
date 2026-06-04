package com.fvoice.app.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fvoice.app.ui.theme.LocalEnableBlur
import com.fvoice.app.ui.util.BlurredBar
import com.fvoice.app.ui.util.rememberBlurBackdrop
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

val LocalFVoiceMiuixBottomSpacing = staticCompositionLocalOf { 24.dp }

@Composable
fun FVoiceMiuixPage(
    title: String,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp),
    bottomSpacing: Dp? = null,
    navigationIcon: @Composable () -> Unit = {},
    content: LazyListScope.() -> Unit,
) {
    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberBlurBackdrop(LocalEnableBlur.current)
    val barColor = if (backdrop != null) Color.Transparent else MiuixTheme.colorScheme.surface
    val actualBottomSpacing = bottomSpacing ?: LocalFVoiceMiuixBottomSpacing.current
    val layoutDirection = LocalLayoutDirection.current

    Scaffold(
        topBar = {
            BlurredBar(backdrop) {
                TopAppBar(
                    color = barColor,
                    title = title,
                    navigationIcon = navigationIcon,
                    scrollBehavior = scrollBehavior
                )
            }
        },
        contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout).only(WindowInsetsSides.Horizontal)
    ) { innerPadding ->
        Box(modifier = if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier) {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .scrollEndHaptic()
                    .overScrollVertical()
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .padding(contentPadding),
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding(),
                    start = innerPadding.calculateLeftPadding(layoutDirection),
                    end = innerPadding.calculateRightPadding(layoutDirection),
                    bottom = actualBottomSpacing + 12.dp
                ),
                overscrollEffect = null,
            ) {
                content()
            }
        }
    }
}

@Composable
fun FVoiceMiuixCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(modifier = modifier.fillMaxWidth().padding(top = 8.dp)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content
        )
    }
}

@Composable
fun FVoiceMiuixTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier.padding(horizontal = 4.dp),
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
    )
}

@Composable
fun FVoiceMiuixInfoRow(
    title: String,
    summary: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    end: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MiuixTheme.colorScheme.onBackground
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            if (summary.isNotBlank()) {
                Text(text = summary, fontSize = 13.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            }
        }
        end?.invoke()
    }
}

@Composable
fun FVoiceMiuixSearchBar(
    query: String,
    @Suppress("UNUSED_PARAMETER") onQueryChange: (String) -> Unit,
    label: String,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary
            )
            Text(
                text = query.ifBlank { label },
                color = if (query.isBlank()) {
                    MiuixTheme.colorScheme.onSurfaceVariantSummary
                } else {
                    MiuixTheme.colorScheme.onSurface
                },
                fontSize = 15.sp
            )
        }
    }
}

@Composable
fun FVoiceMiuixSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(14.dp)
    val selectedPillShape = RoundedCornerShape(11.dp)
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(shape)
            .background(MiuixTheme.colorScheme.surfaceContainerHigh)
    ) {
        val optionCount = options.size.coerceAtLeast(1)
        val itemWidth = maxWidth / optionCount
        val pillOffset by animateDpAsState(
            targetValue = itemWidth * selectedIndex.coerceIn(0, optionCount - 1),
            animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
            label = "segmentedPillOffset"
        )

        Box(
            modifier = Modifier
                .offset(x = pillOffset)
                .width(itemWidth)
                .height(44.dp)
                .padding(3.dp)
                .clip(selectedPillShape)
                .background(MiuixTheme.colorScheme.surface)
        )

        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            options.forEachIndexed { index, option ->
                val selected = selectedIndex == index
                val textColor by animateColorAsState(
                    targetValue = if (selected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
                    label = "segmentedTextColor"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clickable(
                            indication = null,
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            onClick = { onSelected(index) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = option,
                        color = textColor,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                    )
                }
            }
        }
    }
}
