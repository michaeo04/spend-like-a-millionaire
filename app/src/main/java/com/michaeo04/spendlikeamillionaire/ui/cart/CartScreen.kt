package com.michaeo04.spendlikeamillionaire.ui.cart

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.michaeo04.spendlikeamillionaire.AppContainer
import com.michaeo04.spendlikeamillionaire.R
import com.michaeo04.spendlikeamillionaire.platform.ShareReceipt
import com.michaeo04.spendlikeamillionaire.ui.components.MoneyFormatter
import java.text.NumberFormat

@Composable
fun CartRoute(container: AppContainer, onBack: () -> Unit) {
    val vm: CartViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                CartViewModel(
                    container.catalog, container.people, container.fx,
                    container.settingsStore, container.cartStore,
                )
            }
        },
    )
    val state by vm.state.collectAsStateWithLifecycle()
    CartScreen(state, onBack, onClear = vm::clearCart, onRemove = vm::removeLine)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    state: CartUiState,
    onBack: () -> Unit,
    onClear: () -> Unit,
    onRemove: (String) -> Unit,
) {
    val context = LocalContext.current
    val formatter = remember(state.currency, state.rate, state.language) {
        MoneyFormatter(state.currency, state.rate, state.language)
    }
    val numbers = remember(formatter.locale) { NumberFormat.getIntegerInstance(formatter.locale) }
    var confirmClear by remember { mutableStateOf(false) }

    val receiptStrings = ReceiptStrings(
        title = stringResource(R.string.receipt_title),
        totalLabel = stringResource(R.string.cart_total),
        remainingLabel = stringResource(R.string.cart_remaining),
        percentLabel = stringResource(R.string.cart_percent),
        footer = stringResource(R.string.receipt_footer),
        more = { n -> context.getString(R.string.receipt_more, n) },
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.cart_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.cd_back))
                    }
                },
            )
        },
        bottomBar = {
            if (state.lines.isNotEmpty()) {
                Surface(shadowElevation = 8.dp) {
                    Column(
                        Modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = { ShareReceipt.share(context, buildReceipt(state, receiptStrings)) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null)
                            Text(stringResource(R.string.share_receipt), Modifier.padding(start = 8.dp))
                        }
                        OutlinedButton(onClick = { confirmClear = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.clear_cart))
                        }
                    }
                }
            }
        },
    ) { padding ->
        if (state.lines.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                if (!state.loading) {
                    Text(
                        stringResource(R.string.cart_empty),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 8.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(state.lines, key = { it.item.id }) { line ->
                val name = line.item.name.get(state.language)
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(line.item.icon, fontSize = 28.sp, modifier = Modifier.padding(end = 12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(name, style = MaterialTheme.typography.titleSmall)
                            Text(
                                stringResource(
                                    R.string.cart_line,
                                    formatter.money(line.item.priceCents),
                                    numbers.format(line.quantity),
                                ),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Text(
                            formatter.money(line.lineTotalCents),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        IconButton(onClick = { onRemove(line.item.id) }) {
                            Icon(Icons.Default.Close, stringResource(R.string.cd_remove_line, name))
                        }
                    }
                }
            }
            item(key = "summary") {
                Column(Modifier.padding(top = 8.dp)) {
                    HorizontalDivider(Modifier.padding(bottom = 12.dp))
                    SummaryRow(stringResource(R.string.cart_total), formatter.money(state.totalCents), big = true)
                    SummaryRow(stringResource(R.string.cart_remaining), formatter.money(state.remainingCents))
                    SummaryRow(stringResource(R.string.cart_percent), formatter.percent(state.percentSpent))
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(R.string.clear_cart_title)) },
            text = { Text(stringResource(R.string.clear_cart_body)) },
            confirmButton = {
                TextButton(onClick = { confirmClear = false; onClear() }) {
                    Text(stringResource(R.string.action_clear))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun SummaryRow(label: String, value: String, big: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = if (big) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge)
        Text(
            value,
            style = if (big) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge,
            fontWeight = if (big) FontWeight.Bold else FontWeight.Normal,
        )
    }
}
