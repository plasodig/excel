package com.plasodig.excel.core.domain.usecase

import com.plasodig.excel.core.common.AppResult
import com.plasodig.excel.core.domain.model.ExcelId
import com.plasodig.excel.core.domain.repository.ExcelRepository

class ToggleFavoriteUseCase(
    private val repository: ExcelRepository,
) {
    suspend operator fun invoke(id: ExcelId): AppResult<Unit> = repository.toggleFavorite(id)
}
