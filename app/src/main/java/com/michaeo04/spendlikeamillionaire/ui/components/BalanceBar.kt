package com.michaeo04.spendlikeamillionaire.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.michaeo04.spendlikeamillionaire.R
import com.michaeo04.spendlikeamillionaire.domain.Person

/** Sticky header: whose fortune, what is left, and how much has been spent. */
@Composable
fun BalanceBar(
    person: Person?,
    balanceCents: Long,
    spentCents: Long,
    percentSpent: Double,
    formatter: MoneyFormatter,
    onOpenSettings: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.statusBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (person != null) {
                    Avatar(person.name.get(formatter.language), person.avatarColor, size = 40.dp, image = person.image)
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(
                            person.name.get(formatter.language),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            stringResource(
                                R.string.balance_source,
                                person.source,
                                formatter.month(person.asOf),
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                } else {
                    Column(Modifier.weight(1f)) {}
                }
                if (onOpenSettings != null) {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.cd_settings))
                    }
                }
            }
            Text(
                stringResource(R.string.balance_remaining),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                formatter.money((balanceCents - spentCents).coerceAtLeast(0)),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
            )
            LinearProgressIndicator(
                progress = { (percentSpent / 100.0).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    stringResource(R.string.balance_spent_percent, formatter.percent(percentSpent)),
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(
                    stringResource(R.string.balance_of_total, formatter.money(balanceCents)),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}
