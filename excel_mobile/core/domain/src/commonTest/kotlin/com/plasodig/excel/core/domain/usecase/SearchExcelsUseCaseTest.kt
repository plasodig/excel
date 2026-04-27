package com.plasodig.excel.core.domain.usecase

import com.plasodig.excel.core.common.AppResult
import com.plasodig.excel.core.domain.model.Difficulty
import com.plasodig.excel.core.domain.model.Excel
import com.plasodig.excel.core.domain.model.ExcelCategory
import com.plasodig.excel.core.domain.model.ExcelId
import com.plasodig.excel.core.domain.model.ExcelSummary
import com.plasodig.excel.core.domain.repository.ExcelRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SearchExcelsUseCaseTest {

    @Test
    fun `blank query emits empty list without hitting repository`() = runTest {
        var called = false
        val repo = object : FakeRepo() {
            override fun search(query: String): Flow<List<ExcelSummary>> {
                called = true
                return flowOf(emptyList())
            }
        }
        val result = SearchExcelsUseCase(repo).invoke("   ").collectFirst()
        assertTrue(result.isEmpty())
        assertEquals(false, called)
    }

    @Test
    fun `non-blank query delegates to repository`() = runTest {
        val dummy = ExcelSummary(
            id = ExcelId("x"),
            title = "Test",
            description = "",
            imageUrl = "",
            category = ExcelCategory.BasicFormula,
            difficulty = Difficulty.Easy,
            cookingTimeMinutes = 10,
        )
        val repo = object : FakeRepo() {
            override fun search(query: String) = flowOf(listOf(dummy))
        }
        val result = SearchExcelsUseCase(repo).invoke("test").collectFirst()
        assertEquals(listOf(dummy), result)
    }

    private suspend fun <T> Flow<T>.collectFirst(): T {
        var value: T? = null
        collect {
            value = it
            return@collect
        }
        @Suppress("UNCHECKED_CAST")
        return value as T
    }

    abstract class FakeRepo : ExcelRepository {
        override fun observeExcels(category: ExcelCategory?) = flowOf(emptyList<ExcelSummary>())
        override fun observeExcel(id: ExcelId): Flow<Excel?> = flowOf(null)
        override fun observeFavorites() = flowOf(emptyList<ExcelSummary>())
        override fun search(query: String) = flowOf(emptyList<ExcelSummary>())
        override suspend fun refresh() = AppResult.Success(Unit)
        override suspend fun toggleFavorite(id: ExcelId) = AppResult.Success(Unit)
        override suspend fun ensureDetailFetched(id: ExcelId) = AppResult.Success(Unit)
    }
}
