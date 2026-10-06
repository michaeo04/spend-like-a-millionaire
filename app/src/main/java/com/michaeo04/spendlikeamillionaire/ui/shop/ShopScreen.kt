package com.michaeo04.spendlikeamillionaire.ui.shop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.michaeo04.spendlikeamillionaire.AppContainer
import com.michaeo04.spendlikeamillionaire.R
import com.michaeo04.spendlikeamillionaire.domain.Category
import com.michaeo04.spendlikeamillionaire.ui.components.BalanceBar
import com.michaeo04.spendlikeamillionaire.ui.components.ItemRow
import com.michaeo04.spendlikeamillionaire.ui.components.MoneyFormatter
import com.michaeo04.spendlikeamillionaire.ui.components.labelRes

@Composable
fun ShopRoute(container: AppContainer, onOpenCart: () -> Unit, onOpenSettings: () -> Unit) {
    val vm: ShopViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                ShopViewModel(
                    container.catalog, container.people, container.fx,
                    container.settingsStore, container.cartStore,
                )
            }
        },
    )
    val state by vm.state.collectAsStateWithLifecycle()
    ShopScreen(
        state = state,
        onQuery = vm::setQuery,
        onCategory = vm::setCategory,
        onSort = vm::setSort,
        onQuantity = vm::setQuantity,
        onDelta = vm::changeQuantity,
        onBuyMax = vm::buyMax,
        onOpenCart = onOpenCart,
        onOpenSettings = onOpenSettings,
    )
}

@Composable
fun ShopScreen(
    state: ShopUiState,
    onQuery: (String) -> Unit,
    onCategory: (Category?) -> Unit,
    onSort: (SortOrder) -> Unit,
    onQuantity: (String, Long) -> Unit,
    onDelta: (String, Long) -> Unit,
    onBuyMax: (String) -> Unit,
    onOpenCart: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val formatter = remember(state.currency, state.rate, state.language) {
        MoneyFormatter(state.currency, state.rate, state.language)
    }
    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            BalanceBar(
                person = state.person,
                balanceCents = state.balanceCents,
                spentCents = state.spentCents,
                percentSpent = state.percentSpent,
                formatter = formatter,
                onOpenSettings = onOpenSettings,
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Button(
                    onClick = onOpenCart,
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp),
                ) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = null)
                    Text(
                        if (state.cartLineCount > 0) {
                            stringResource(R.string.view_cart_count, state.cartLineCount)
                        } else {
                            stringResource(R.string.view_cart)
                        },
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        },
    ) { padding ->
        if (state.loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 12.dp, end = 12.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 8.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "filters") {
                Filters(state, onQuery, onCategory, onSort)
            }
            if (state.items.isEmpty()) {
                item(key = "empty") {
                    Text(
                        stringResource(R.string.shop_empty),
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                    )
                }
            }
            items(state.items, key = { it.item.id }) { ui ->
                ItemRow(
                    ui = ui,
                    formatter = formatter,
                    onQuantity = { onQuantity(ui.item.id, it) },
                    onDelta = { onDelta(ui.item.id, it) },
                    onBuyMax = { onBuyMax(ui.item.id) },
                )
            }
        }
    }
}

@Composable
private fun Filters(
    state: ShopUiState,
    onQuery: (String) -> Unit,
    onCategory: (Category?) -> Unit,
    onSort: (SortOrder) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = state.query,
            onValueChange = onQuery,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            placeholder = { Text(stringResource(R.string.shop_search_hint)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = state.sort == SortOrder.PRICE_ASC,
                onClick = { onSort(SortOrder.PRICE_ASC) },
                label = { Text(stringResource(R.string.sort_low_high)) },
            )
            FilterChip(
                selected = state.sort == SortOrder.PRICE_DESC,
                onClick = { onSort(SortOrder.PRICE_DESC) },
                label = { Text(stringResource(R.string.sort_high_low)) },
            )
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = state.category == null,
                    onClick = { onCategory(null) },
                    label = { Text(stringResource(R.string.category_all)) },
                )
            }
            items(Category.entries.toList()) { category ->
                FilterChip(
                    selected = state.category == category,
                    onClick = { onCategory(category) },
                    label = { Text(stringResource(category.labelRes())) },
                )
            }
        }
    }
}
