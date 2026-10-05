package com.michaeo04.spendlikeamillionaire.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.michaeo04.spendlikeamillionaire.AppContainer
import com.michaeo04.spendlikeamillionaire.BuildConfig
import com.michaeo04.spendlikeamillionaire.R
import com.michaeo04.spendlikeamillionaire.ui.components.MoneyFormatter
import java.util.Currency
import java.util.Locale

const val PRIVACY_POLICY_URL = "https://michaeo04.github.io/spend-like-a-millionaire/privacy-policy"

private enum class Picker { LANGUAGE, CURRENCY, PERSON }

@Composable
fun SettingsRoute(container: AppContainer, onLanguageChosen: (String) -> Unit, onBack: () -> Unit) {
    val vm: SettingsViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                SettingsViewModel(
                    container.catalog, container.people, container.fx,
                    container.settingsStore, container.cartStore,
                )
            }
        },
    )
    val state by vm.state.collectAsStateWithLifecycle()
    SettingsScreen(
        state = state,
        onBack = onBack,
        onLanguage = { vm.setLanguage(it); onLanguageChosen(it) },
        onCurrency = vm::setCurrency,
        onPerson = vm::requestPersonChange,
        onConfirmSwitch = vm::confirmPersonChange,
        onCancelSwitch = vm::cancelPersonChange,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onBack: () -> Unit,
    onLanguage: (String) -> Unit,
    onCurrency: (String) -> Unit,
    onPerson: (String) -> Unit,
    onConfirmSwitch: () -> Unit,
    onCancelSwitch: () -> Unit,
) {
    val context = LocalContext.current
    val locale = remember(state.language) { Locale.forLanguageTag(state.language) }
    var picker by remember { mutableStateOf<Picker?>(null) }
    val formatter = remember(state.currency, state.rates, state.language) {
        MoneyFormatter(state.currency, state.rates[state.currency], state.language)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.cd_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            SettingRow(
                stringResource(R.string.settings_language),
                if (state.language == "vi") "Tiếng Việt" else "English",
            ) { picker = Picker.LANGUAGE }
            SettingRow(
                stringResource(R.string.settings_currency),
                "${state.currency} · ${Currency.getInstance(state.currency).getDisplayName(locale)}",
            ) { picker = Picker.CURRENCY }
            SettingRow(
                stringResource(R.string.settings_person),
                state.person?.name?.get(state.language).orEmpty(),
            ) { picker = Picker.PERSON }
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Text(
                stringResource(R.string.settings_about),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            Text(
                stringResource(R.string.settings_disclaimer),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            SettingRow(stringResource(R.string.settings_privacy), null) {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL)))
            }
            Text(
                stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(16.dp),
            )
        }
    }

    when (picker) {
        Picker.LANGUAGE -> PickerDialog(
            title = stringResource(R.string.settings_language),
            options = listOf("en" to "English", "vi" to "Tiếng Việt"),
            selected = state.language,
            onSelect = { onLanguage(it); picker = null },
            onDismiss = { picker = null },
        )
        Picker.CURRENCY -> PickerDialog(
            title = stringResource(R.string.settings_currency),
            options = state.currencies.map { it to "$it · ${Currency.getInstance(it).getDisplayName(locale)}" },
            selected = state.currency,
            onSelect = { onCurrency(it); picker = null },
            onDismiss = { picker = null },
        )
        Picker.PERSON -> PickerDialog(
            title = stringResource(R.string.settings_person),
            options = state.people.map { it.id to it.name.get(state.language) },
            selected = state.person?.id,
            onSelect = { onPerson(it); picker = null },
            onDismiss = { picker = null },
        )
        null -> Unit
    }

    state.pendingPerson?.let { target ->
        AlertDialog(
            onDismissRequest = onCancelSwitch,
            title = { Text(stringResource(R.string.switch_person_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.switch_person_body,
                        formatter.money(state.cartTotalCents),
                        target.name.get(state.language),
                    ),
                )
            },
            confirmButton = { TextButton(onClick = onConfirmSwitch) { Text(stringResource(R.string.action_switch)) } },
            dismissButton = { TextButton(onClick = onCancelSwitch) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun SettingRow(label: String, value: String?, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(label) },
        supportingContent = value?.let { { Text(it) } },
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    )
}

@Composable
private fun PickerDialog(
    title: String,
    options: List<Pair<String, String>>,
    selected: String?,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn(Modifier.selectableGroup()) {
                items(options, key = { it.first }) { (id, label) ->
                    androidx.compose.foundation.layout.Row(
                        Modifier.fillMaxWidth().selectable(
                            selected = id == selected,
                            role = Role.RadioButton,
                            onClick = { onSelect(id) },
                        ).padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = id == selected, onClick = null)
                        Text(label, Modifier.padding(start = 12.dp))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) } },
    )
}
