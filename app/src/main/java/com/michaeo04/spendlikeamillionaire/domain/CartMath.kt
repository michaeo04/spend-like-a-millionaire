package com.michaeo04.spendlikeamillionaire.domain

/** Cart contents: item id -> quantity. Immutable. */
data class Cart(val lines: Map<String, Long> = emptyMap())

object CartMath {
    /** price * quantity, saturating at [Long.MAX_VALUE] so corrupt stored data can never crash. */
    fun lineCost(item: Item, quantity: Long): Long =
        try {
            Money.lineTotal(item.priceCents, quantity)
        } catch (e: ArithmeticException) {
            Long.MAX_VALUE
        }

    /** Saturating sum of price * quantity for known items with a positive quantity. */
    fun total(cart: Cart, items: Map<String, Item>): Long {
        var sum = 0L
        for ((id, qty) in cart.lines) {
            val item = items[id] ?: continue
            if (qty <= 0) continue
            val cost = lineCost(item, qty)
            sum = if (sum > Long.MAX_VALUE - cost) Long.MAX_VALUE else sum + cost
        }
        return sum
    }

    /**
     * Sets [item]'s quantity, clamped to what the balance still affords after the other lines.
     * A resulting quantity of 0 removes the line.
     */
    fun setQuantity(
        cart: Cart,
        item: Item,
        requested: Long,
        balanceCents: Long,
        items: Map<String, Item>,
    ): Cart {
        val others = Cart(cart.lines - item.id)
        val available = balanceCents - total(others, items)
        val quantity = Money.clampQuantity(requested, Money.maxQuantity(available, item.priceCents))
        return if (quantity == 0L) others else Cart(cart.lines + (item.id to quantity))
    }

    /**
     * Makes a stored cart safe: drops unknown ids and non-positive quantities, then trims lines in
     * order so the total never exceeds [balanceCents].
     */
    fun sanitize(cart: Cart, items: Map<String, Item>, balanceCents: Long): Cart {
        var remaining = balanceCents
        val kept = LinkedHashMap<String, Long>()
        for ((id, qty) in cart.lines) {
            val item = items[id] ?: continue
            if (qty <= 0) continue
            val affordable = minOf(qty, Money.maxQuantity(remaining, item.priceCents))
            if (affordable <= 0) continue
            kept[id] = affordable
            remaining -= Money.lineTotal(item.priceCents, affordable)
        }
        return Cart(kept)
    }
}
