package com.plasodig.excel.core.domain.usecase

import com.plasodig.excel.core.common.AppResult
import com.plasodig.excel.core.domain.model.PageResult
import com.plasodig.excel.core.domain.model.ExcelCategory
import com.plasodig.excel.core.domain.repository.ExcelRepository

/**
 * Refresh = fetch page pertama. Digunakan saat startup dan pull-to-refresh.
 * UI me-reset loadedCount ke pageSize setelah ini berhasil.
 */
class RefreshExcelsUseCase(
    private val repository: ExcelRepository,
) {
    suspend operator fun invoke(
        category: ExcelCategory? = null,
        pageSize: Int,
    ): AppResult<PageResult> = repository.refreshFirstPage(category, pageSize)
}
