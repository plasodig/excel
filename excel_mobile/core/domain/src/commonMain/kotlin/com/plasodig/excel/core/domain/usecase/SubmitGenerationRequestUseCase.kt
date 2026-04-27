package com.plasodig.excel.core.domain.usecase

import com.plasodig.excel.core.common.AppResult
import com.plasodig.excel.core.domain.model.SubmitRequestResult
import com.plasodig.excel.core.domain.repository.ExcelRepository

class SubmitGenerationRequestUseCase(
    private val repository: ExcelRepository,
) {
    suspend operator fun invoke(query: String): AppResult<SubmitRequestResult> =
        repository.submitGenerationRequest(query)
}
