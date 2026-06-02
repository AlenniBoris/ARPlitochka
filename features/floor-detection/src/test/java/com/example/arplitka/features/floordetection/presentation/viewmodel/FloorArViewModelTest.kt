package com.example.arplitka.features.floordetection.presentation.viewmodel

import androidx.compose.ui.unit.IntSize
import app.cash.turbine.test
import com.example.arplitka.features.floordetection.domain.model.ArFrameResult
import com.example.arplitka.features.floordetection.domain.model.ArInstruction
import com.example.arplitka.features.floordetection.domain.model.ArStatus
import com.example.arplitka.features.floordetection.domain.model.FloorDetectionState
import com.example.arplitka.features.floordetection.domain.model.TextureRotation
import com.example.arplitka.features.floordetection.domain.model.TileType
import com.example.arplitka.features.floordetection.domain.usecase.ProcessArFrameUseCase
import com.google.ar.core.Frame
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FloorArViewModelTest {

    private val processArFrameUseCase: ProcessArFrameUseCase = mockk()
    private lateinit var viewModel: FloorArViewModel
    
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = FloorArViewModel(processArFrameUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is correct`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(ArStatus.INITIALIZATION, state.status)
            assertEquals(ArInstruction.PLEASE_WAIT, state.instruction)
            assertEquals(TileType.PAVING_STONES_V1, state.selectedTileType)
            assertEquals(TextureRotation.DEGREES_0, state.textureRotation)
        }
    }

    @Test
    fun `onSessionUpdated updates state correctly when floor detected`() = runTest {
        val session: Session = mockk()
        val frame: Frame = mockk()
        val viewportSize = IntSize(1080, 1920)
        
        val frameResult = ArFrameResult(
            trackingState = TrackingState.TRACKING,
            isFloorDetected = true,
            horizontalPlaneCount = 1,
            selectedArea = 1.5f,
            hasCenterHit = true,
            hitPose = mockk(),
            hitResult = mockk(),
            isDepthEnabled = true
        )

        every { processArFrameUseCase(session, frame, viewportSize) } returns frameResult

        viewModel.onSessionUpdated(session, frame, viewportSize)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(FloorDetectionState.CandidateFound, state.detectionState)
            assertEquals(ArStatus.FLOOR_DETECTED, state.status)
            assertEquals(ArInstruction.DETECTED, state.instruction)
            assertEquals(1.5f, state.selectedArea)
        }
    }

    fun `changeTileType keeps selection when contour is open`() = runTest {
        viewModel.changeTileType()
        assertEquals(TileType.MODERN, viewModel.uiState.value.selectedTileType)
    }
}
