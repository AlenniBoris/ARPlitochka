package com.example.arplitka.shared.ui.core.format.atomic

fun formatPrice(price: Double): String {
    return if (price == price.toLong().toDouble()) {
        price.toLong().toString()
    } else {
        price.toString()
    }
}
