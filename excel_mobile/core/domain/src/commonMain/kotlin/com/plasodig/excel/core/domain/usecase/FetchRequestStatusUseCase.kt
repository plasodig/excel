package com.plasodig.excel.core.domain.usecase

import com.plasodig.excel.core.common.AppResult
import com.plasodig.excel.core.domain.model.RequestStatus
import com.plasodig.excel.core.domain.repository.ExcelRepository

class FetchRequestStatusUseCase(
    private val repository: ExcelRepository,
) {
    suspend operator fun invoke(id: String): AppResult<RequestStatus> =
        repository.fetchRequestStatus(id)
}
