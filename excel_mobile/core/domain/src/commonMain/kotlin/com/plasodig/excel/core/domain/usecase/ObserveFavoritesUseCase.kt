package com.plasodig.excel.core.domain.usecase

import com.plasodig.excel.core.domain.model.ExcelSummary
import com.plasodig.excel.core.domain.repository.ExcelRepository
import kotlinx.coroutines.flow.Flow

class ObserveFavoritesUseCase(
    private val repository: ExcelRepository,
) {
    operator fun invoke(): Flow<List<ExcelSummary>> = repository.observeFavorites()
}
