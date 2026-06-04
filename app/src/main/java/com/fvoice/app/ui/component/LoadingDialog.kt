package com.fvoice.app.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.fvoice.app.data.model.UiMode
import com.fvoice.app.ui.theme.LocalUiMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

interface LoadingDialogHandle {
    val isShown: Boolean
    fun show()
    fun hide()
    suspend fun <R> withLoading(block: suspend () -> R): R
}

private class LoadingDialogHandleImpl(
    val visible: MutableState<Boolean>,
    val coroutineScope: CoroutineScope
) : LoadingDialogHandle {
    override val isShown: Boolean get() = visible.value
    override fun show() { coroutineScope.launch { visible.value = true } }
    override fun hide() { coroutineScope.launch { visible.value = false } }
    override suspend fun <R> withLoading(block: suspend () -> R): R {
        return coroutineScope.async {
            try {
                visible.value = true
                block()
            } finally {
                visible.value = false
            }
        }.await()
    }
}

@Composable
fun rememberLoadingDialog(): LoadingDialogHandle {
    val visible = remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val uiMode = LocalUiMode.current

    if (visible.value) {
        when (uiMode) {
            UiMode.Miuix -> LoadingDialogMiuix()
            UiMode.Material -> LoadingDialogMaterial()
        }
    }

    return remember {
        LoadingDialogHandleImpl(visible, coroutineScope)
    }
}

@Composable
private fun LoadingDialogMaterial() {
    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Box(
            modifier = Modifier.wrapContentSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun LoadingDialogMiuix() {
    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Box(
            modifier = Modifier.wrapContentSize(),
            contentAlignment = Alignment.Center
        ) {
            top.yukonga.miuix.kmp.basic.CircularProgressIndicator(
                modifier = Modifier.size(48.dp)
            )
        }
    }
}
