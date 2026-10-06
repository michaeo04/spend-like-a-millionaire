package com.michaeo04.spendlikeamillionaire.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.michaeo04.spendlikeamillionaire.R
import com.michaeo04.spendlikeamillionaire.domain.Formatting
import com.michaeo04.spendlikeamillionaire.ui.shop.ItemUi
import java.text.NumberFormat

/** Shop grid cell: big photo, name, price and a compact +/- stepper (tap the number to type or use MAX). */
@Composable
fun ItemCard(
    ui: ItemUi,
    formatter: MoneyFormatter,
    onDelta: (Long) -> Unit,
    onEditQuantity: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val name = ui.item.name.get(formatter.language)
    val price = formatter.money(ui.item.priceCents)
    val estimateHint = stringResource(R.string.item_estimate_hint)
    Card(modifier.fillMaxWidth()) {
        Column {
            Box {
                ItemImage(
                    icon = ui.item.icon,
                    image = ui.item.image,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1.2f),
                    emojiSize = 64.sp,
                )
                if (ui.item.estimate) {
                    Surface(
                        shape = RoundedCornerShape(topEnd = 10.dp, bottomEnd = 10.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.padding(top = 8.dp).align(Alignment.TopStart),
                    ) {
                        Text(
                            "≈",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
            }
            Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                Text(
                    name,
                    style = MaterialTheme.typography.titleSmall,
                    minLines = 2,
                    maxLines = 2,
                )
                Text(
                    price,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    modifier = Modifier.semantics {
                        if (ui.item.estimate) contentDescription = "$price, $estimateHint"
                    },
                )
                Row(
                    Modifier.fillMaxWidth().padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    IconButton(onClick = { onDelta(-1) }, enabled = ui.quantity > 0, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.Remove, contentDescription = stringResource(R.string.cd_decrease, name))
                    }
                    val quantityLabel = stringResource(R.string.cd_quantity, name)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(onClick = onEditQuantity)
                            .semantics {
                                contentDescription = "$quantityLabel: ${ui.quantity}"
                            },
                    ) {
                        Text(
                            Formatting.compactCount(ui.quantity, formatter.locale),
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    }
                    IconButton(
                        onClick = { onDelta(1) },
                        enabled = ui.quantity < ui.maxQuantity,
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.cd_increase, name))
                    }
                }
            }
        }
    }
}

/** Type an exact quantity or jump to the most the balance affords. Out-of-range input is clamped by the ViewModel. */
@Composable
fun QuantityDialog(
    ui: ItemUi,
    formatter: MoneyFormatter,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(if (ui.quantity == 0L) "" else ui.quantity.toString()) }
    val numbers = remember(formatter.locale) { NumberFormat.getIntegerInstance(formatter.locale) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(ui.item.name.get(formatter.language)) },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.filter(Char::isDigit).take(18) },
                    singleLine = true,
                    placeholder = { Text("0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    stringResource(R.string.qty_max_hint, numbers.format(ui.maxQuantity)),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text.toLongOrNull() ?: 0L) }) { Text(stringResource(R.string.action_ok)) }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { text = ui.maxQuantity.toString() }) { Text(stringResource(R.string.item_buy_max)) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
            }
        },
    )
}
