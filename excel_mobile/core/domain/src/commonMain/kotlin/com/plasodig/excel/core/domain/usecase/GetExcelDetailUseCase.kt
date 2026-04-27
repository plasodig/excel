package com.plasodig.excel.core.domain.usecase

import com.plasodig.excel.core.domain.model.Excel
import com.plasodig.excel.core.domain.model.ExcelId
import com.plasodig.excel.core.domain.repository.ExcelRepository
import kotlinx.coroutines.flow.Flow

class GetExcelDetailUseCase(
    private val repository: ExcelRepository,
) {
    operator fun invoke(id: ExcelId): Flow<Excel?> = repository.observeExcel(id)
}
