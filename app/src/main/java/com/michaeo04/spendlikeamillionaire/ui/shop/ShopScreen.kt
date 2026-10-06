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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items as rowItems
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.michaeo04.spendlikeamillionaire.ui.components.ItemCard
import com.michaeo04.spendlikeamillionaire.ui.components.QuantityDialog
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
    var editingId by remember { mutableStateOf<String?>(null) }
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
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 156.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 12.dp, end = 12.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 8.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "filters", span = { GridItemSpan(maxLineSpan) }) {
                Filters(state, onQuery, onCategory, onSort)
            }
            if (state.items.isEmpty()) {
                item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        stringResource(R.string.shop_empty),
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                    )
                }
            }
            items(state.items, key = { it.item.id }) { ui ->
                ItemCard(
                    ui = ui,
                    formatter = formatter,
                    onDelta = { onDelta(ui.item.id, it) },
                    onEditQuantity = { editingId = ui.item.id },
                )
            }
        }
    }

    // Look the item up again so the dialog always shows the latest max after other changes.
    state.items.firstOrNull { it.item.id == editingId }?.let { editing ->
        QuantityDialog(
            ui = editing,
            formatter = formatter,
            onConfirm = { onQuantity(editing.item.id, it); editingId = null },
            onDismiss = { editingId = null },
        )
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
            rowItems(Category.entries.toList()) { category ->
                FilterChip(
                    selected = state.category == category,
                    onClick = { onCategory(category) },
                    label = { Text(stringResource(category.labelRes())) },
                )
            }
        }
    }
}
