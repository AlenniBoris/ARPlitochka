package com.example.arplitka.shared.ui.navigation

interface AppNavigator {
    fun openCatalog()
    fun openTile(id: Long)
    fun openAr()
    fun back()
}
