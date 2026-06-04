package com.fvoice.app.ui.component

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List as FilledList
import androidx.compose.material.icons.automirrored.outlined.List as OutlinedList
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fvoice.app.R
import com.fvoice.app.data.model.UiMode
import com.fvoice.app.ui.navigation3.LocalMainPagerState
import com.fvoice.app.ui.theme.LocalEnableFloatingBottomBar
import com.fvoice.app.ui.theme.LocalEnableFloatingBottomBarBlur
import com.fvoice.app.ui.theme.LocalUiMode
import com.fvoice.app.ui.util.BlurredBar
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.NavigationBar as MiuixNavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem as MiuixNavigationBarItem
import top.yukonga.miuix.kmp.basic.NavigationItem
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

enum class BottomNavDestination(
    @get:StringRes val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    Home(
        labelRes = R.string.nav_home,
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    ),
    TaskList(
        labelRes = R.string.nav_tasks,
        selectedIcon = Icons.AutoMirrored.Filled.FilledList,
        unselectedIcon = Icons.AutoMirrored.Outlined.OutlinedList
    ),
    Transcript(
        labelRes = R.string.nav_transcript,
        selectedIcon = Icons.Filled.TextFields,
        unselectedIcon = Icons.Outlined.TextFields
    ),
    Settings(
        labelRes = R.string.nav_settings,
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    );
}

@Composable
fun BottomNavBar(
    blurBackdrop: LayerBackdrop? = null,
    backdrop: Backdrop? = null,
) {
    when (LocalUiMode.current) {
        UiMode.Miuix -> BottomNavBarMiuix(
            blurBackdrop = blurBackdrop,
            backdrop = backdrop,
        )
        UiMode.Material -> BottomNavBarMaterial()
    }
}

@Composable
private fun BottomNavBarMiuix(
    blurBackdrop: LayerBackdrop?,
    backdrop: Backdrop?,
) {
    val mainPagerState = LocalMainPagerState.current
    val enableFloatingBottomBar = LocalEnableFloatingBottomBar.current
    val enableFloatingBottomBarBlur = LocalEnableFloatingBottomBarBlur.current

    val items = BottomNavDestination.entries.map { destination ->
        NavigationItem(
            label = stringResource(destination.labelRes),
            icon = destination.selectedIcon,
        )
    }

    if (!enableFloatingBottomBar) {
        BlurredBar(blurBackdrop) {
            MiuixNavigationBar(
                color = if (blurBackdrop != null) androidx.compose.ui.graphics.Color.Transparent else MiuixTheme.colorScheme.surface,
                content = {
                    items.forEachIndexed { index, item ->
                        MiuixNavigationBarItem(
                            modifier = Modifier.weight(1f),
                            icon = item.icon,
                            label = item.label,
                            selected = mainPagerState.selectedPage == index,
                            onClick = { mainPagerState.animateToPage(index) }
                        )
                    }
                }
            )
        }
    } else {
        if (backdrop != null) {
            FloatingBottomBar(
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    )
                    .padding(bottom = 12.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
                selectedIndex = { mainPagerState.selectedPage },
                onSelected = { mainPagerState.animateToPage(it) },
                backdrop = backdrop,
                tabsCount = items.size,
                isBlurEnabled = enableFloatingBottomBarBlur,
            ) {
                items.forEachIndexed { index, item ->
                    FloatingBottomBarItem(
                        onClick = { mainPagerState.animateToPage(index) },
                        modifier = Modifier.defaultMinSize(minWidth = 76.dp)
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = MiuixTheme.colorScheme.onSurface
                        )
                        Text(
                            text = item.label,
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                            color = MiuixTheme.colorScheme.onSurface,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Visible
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomNavBarMaterial() {
    val mainPagerState = LocalMainPagerState.current

    NavigationBar {
        BottomNavDestination.entries.forEachIndexed { index, destination ->
            val selected = mainPagerState.selectedPage == index
            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (!selected) {
                        mainPagerState.animateToPage(index)
                    }
                },
                icon = {
                    androidx.compose.material3.Icon(
                        if (selected) destination.selectedIcon else destination.unselectedIcon,
                        stringResource(destination.labelRes)
                    )
                },
                label = {
                    androidx.compose.material3.Text(
                        stringResource(destination.labelRes),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            )
        }
    }
}
