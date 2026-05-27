package com.example.arplitka.features.catalog.presentation.screen

import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import com.kaspersky.components.composesupport.config.withComposeSupport
import com.kaspersky.kaspresso.kaspresso.Kaspresso
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import org.junit.Rule
import org.junit.Test

class CatalogScreenTest : TestCase(
    kaspressoBuilder = Kaspresso.Builder.withComposeSupport()
) {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun catalogScreen_displaysPlaceholderText() = run {
        step("Set up CatalogScreen") {
            composeTestRule.setContent {
                CatalogScreen()
            }
        }

        step("Check if placeholder text is displayed") {
            onComposeScreen<CatalogScreenKaspresso>(composeTestRule) {
                placeholderText {
                    assertIsDisplayed()
                    assertTextEquals("Экран Каталога (Заглушка)")
                }
            }
        }
    }
}

class CatalogScreenKaspresso(composeTestRule: ComposeTestRule) : 
    ComposeScreen<CatalogScreenKaspresso>(
        composeTestRule = composeTestRule
    ) {
    val placeholderText: KNode = child {
        hasText("Экран Каталога (Заглушка)")
    }
}
