package com.michaeo04.spendlikeamillionaire.ui.onboarding

import android.app.Activity
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.michaeo04.spendlikeamillionaire.AppContainer
import com.michaeo04.spendlikeamillionaire.R
import com.michaeo04.spendlikeamillionaire.domain.Formatting
import com.michaeo04.spendlikeamillionaire.domain.Money
import com.michaeo04.spendlikeamillionaire.ui.components.Avatar
import com.michaeo04.spendlikeamillionaire.ui.components.ItemImage
import com.michaeo04.spendlikeamillionaire.ui.components.MoneyFormatter
import kotlinx.coroutines.delay
import java.util.Currency
import java.util.Locale

private val GradientTop = Color(0xFF0A3524)
private val GradientBottom = Color(0xFF1B6B4A)
private val Gold = Color(0xFFF5C542)

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
                OnboardingViewModel(
                    container.catalog, container.people, container.fx, container.settingsStore, deviceLanguage,
                )
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
    LightStatusBarIconsOffWhileVisible()
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(GradientTop, GradientBottom)))) {
        AnimatedContent(
            targetState = state.step,
            transitionSpec = {
                val forward = targetState > initialState
                (fadeIn(tween(250)) + slideInHorizontally(tween(300)) { if (forward) it / 4 else -it / 4 }) togetherWith
                    (fadeOut(tween(150)) + slideOutHorizontally(tween(300)) { if (forward) -it / 4 else it / 4 })
            },
            label = "onboarding-step",
        ) { step ->
            if (step == 0) {
                WelcomeStep(state, onNext)
            } else {
                SheetStep(state, step, onLanguage, onCurrency, onPerson, onNext, onBack, onFinish)
            }
        }
    }
}

/** The onboarding header is dark green, so status-bar icons must be light; restored when leaving. */
@Composable
private fun LightStatusBarIconsOffWhileVisible() {
    val view = LocalView.current
    DisposableEffect(view) {
        val activity = generateSequence(view.context) { (it as? ContextWrapper)?.baseContext }
            .filterIsInstance<Activity>().firstOrNull()
        val controller = activity?.let { WindowCompat.getInsetsController(it.window, view) }
        val previous = controller?.isAppearanceLightStatusBars
        controller?.isAppearanceLightStatusBars = false
        onDispose { if (controller != null && previous != null) controller.isAppearanceLightStatusBars = previous }
    }
}

// ---------------------------------------------------------------- welcome

@Composable
private fun WelcomeStep(state: OnboardingUiState, onStart: () -> Unit) {
    val pulse = rememberInfiniteTransition(label = "coin")
    val coinScale by pulse.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(1600), RepeatMode.Reverse),
        label = "coin-scale",
    )
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.size(190.dp).scale(coinScale),
        )
        Text(
            stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Text(
            stringResource(R.string.onb_welcome_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.82f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp, start = 32.dp, end = 32.dp),
        )
        Spacer(Modifier.weight(1f))
        Marquee(state)
        Spacer(Modifier.weight(0.6f))
        Button(
            onClick = onStart,
            colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color(0xFF12372A)),
            modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth().height(58.dp),
        ) {
            Text(stringResource(R.string.onb_get_started), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/** Slowly scrolling strip of showcase items, from a Big Mac to a space station. */
@Composable
private fun Marquee(state: OnboardingUiState) {
    val items = state.featured
    if (items.isEmpty()) return
    val listState = rememberLazyListState()
    LaunchedEffect(items) {
        while (true) {
            listState.scrollBy(1.4f)
            delay(16)
        }
    }
    LazyRow(
        state = listState,
        userScrollEnabled = false,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth().height(96.dp),
    ) {
        items(count = Int.MAX_VALUE) { index ->
            val item = items[index % items.size]
            ItemImage(
                icon = item.icon,
                image = item.image,
                modifier = Modifier.size(96.dp).clip(RoundedCornerShape(20.dp)),
                emojiSize = 40.sp,
            )
        }
    }
}

// ---------------------------------------------------------------- steps 1-3

@Composable
private fun SheetStep(
    state: OnboardingUiState,
    step: Int,
    onLanguage: (String) -> Unit,
    onCurrency: (String) -> Unit,
    onPerson: (String) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onFinish: () -> Unit,
) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Column(Modifier.padding(horizontal = 24.dp).padding(top = 20.dp, bottom = 20.dp)) {
            StepDots(step, ONBOARDING_LAST_STEP)
            Text(
                stringResource(
                    when (step) {
                        1 -> R.string.onb_language_title
                        2 -> R.string.onb_currency_title
                        else -> R.string.onb_person_title
                    },
                ),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                modifier = Modifier.padding(top = 14.dp),
            )
            if (step == 2) {
                Text(
                    stringResource(R.string.onb_currency_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
        Surface(
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) {
            Column(Modifier.navigationBarsPadding().padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 12.dp)) {
                Box(Modifier.weight(1f)) {
                    if (state.loading) {
                        CircularProgressIndicator(Modifier.align(Alignment.Center))
                    } else {
                        when (step) {
                            1 -> LanguageStep(state.language, onLanguage)
                            2 -> CurrencyStep(state, onCurrency)
                            else -> PersonStep(state, onPerson)
                        }
                    }
                }
                Row(
                    Modifier.fillMaxWidth().padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    TextButton(onClick = onBack, modifier = Modifier.height(54.dp)) {
                        Text(stringResource(R.string.onb_back), fontSize = 16.sp)
                    }
                    Button(
                        onClick = if (step < ONBOARDING_LAST_STEP) onNext else onFinish,
                        enabled = if (step < ONBOARDING_LAST_STEP) !state.loading else state.canFinish,
                        modifier = Modifier.weight(1f).height(54.dp),
                    ) {
                        Text(
                            stringResource(if (step < ONBOARDING_LAST_STEP) R.string.onb_next else R.string.onb_finish),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

/** Dots for the three choice steps (the welcome screen is not counted). */
@Composable
private fun StepDots(step: Int, total: Int) {
    val description = stringResource(R.string.onb_step, step, total)
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.semantics { contentDescription = description },
    ) {
        repeat(total) { index ->
            val active = index + 1 <= step
            val width by animateFloatAsState(if (index + 1 == step) 28f else 10f, label = "dot-width")
            Box(
                Modifier
                    .size(width = width.dp, height = 10.dp)
                    .clip(CircleShape)
                    .background(if (active) Gold else Color.White.copy(alpha = 0.3f)),
            )
        }
    }
}

/** Shared look for the big selectable cards: scale + colored border + check badge when selected. */
@Composable
private fun ChoiceCard(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val scale by animateFloatAsState(if (selected) 1.02f else 1f, label = "card-scale")
    val border by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        label = "card-border",
    )
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(if (selected) 2.dp else 1.dp, border),
        shadowElevation = if (selected) 4.dp else 0.dp,
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
    ) {
        Box {
            content()
            if (selected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(22.dp),
                )
            }
        }
    }
}

@Composable
private fun LanguageStep(selected: String, onSelect: (String) -> Unit) {
    Column(Modifier.selectableGroup().padding(horizontal = 4.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        listOf(
            Triple("en", "🇬🇧", "English"),
            Triple("vi", "🇻🇳", "Tiếng Việt"),
        ).forEach { (code, flag, label) ->
            ChoiceCard(selected = code == selected, onClick = { onSelect(code) }, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(flag, fontSize = 44.sp)
                    Text(
                        label,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun CurrencyStep(state: OnboardingUiState, onSelect: (String) -> Unit) {
    val locale = remember(state.language) { Locale.forLanguageTag(state.language) }
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp),
        modifier = Modifier.selectableGroup(),
    ) {
        gridItems(state.currencies, key = { it }) { code ->
            val currency = Currency.getInstance(code)
            ChoiceCard(selected = code == state.currency, onClick = { onSelect(code) }, modifier = Modifier.fillMaxWidth()) {
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 14.dp, horizontal = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        currency.getSymbol(locale),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                    )
                    Text(code, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        currency.getDisplayName(locale),
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        minLines = 2,
                        maxLines = 2,
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
    LazyColumn(
        Modifier.selectableGroup(),
        contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(state.people, key = { it.id }) { person ->
            val cents = Money.usdToCents(person.netWorthUsd)
            ChoiceCard(
                selected = person.id == state.personId,
                onClick = { onSelect(person.id) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Avatar(person.name.get(state.language), person.avatarColor, size = 56.dp)
                    Column(Modifier.weight(1f).padding(start = 14.dp, end = 12.dp)) {
                        Text(
                            person.name.get(state.language),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "${person.source} · ${formatter.month(person.asOf)}",
                            style = MaterialTheme.typography.labelSmall,
                        )
                        state.bigMacCents?.takeIf { it > 0 }?.let { bigMac ->
                            Text(
                                stringResource(R.string.onb_fun_fact, Formatting.compactCount(cents / bigMac, formatter.locale)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                    Text(
                        formatter.money(cents),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.End,
                    )
                }
            }
        }
    }
}
