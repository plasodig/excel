package com.plasodig.excel.core.domain.usecase

import com.plasodig.excel.core.common.AppResult
import com.plasodig.excel.core.domain.model.ExcelId
import com.plasodig.excel.core.domain.repository.ExcelRepository

/**
 * Pastikan detail (relatedFunctions + steps) sudah ada di cache lokal. Kalau belum, fetch dari API.
 * Dipanggil saat user buka layar detail (lazy load).
 */
class EnsureExcelDetailUseCase(
    private val repository: ExcelRepository,
) {
    suspend operator fun invoke(id: ExcelId): AppResult<Unit> = repository.ensureDetailFetched(id)
}
