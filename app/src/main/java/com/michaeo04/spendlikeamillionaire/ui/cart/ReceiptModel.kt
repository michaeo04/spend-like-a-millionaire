package com.michaeo04.spendlikeamillionaire.ui.cart

import com.michaeo04.spendlikeamillionaire.ui.components.MoneyFormatter
import java.text.NumberFormat

/** Already-localized texts for the receipt image. */
data class ReceiptStrings(
    val title: String,
    val totalLabel: String,
    val remainingLabel: String,
    val percentLabel: String,
    val footer: String,
    val more: (Int) -> String,
)

data class ReceiptLine(val label: String, val amount: String)

/** Everything the share image needs, free of Android types so it is unit-testable. */
data class ReceiptModel(
    val title: String,
    val personName: String,
    val lines: List<ReceiptLine>,
    val moreCount: Int,
    val moreText: String,
    val total: String,
    val remaining: String,
    val percent: String,
    val totalLabel: String,
    val remainingLabel: String,
    val percentLabel: String,
    val footer: String,
)

fun buildReceipt(state: CartUiState, strings: ReceiptStrings, maxLines: Int = 8): ReceiptModel {
    val formatter = MoneyFormatter(state.currency, state.rate, state.language)
    val numbers = NumberFormat.getIntegerInstance(formatter.locale)
    val shown = state.lines.take(maxLines)
    val more = state.lines.size - shown.size
    return ReceiptModel(
        title = strings.title,
        personName = state.person?.name?.get(state.language).orEmpty(),
        lines = shown.map {
            ReceiptLine(
                label = "${it.item.icon} ${it.item.name.get(state.language)} × ${numbers.format(it.quantity)}",
                amount = formatter.money(it.lineTotalCents),
            )
        },
        moreCount = more,
        moreText = if (more > 0) strings.more(more) else "",
        total = formatter.money(state.totalCents),
        remaining = formatter.money(state.remainingCents),
        percent = formatter.percent(state.percentSpent),
        totalLabel = strings.totalLabel,
        remainingLabel = strings.remainingLabel,
        percentLabel = strings.percentLabel,
        footer = strings.footer,
    )
}
