package com.example.arplitka.shared.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.ui.graphics.vector.ImageVector

data class BottomBarModelUi(
    val onClick: () -> Unit,
    val icon: ImageVector,
    val text: String,
    val route: String
)

enum class BottomBarValues {
    Catalog,
    AR
}

const val CATALOG_ROUTE = "catalog"
const val AR_ROUTE = "ar"
const val TRANSITION_ROUTE = "transition"

val BOTTOM_BAR_ROUTES = listOf(CATALOG_ROUTE)

fun BottomBarValues.toModelUi(
    onClick: () -> Unit = {}
): BottomBarModelUi = when (this) {
    BottomBarValues.Catalog ->
        BottomBarModelUi(
            onClick = onClick,
            icon = Icons.Default.GridView,
            text = "Каталог",
            route = CATALOG_ROUTE
        )

    BottomBarValues.AR ->
        BottomBarModelUi(
            onClick = onClick,
            icon = Icons.Default.ViewInAr,
            text = "AR",
            route = AR_ROUTE
        )
}
