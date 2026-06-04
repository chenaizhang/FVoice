package com.fvoice.app.ui.screen.welcome

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fvoice.app.R
import com.fvoice.app.data.model.UiMode
import com.fvoice.app.ui.component.FVoiceMiuixCard
import com.fvoice.app.ui.component.FVoiceMiuixInfoRow
import com.fvoice.app.ui.theme.LocalUiMode
import top.yukonga.miuix.kmp.basic.Button as MiuixButton
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit
) {
    if (LocalUiMode.current == UiMode.Miuix) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            MiuixIcon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                modifier = Modifier.size(76.dp),
                tint = MiuixTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.padding(18.dp))
            MiuixText(
                text = stringResource(R.string.welcome_slogan),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.padding(12.dp))
            FVoiceMiuixCard {
                FVoiceMiuixInfoRow(
                    title = stringResource(R.string.app_name),
                    summary = stringResource(R.string.welcome_desc)
                )
                TabRow(
                    tabs = listOf(
                        stringResource(R.string.welcome_tag_local),
                        stringResource(R.string.welcome_tag_bilingual),
                        stringResource(R.string.welcome_tag_subtitle)
                    ),
                    selectedTabIndex = 0,
                    onTabSelected = {},
                    height = 44.dp
                )
            }
            Spacer(modifier = Modifier.padding(24.dp))
            MiuixButton(onClick = onGetStarted, modifier = Modifier.fillMaxWidth()) {
                MiuixText(stringResource(R.string.welcome_get_started))
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.padding(24.dp))

        Text(
            text = stringResource(R.string.welcome_slogan),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.padding(16.dp))

        Text(
            text = stringResource(R.string.welcome_desc),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.padding(24.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                R.string.welcome_tag_local,
                R.string.welcome_tag_bilingual,
                R.string.welcome_tag_subtitle
            ).forEach { tagRes ->
                androidx.compose.material3.FilterChip(
                    selected = false,
                    onClick = {},
                    label = { Text(stringResource(tagRes)) }
                )
            }
        }

        Spacer(modifier = Modifier.padding(32.dp))

        Button(
            onClick = onGetStarted,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.welcome_get_started))
        }
    }
}
