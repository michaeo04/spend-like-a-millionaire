package com.michaeo04.spendlikeamillionaire.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.michaeo04.spendlikeamillionaire.AppContainer
import com.michaeo04.spendlikeamillionaire.R
import com.michaeo04.spendlikeamillionaire.domain.Money
import com.michaeo04.spendlikeamillionaire.ui.components.Avatar
import com.michaeo04.spendlikeamillionaire.ui.components.MoneyFormatter
import java.util.Currency
import java.util.Locale

@Composable
fun OnboardingRoute(
    container: AppContainer,
    deviceLanguage: String,
    onLanguageChosen: (String) -> Unit,
    onDone: () -> Unit,
) {
    val vm: OnboardingViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                OnboardingViewModel(container.people, container.fx, container.settingsStore, deviceLanguage)
            }
        },
    )
    val state by vm.state.collectAsStateWithLifecycle()
    OnboardingScreen(
        state = state,
        onLanguage = { vm.setLanguage(it); onLanguageChosen(it) },
        onCurrency = vm::setCurrency,
        onPerson = vm::setPerson,
        onNext = vm::next,
        onBack = vm::back,
        onFinish = { vm.finish(onDone) },
    )
}

@Composable
fun OnboardingScreen(
    state: OnboardingUiState,
    onLanguage: (String) -> Unit,
    onCurrency: (String) -> Unit,
    onPerson: (String) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onFinish: () -> Unit,
) {
    BackHandler(enabled = state.step > 0, onBack = onBack)
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(20.dp)) {
        LinearProgressIndicator(
            progress = { (state.step + 1f) / (ONBOARDING_LAST_STEP + 1) },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            stringResource(R.string.onb_step, state.step + 1, ONBOARDING_LAST_STEP + 1),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            stringResource(
                when (state.step) {
                    0 -> R.string.onb_language_title
                    1 -> R.string.onb_currency_title
                    else -> R.string.onb_person_title
                },
            ),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 16.dp),
        )
        Column(Modifier.weight(1f)) {
            if (state.loading) {
                CircularProgressIndicator()
            } else {
                when (state.step) {
                    0 -> LanguageStep(state.language, onLanguage)
                    1 -> CurrencyStep(state, onCurrency)
                    else -> PersonStep(state, onPerson)
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (state.step > 0) {
                OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.onb_back))
                }
            }
            if (state.step < ONBOARDING_LAST_STEP) {
                Button(onClick = onNext, enabled = !state.loading, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.onb_next))
                }
            } else {
                Button(onClick = onFinish, enabled = state.canFinish, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.onb_finish))
                }
            }
        }
    }
}

@Composable
private fun LanguageStep(selected: String, onSelect: (String) -> Unit) {
    Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        listOf("en" to "English", "vi" to "Tiếng Việt").forEach { (code, label) ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (code == selected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                ),
                modifier = Modifier.fillMaxWidth().selectable(
                    selected = code == selected,
                    role = Role.RadioButton,
                    onClick = { onSelect(code) },
                ),
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = code == selected, onClick = null)
                    Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 12.dp))
                }
            }
        }
    }
}

@Composable
private fun CurrencyStep(state: OnboardingUiState, onSelect: (String) -> Unit) {
    val locale = remember(state.language) { Locale.forLanguageTag(state.language) }
    Column {
        Text(
            stringResource(R.string.onb_currency_hint),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        LazyColumn(Modifier.selectableGroup()) {
            items(state.currencies, key = { it }) { code ->
                Row(
                    Modifier.fillMaxWidth().selectable(
                        selected = code == state.currency,
                        role = Role.RadioButton,
                        onClick = { onSelect(code) },
                    ).padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = code == state.currency, onClick = null)
                    Text(
                        "$code · ${Currency.getInstance(code).getDisplayName(locale)}",
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun PersonStep(state: OnboardingUiState, onSelect: (String) -> Unit) {
    val formatter = remember(state.currency, state.rates, state.language) {
        MoneyFormatter(state.currency, state.rates[state.currency], state.language)
    }
    LazyColumn(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(state.people, key = { it.id }) { person ->
            val selected = person.id == state.personId
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (selected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                ),
                modifier = Modifier.fillMaxWidth().selectable(
                    selected = selected,
                    role = Role.RadioButton,
                    onClick = { onSelect(person.id) },
                ),
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Avatar(person.name.get(state.language), person.avatarColor, size = 44.dp)
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(
                            person.name.get(state.language),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            "${person.source} · ${formatter.month(person.asOf)}",
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                    Text(
                        formatter.money(Money.usdToCents(person.netWorthUsd)),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}
