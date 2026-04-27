package com.plasodig.excel.core.domain.usecase

import com.plasodig.excel.core.common.AppResult
import com.plasodig.excel.core.domain.model.ExcelSuggestion
import com.plasodig.excel.core.domain.repository.ExcelRepository

class FetchSuggestionsUseCase(
    private val repository: ExcelRepository,
) {
    suspend operator fun invoke(query: String): AppResult<List<ExcelSuggestion>> =
        repository.fetchSuggestions(query)
}
