package com.plasodig.excel.core.domain.usecase

import com.plasodig.excel.core.common.AppResult
import com.plasodig.excel.core.domain.model.ExcelId
import com.plasodig.excel.core.domain.model.ReportReason
import com.plasodig.excel.core.domain.model.ReportResult
import com.plasodig.excel.core.domain.repository.ExcelRepository

class ReportExcelUseCase(
    private val repository: ExcelRepository,
) {
    suspend operator fun invoke(
        id: ExcelId,
        reason: ReportReason,
        detail: String = "",
    ): AppResult<ReportResult> = repository.reportExcel(id, reason, detail)
}
