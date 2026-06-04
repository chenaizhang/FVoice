package com.fvoice.app.ui.component.material

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.ui.res.stringResource
import com.fvoice.app.R
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.fvoice.app.ui.component.SearchStatus

// Search Box Composable
@Composable
fun SearchStatus.SearchBox(
    content: @Composable () -> Unit
) {
    if (shouldCollapsed()) content()
}

// Search Pager Composable
@Composable
fun SearchStatus.SearchPager(
    onSearchStatusChange: (SearchStatus) -> Unit,
    defaultResult: @Composable () -> Unit,
    expandBar: @Composable (SearchStatus, (SearchStatus) -> Unit, Dp) -> Unit = { searchStatus, onStatusChange, padding ->
        FVoiceMaterialSearchBar(searchStatus, onStatusChange, padding)
    },
    searchBarTopPadding: Dp = 12.dp,
    result: @Composable () -> Unit
) {
    val searchStatus = this
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val systemBarsPadding = WindowInsets.systemBars.asPaddingValues().calculateTopPadding()

    BackHandler(enabled = searchStatus.shouldExpand()) {
        keyboardController?.hide()
        focusManager.clearFocus()
        onSearchStatusChange(
            searchStatus.copy(
                searchText = "",
                current = SearchStatus.Status.COLLAPSING
            )
        )
    }

    val topPadding by animateDpAsState(
        targetValue = if (searchStatus.shouldExpand()) {
            systemBarsPadding + 5.dp
        } else {
            max(searchStatus.offsetY, 0.dp)
        },
        animationSpec = tween(300, easing = LinearOutSlowInEasing),
        label = "SearchPagerTopPadding"
    ) {
        onSearchStatusChange(searchStatus.onAnimationComplete())
    }
    val surfaceAlpha by animateFloatAsState(
        if (searchStatus.shouldExpand()) 1f else 0f,
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label = "SearchPagerSurfaceAlpha"
    )
    val surfaceColor = MaterialTheme.colorScheme.surfaceContainerHighest

    Column(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(5f)
            .drawBehind { drawRect(surfaceColor.copy(alpha = surfaceAlpha)) }
            .semantics { onClick { false } }
            .then(
                if (!searchStatus.isCollapsed()) Modifier.pointerInput(Unit) { } else Modifier
            )
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = topPadding)
                .then(
                    if (!searchStatus.isCollapsed()) Modifier.background(MaterialTheme.colorScheme.surface)
                    else Modifier
                ),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!searchStatus.isCollapsed()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    expandBar(searchStatus, onSearchStatusChange, searchBarTopPadding)
                }
            }
            AnimatedVisibility(
                visible = searchStatus.isExpand() || searchStatus.isAnimatingExpand(),
                enter = expandHorizontally() + slideInHorizontally(initialOffsetX = { it }),
                exit = shrinkHorizontally() + slideOutHorizontally(targetOffsetX = { it })
            ) {
                Text(
                    text = stringResource(R.string.cancel),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(start = 4.dp, end = 16.dp, top = searchBarTopPadding, bottom = 6.dp)
                        .clickable(
                            interactionSource = null,
                            enabled = searchStatus.isExpand(),
                            indication = null
                        ) {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            onSearchStatusChange(
                                searchStatus.copy(
                                    searchText = "",
                                    current = SearchStatus.Status.COLLAPSING
                                )
                            )
                        }
                )
            }
        }
        AnimatedVisibility(
            visible = searchStatus.isExpand(),
            modifier = Modifier
                .fillMaxSize()
                .zIndex(1f),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            when (searchStatus.resultStatus) {
                SearchStatus.ResultStatus.DEFAULT -> defaultResult()
                SearchStatus.ResultStatus.EMPTY -> {}
                SearchStatus.ResultStatus.LOAD -> {}
                SearchStatus.ResultStatus.SHOW -> result()
            }
        }
    }
}

@Composable
fun FVoiceMaterialSearchBar(
    searchStatus: SearchStatus,
    onSearchStatusChange: (SearchStatus) -> Unit,
    searchBarTopPadding: Dp = 12.dp,
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var textFieldValue by remember { mutableStateOf(TextFieldValue(searchStatus.searchText)) }

    LaunchedEffect(searchStatus.searchText) {
        if (textFieldValue.text != searchStatus.searchText) {
            textFieldValue = TextFieldValue(searchStatus.searchText)
        }
    }

    LaunchedEffect(searchStatus.current) {
        when (searchStatus.current) {
            SearchStatus.Status.EXPANDING -> focusRequester.requestFocus()
            SearchStatus.Status.COLLAPSING, SearchStatus.Status.COLLAPSED -> {
                keyboardController?.hide()
                focusManager.clearFocus()
            }
            else -> {}
        }
    }

    BasicTextField(
        value = textFieldValue,
        onValueChange = {
            textFieldValue = it
            onSearchStatusChange(searchStatus.copy(searchText = it.text))
        },
        singleLine = true,
        textStyle = TextStyle(
            fontWeight = FontWeight.Medium,
            fontSize = 17.sp,
            color = MaterialTheme.colorScheme.onSurface
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(top = searchBarTopPadding, bottom = 6.dp)
            .heightIn(min = 45.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape)
            .focusRequester(focusRequester),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "search",
                    modifier = Modifier
                        .size(44.dp)
                        .padding(start = 16.dp, end = 8.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Box(modifier = Modifier.weight(1f)) {
                    innerTextField()
                }
                AnimatedVisibility(
                    searchStatus.searchText.isNotEmpty(),
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut(),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        tint = MaterialTheme.colorScheme.onSurface,
                        contentDescription = "Clean",
                        modifier = Modifier
                            .size(44.dp)
                            .padding(start = 8.dp, end = 16.dp)
                            .clickable(
                                interactionSource = null,
                                indication = null
                            ) {
                                textFieldValue = TextFieldValue("")
                                onSearchStatusChange(searchStatus.copy(searchText = ""))
                            },
                    )
                }
            }
        }
    )
}

@Composable
fun FVoiceMaterialSearchBarFake(
    label: String,
    searchBarTopPadding: Dp = 12.dp,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(top = searchBarTopPadding, bottom = 6.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 45.dp)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
