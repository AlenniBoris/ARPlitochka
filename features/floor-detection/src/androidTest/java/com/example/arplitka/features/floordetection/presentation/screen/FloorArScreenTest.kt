package com.example.arplitka.features.floordetection.presentation.screen

import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.arplitka.features.floordetection.domain.model.ArInstruction
import com.example.arplitka.features.floordetection.domain.model.ArStatus
import com.example.arplitka.features.floordetection.domain.model.FloorUiState
import com.example.arplitka.features.floordetection.presentation.viewmodel.FloorArViewModel
import com.kaspersky.components.composesupport.config.withComposeSupport
import com.kaspersky.kaspresso.kaspresso.Kaspresso
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

class FloorArScreenTest : TestCase(
    kaspressoBuilder = Kaspresso.Builder.withComposeSupport()
) {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val viewModel: FloorArViewModel = mockk(relaxed = true)
    private val uiStateFlow = MutableStateFlow(FloorUiState())

    @Test
    fun floorArScreen_displaysInitialStatus() = run {
        every { viewModel.uiState } returns uiStateFlow

        step("Set up FloorArScreen") {
            composeTestRule.setContent {
                FloorArScreen(viewModel = viewModel)
            }
        }

        step("Check if initial status is displayed") {
            onComposeScreen<FloorArScreenKaspresso>(composeTestRule) {
                // Since we use mappers, we check for the text mapped from INITIALIZATION
                // status_searching = "Наведите камеру на пол"
                // please_wait = "Пожалуйста, подождите"
                statusText("Пожалуйста, подождите") {
                    assertIsDisplayed()
                }
            }
        }
    }

    @Test
    fun floorArScreen_updatesStatusWhenFloorDetected() = run {
        every { viewModel.uiState } returns uiStateFlow

        step("Set up FloorArScreen") {
            composeTestRule.setContent {
                FloorArScreen(viewModel = viewModel)
            }
        }

        step("Update state to Floor Detected") {
            uiStateFlow.value = FloorUiState(
                status = ArStatus.FLOOR_DETECTED,
                instruction = ArInstruction.DETECTED
            )
        }

        step("Check if updated status is displayed") {
            onComposeScreen<FloorArScreenKaspresso>(composeTestRule) {
                // status_candidate = "Пол обнаружен"
                statusText("Пол обнаружен") {
                    assertIsDisplayed()
                }
            }
        }
    }
}

class FloorArScreenKaspresso(composeTestRule: ComposeTestRule) : 
    ComposeScreen<FloorArScreenKaspresso>(
        composeTestRule = composeTestRule
    ) {
    
    fun statusText(text: String, block: KNode.() -> Unit) = child<KNode> {
        hasText(text)
    }.apply(block)
    
    val addPointButton: KNode = child {
        hasText("Поставить точку")
    }
}
