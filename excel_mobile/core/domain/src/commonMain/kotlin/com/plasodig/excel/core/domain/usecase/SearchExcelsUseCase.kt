package com.plasodig.excel.core.domain.usecase

import com.plasodig.excel.core.domain.model.ExcelSummary
import com.plasodig.excel.core.domain.repository.ExcelRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class SearchExcelsUseCase(
    private val repository: ExcelRepository,
) {
    operator fun invoke(query: String): Flow<List<ExcelSummary>> {
        val trimmed = query.trim()
        return if (trimmed.isBlank()) flowOf(emptyList()) else repository.search(trimmed)
    }
}
