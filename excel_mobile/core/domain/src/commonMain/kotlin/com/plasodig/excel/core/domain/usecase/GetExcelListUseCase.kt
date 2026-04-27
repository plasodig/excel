package com.plasodig.excel.core.domain.usecase

import com.plasodig.excel.core.domain.model.ExcelCategory
import com.plasodig.excel.core.domain.model.ExcelSummary
import com.plasodig.excel.core.domain.repository.ExcelRepository
import kotlinx.coroutines.flow.Flow

class GetExcelListUseCase(
    private val repository: ExcelRepository,
) {
    operator fun invoke(
        category: ExcelCategory? = null,
        limit: Int,
    ): Flow<List<ExcelSummary>> = repository.observeExcels(category, limit)
}
