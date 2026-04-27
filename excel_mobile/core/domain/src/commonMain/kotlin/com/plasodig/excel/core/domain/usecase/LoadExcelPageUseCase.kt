package com.plasodig.excel.core.domain.usecase

import com.plasodig.excel.core.common.AppResult
import com.plasodig.excel.core.domain.model.PageResult
import com.plasodig.excel.core.domain.model.ExcelCategory
import com.plasodig.excel.core.domain.repository.ExcelRepository

/**
 * Fetch page berikutnya (offset > 0). Dipanggil saat user scroll mendekati akhir daftar.
 * Caller harus guard: jangan panggil saat hasil sebelumnya `reachedEnd = true`.
 */
class LoadExcelPageUseCase(
    private val repository: ExcelRepository,
) {
    suspend operator fun invoke(
        category: ExcelCategory? = null,
        offset: Int,
        pageSize: Int,
    ): AppResult<PageResult> = repository.loadNextPage(category, offset, pageSize)
}
