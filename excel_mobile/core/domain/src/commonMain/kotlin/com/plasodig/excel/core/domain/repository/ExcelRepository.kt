package com.plasodig.excel.core.domain.repository

import com.plasodig.excel.core.common.AppResult
import com.plasodig.excel.core.domain.model.PageResult
import com.plasodig.excel.core.domain.model.Excel
import com.plasodig.excel.core.domain.model.ExcelCategory
import com.plasodig.excel.core.domain.model.ExcelId
import com.plasodig.excel.core.domain.model.ExcelSuggestion
import com.plasodig.excel.core.domain.model.ExcelSummary
import com.plasodig.excel.core.domain.model.ReportReason
import com.plasodig.excel.core.domain.model.ReportResult
import com.plasodig.excel.core.domain.model.RequestStatus
import com.plasodig.excel.core.domain.model.SubmitRequestResult
import kotlinx.coroutines.flow.Flow

/**
 * Sumber kebenaran konten tutorial. Konten di-pre-generate di dashboard admin
 * (proyek `excel_dashboard/`); mobile app hanya konsumsi via API + cache lokal SQLDelight.
 *
 * **Pagination**: daftar tutorial diambil page-by-page (server-paginated) supaya skalabel
 * sampai ribuan tutorial. UI subscribe ke `observeExcels(category, limit)` — Flow dari
 * cache lokal dengan LIMIT, lalu trigger `loadNextPage()` saat scroll mendekati akhir.
 */
interface ExcelRepository {
    /**
     * Observe top-N tutorial dari cache lokal. Flow emit setiap kali DB berubah (upsert dari
     * `loadNextPage`/`refreshFirstPage` atau favorite toggle). `limit` dikontrol UI — saat
     * scroll, UI re-subscribe dengan limit yang lebih besar.
     */
    fun observeExcels(category: ExcelCategory? = null, limit: Int): Flow<List<ExcelSummary>>

    fun observeExcel(id: ExcelId): Flow<Excel?>
    fun observeFavorites(): Flow<List<ExcelSummary>>
    fun search(query: String): Flow<List<ExcelSummary>>

    /**
     * Fetch page pertama dari server + upsert ke DB. Dipanggil saat startup atau
     * pull-to-refresh. Idempotent — row yang sudah ada di-overwrite (preserve detail
     * relatedFunctions/steps kalau sudah pernah di-fetch).
     */
    suspend fun refreshFirstPage(
        category: ExcelCategory? = null,
        pageSize: Int,
    ): AppResult<PageResult>

    /**
     * Fetch page berikutnya mulai dari `offset` + upsert ke DB. Dipanggil saat user scroll
     * mendekati akhir daftar. Caller harus guard supaya tidak dipanggil saat sudah `reachedEnd`.
     */
    suspend fun loadNextPage(
        category: ExcelCategory? = null,
        offset: Int,
        pageSize: Int,
    ): AppResult<PageResult>

    suspend fun toggleFavorite(id: ExcelId): AppResult<Unit>

    /**
     * Pastikan detail (relatedFunctions + steps) untuk satu tutorial sudah ada di DB. Kalau belum,
     * fetch dari API. Idempotent — kalau sudah lengkap, no-op.
     */
    suspend fun ensureDetailFetched(id: ExcelId): AppResult<Unit>

    // ---- Demand-driven generation (halaman pencarian) -----------------------

    /** Saran tutorial yang mirip untuk query miss. Zero-AI di server, gratis. */
    suspend fun fetchSuggestions(query: String): AppResult<List<ExcelSuggestion>>

    /** Minta dashboard generate tutorial baru berdasarkan query user (rate-limited 5/jam/IP). */
    suspend fun submitGenerationRequest(query: String): AppResult<SubmitRequestResult>

    /**
     * Extra request setelah user tonton rewarded ad (escape hatch saat rate limit).
     * Bucket rate limit terpisah (10/jam/IP) — caller bertanggung jawab validate reward.
     */
    suspend fun submitGenerationRequestExtra(query: String): AppResult<SubmitRequestResult>

    /** Poll status permintaan yang sudah di-submit. */
    suspend fun fetchRequestStatus(id: String): AppResult<RequestStatus>

    /**
     * Laporkan tutorial ke admin. Required by Google Play Generative AI + UGC policy.
     * Server akan auto-unpublish kalau count >= threshold.
     */
    suspend fun reportExcel(
        id: ExcelId,
        reason: ReportReason,
        detail: String,
    ): AppResult<ReportResult>
}
