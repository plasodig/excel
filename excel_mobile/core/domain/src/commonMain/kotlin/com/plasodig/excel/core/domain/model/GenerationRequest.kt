package com.plasodig.excel.core.domain.model

/**
 * Status auto-generate tutorial dari halaman pencarian mobile.
 * Lifecycle: Processing → Completed | Failed. AlreadyExists = short-circuit (tutorial sudah ada).
 */
enum class GenerationRequestStatus {
    Processing,
    Completed,
    Failed,
    AlreadyExists,  // slug sudah ada di katalog → langsung pakai excelId
    Unknown,
}

fun String.toGenerationRequestStatus(): GenerationRequestStatus = when (lowercase()) {
    "processing" -> GenerationRequestStatus.Processing
    "completed" -> GenerationRequestStatus.Completed
    "failed" -> GenerationRequestStatus.Failed
    "already_exists" -> GenerationRequestStatus.AlreadyExists
    else -> GenerationRequestStatus.Unknown
}

/**
 * Hasil submit request (POST /api/requests).
 *  - Processing: job baru ter-dispatch, mulai polling dengan requestId
 *  - AlreadyExists: tutorial sudah published di katalog, langsung pakai excelId
 */
data class SubmitRequestResult(
    val status: GenerationRequestStatus,
    val requestId: String?,
    val excelId: ExcelId?,
    val message: String,
)

/** Hasil polling status (GET /api/requests/:id). */
data class RequestStatus(
    val id: String,
    val status: GenerationRequestStatus,
    val query: String,
    val errorMessage: String?,
    val excelId: ExcelId?,
)
