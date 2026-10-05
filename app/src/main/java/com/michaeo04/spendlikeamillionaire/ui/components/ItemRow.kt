package com.michaeo04.spendlikeamillionaire.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.michaeo04.spendlikeamillionaire.R
import com.michaeo04.spendlikeamillionaire.ui.shop.ItemUi

@Composable
fun ItemRow(
    ui: ItemUi,
    formatter: MoneyFormatter,
    onQuantity: (Long) -> Unit,
    onBuyMax: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val name = ui.item.name.get(formatter.language)
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(ui.item.icon, fontSize = 32.sp, modifier = Modifier.padding(end = 12.dp))
                Column(Modifier.weight(1f)) {
                    Text(name, style = MaterialTheme.typography.titleMedium)
                    val price = formatter.money(ui.item.priceCents)
                    val estimateHint = stringResource(R.string.item_estimate_hint)
                    Text(
                        if (ui.item.estimate) "≈ $price" else price,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.semantics {
                            if (ui.item.estimate) contentDescription = "$price, $estimateHint"
                        },
                    )
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = { onQuantity(ui.quantity - 1) },
                    enabled = ui.quantity > 0,
                ) {
                    Icon(Icons.Default.Remove, contentDescription = stringResource(R.string.cd_decrease, name))
                }
                QuantityField(
                    quantity = ui.quantity,
                    label = stringResource(R.string.cd_quantity, name),
                    onChange = onQuantity,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = { onQuantity(ui.quantity + 1) },
                    enabled = ui.quantity < ui.maxQuantity,
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.cd_increase, name))
                }
                TextButton(onClick = onBuyMax, enabled = ui.quantity < ui.maxQuantity) {
                    Text(stringResource(R.string.item_buy_max))
                }
            }
        }
    }
}

@Composable
private fun QuantityField(
    quantity: Long,
    label: String,
    onChange: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Re-keyed on the committed quantity so a clamped value replaces what the user typed.
    var text by remember(quantity) { mutableStateOf(if (quantity == 0L) "" else quantity.toString()) }
    OutlinedTextField(
        value = text,
        onValueChange = { raw ->
            val digits = raw.filter(Char::isDigit).take(18)
            text = digits
            onChange(digits.toLongOrNull() ?: 0L)
        },
        placeholder = { Text("0", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center),
        modifier = modifier.semantics { contentDescription = label },
    )
}
