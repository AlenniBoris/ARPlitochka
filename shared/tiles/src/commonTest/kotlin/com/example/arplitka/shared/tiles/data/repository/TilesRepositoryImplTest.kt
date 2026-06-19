package com.example.arplitka.shared.tiles.data.repository

import com.example.arplitka.shared.core.domain.model.CustomResultModelDomain
import com.example.arplitka.shared.core.domain.model.CommonException
import com.example.arplitka.shared.tiles.data.remote.TilesApi
import com.example.arplitka.shared.tiles.data.remote.dto.TilesResponseDto
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class TilesRepositoryImplTest {

    private class FakeTilesApi(val shouldFail: Boolean = false) : TilesApi {
        override suspend fun getTiles(): TilesResponseDto {
            if (shouldFail) throw RuntimeException("Network error")
            return TilesResponseDto(items = emptyList())
        }
    }

    @Test
    fun `getTiles returns Success when api is successful`() = runTest {
        val repository = TilesRepositoryImpl(FakeTilesApi(shouldFail = false))
        
        val result = repository.getTiles()
        
        assertTrue(result is CustomResultModelDomain.Success)
        assertEquals(0, result.result.size)
    }

    @Test
    fun `getTiles returns Error when api fails`() = runTest {
        val repository = TilesRepositoryImpl(FakeTilesApi(shouldFail = true))
        
        val result = repository.getTiles()
        
        assertTrue(result is CustomResultModelDomain.Error)
        assertEquals(CommonException.Unknown, result.exception)
    }
}
