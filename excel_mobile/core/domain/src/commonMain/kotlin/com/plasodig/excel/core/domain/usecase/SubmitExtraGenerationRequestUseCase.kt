package com.plasodig.excel.core.domain.usecase

import com.plasodig.excel.core.common.AppResult
import com.plasodig.excel.core.domain.model.SubmitRequestResult
import com.plasodig.excel.core.domain.repository.ExcelRepository

/**
 * Submit generate request via jalur "extra" — bypass rate limit normal. Dipanggil
 * HANYA setelah user tonton rewarded ad sampai selesai (escape hatch saat kena limit).
 */
class SubmitExtraGenerationRequestUseCase(
    private val repository: ExcelRepository,
) {
    suspend operator fun invoke(query: String): AppResult<SubmitRequestResult> =
        repository.submitGenerationRequestExtra(query)
}
