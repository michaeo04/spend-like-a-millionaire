package com.michaeo04.spendlikeamillionaire.ui.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.michaeo04.spendlikeamillionaire.AppContainer
import com.michaeo04.spendlikeamillionaire.R
import com.michaeo04.spendlikeamillionaire.data.ImageCredit
import com.michaeo04.spendlikeamillionaire.domain.Item

private class CreditsData(val credits: List<ImageCredit>, val items: Map<String, Item>)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditsRoute(container: AppContainer, onBack: () -> Unit) {
    val context = LocalContext.current
    val settings by container.settingsStore.settings.collectAsStateWithLifecycle(initialValue = null)
    val language = settings?.language ?: "en"
    val data by produceState<CreditsData?>(initialValue = null) {
        value = CreditsData(container.credits.credits(), container.catalog.items().associateBy { it.id })
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.credits_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.cd_back))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            item {
                Text(
                    stringResource(R.string.credits_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp),
                )
            }
            items(data?.credits.orEmpty(), key = { it.id }) { credit ->
                val name = data?.items?.get(credit.id)?.name?.get(language) ?: credit.title
                Column(
                    Modifier.fillMaxWidth().clickable {
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(credit.url)))
                        } catch (e: ActivityNotFoundException) {
                            // No browser installed: nothing to open.
                        }
                    }.padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    Text(name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        stringResource(R.string.credits_photo_by, credit.author, credit.license),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                HorizontalDivider()
            }
        }
    }
}
