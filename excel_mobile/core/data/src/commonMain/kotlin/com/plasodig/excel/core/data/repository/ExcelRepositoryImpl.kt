package com.plasodig.excel.core.data.repository

import co.touchlab.kermit.Logger
import com.plasodig.excel.core.common.AppError
import com.plasodig.excel.core.common.AppResult
import com.plasodig.excel.core.data.config.ApiConfig
import com.plasodig.excel.core.data.mapper.toDomain
import com.plasodig.excel.core.data.mapper.toSummary
import com.plasodig.excel.core.data.remote.ExcelDetailDto
import com.plasodig.excel.core.data.remote.ExcelSummaryDto
import com.plasodig.excel.core.data.remote.RemoteExcelApi
import com.plasodig.excel.core.database.dao.RelatedFunctionInput
import com.plasodig.excel.core.database.dao.ExcelDao
import com.plasodig.excel.core.database.dao.StepInput
import com.plasodig.excel.core.domain.model.Difficulty
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
import com.plasodig.excel.core.domain.model.toGenerationRequestStatus
import com.plasodig.excel.core.domain.repository.ExcelRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * Konsumen API excel_dashboard + cache lokal SQLDelight (offline-first, server-paginated).
 *
 * Alur:
 * 1. `refreshFirstPage()` / `loadNextPage()` → fetch satu page dari API, upsert ke DB.
 * 2. `observeExcels(category, limit)` reaktif dari DB dengan LIMIT — UI selalu render
 *    dari cache, tidak pernah full-load ribuan row ke memory.
 * 3. `ensureDetailFetched(id)` → kalau relatedFunctions/steps kosong, fetch detail & isi DB.
 *    Dipanggil saat user buka detail screen (lazy).
 *
 * Tidak ada generate AI di sini — itu kerjaan dashboard.
 */
class ExcelRepositoryImpl(
    private val dao: ExcelDao,
    private val api: RemoteExcelApi,
    private val apiConfig: ApiConfig,
    private val clock: () -> Long = { currentTimeMillis() },
) : ExcelRepository {

    private val log = Logger.withTag("ExcelRepo")

    override fun observeExcels(category: ExcelCategory?, limit: Int): Flow<List<ExcelSummary>> =
        combine(
            dao.observeTop(category?.name, limit),
            dao.observeFavoriteIds(),
        ) { excels, favIds ->
            val favSet = favIds.toHashSet()
            excels.map { it.toSummary(isFavorite = favSet.contains(it.id)) }
        }

    override fun observeExcel(id: ExcelId): Flow<Excel?> =
        dao.observeById(id.value).map { entity ->
            entity?.let {
                val relatedFunctions = dao.relatedFunctionsOf(it.id)
                val steps = dao.stepsOf(it.id)
                it.toDomain(relatedFunctions, steps)
            }
        }

    override fun observeFavorites(): Flow<List<ExcelSummary>> =
        combine(dao.observeAll(), dao.observeFavoriteIds()) { excels, favIds ->
            val favSet = favIds.toHashSet()
            excels.filter { favSet.contains(it.id) }.map { it.toSummary(isFavorite = true) }
        }

    override fun search(query: String): Flow<List<ExcelSummary>> =
        combine(dao.search(query), dao.observeFavoriteIds()) { excels, favIds ->
            val favSet = favIds.toHashSet()
            excels.map { it.toSummary(isFavorite = favSet.contains(it.id)) }
        }

    override suspend fun refreshFirstPage(
        category: ExcelCategory?,
        pageSize: Int,
    ): AppResult<PageResult> = fetchPage(category, offset = 0, pageSize = pageSize)

    override suspend fun loadNextPage(
        category: ExcelCategory?,
        offset: Int,
        pageSize: Int,
    ): AppResult<PageResult> = fetchPage(category, offset = offset, pageSize = pageSize)

    private suspend fun fetchPage(
        category: ExcelCategory?,
        offset: Int,
        pageSize: Int,
    ): AppResult<PageResult> {
        val result = api.fetchManifest(
            category = category?.name,
            offset = offset,
            limit = pageSize,
        )
        return when (result) {
            is AppResult.Failure -> {
                log.w { "fetchPage gagal [offset=$offset limit=$pageSize cat=${category?.name}]: ${result.error.message}" }
                result
            }
            is AppResult.Success -> {
                val now = clock()
                result.data.excels.forEach { upsertSummary(it, now) }
                val received = result.data.excels.size
                val total = result.data.total
                val reachedEnd = when {
                    // Server honor pagination kontrak — trust total.
                    total > 0 -> offset + received >= total
                    // Server return lebih banyak dari yang diminta → dia tidak support pagination
                    // (misal dashboard versi lama). Anggap ini full manifest, stop paging
                    // supaya UI tidak infinite-loop LoadMore.
                    received > pageSize -> {
                        log.w {
                            "Server tidak honor limit=$pageSize (kirim $received items). " +
                                "Treat as full manifest — deploy dashboard terbaru untuk aktifkan paging."
                        }
                        true
                    }
                    // Last page normal.
                    received < pageSize -> true
                    // received == pageSize tanpa total — ambigu, teruskan (server lama yang
                    // kebetulan punya PAGE_SIZE tutorial per page).
                    else -> false
                }
                log.i { "page OK [offset=$offset received=$received total=$total end=$reachedEnd]" }
                AppResult.Success(
                    PageResult(
                        receivedCount = received,
                        total = total,
                        reachedEnd = reachedEnd,
                    )
                )
            }
        }
    }

    override suspend fun toggleFavorite(id: ExcelId): AppResult<Unit> = try {
        if (dao.isFavorite(id.value)) dao.removeFavorite(id.value) else dao.addFavorite(id.value, clock())
        AppResult.Success(Unit)
    } catch (t: Throwable) {
        AppResult.Failure(AppError.Database(t.message ?: "database error"))
    }

    override suspend fun ensureDetailFetched(id: ExcelId): AppResult<Unit> {
        val entity = dao.findById(id.value)
        val hasDetail = entity != null &&
            entity.description.isNotBlank() &&
            dao.relatedFunctionsOf(entity.id).isNotEmpty()
        if (hasDetail) return AppResult.Success(Unit)

        return when (val result = api.fetchDetail(id.value)) {
            is AppResult.Failure -> result
            is AppResult.Success -> {
                upsertDetail(result.data, clock())
                AppResult.Success(Unit)
            }
        }
    }

    // ---- Demand-driven generation ------------------------------------------

    override suspend fun fetchSuggestions(query: String): AppResult<List<ExcelSuggestion>> =
        when (val result = api.fetchSuggestions(query)) {
            is AppResult.Failure -> result
            is AppResult.Success -> AppResult.Success(
                result.data.suggestions.map { dto ->
                    ExcelSuggestion(
                        id = ExcelId(dto.id),
                        title = dto.title,
                        category = parseCategory(dto.category),
                        description = dto.description,
                        difficulty = parseDifficulty(dto.difficulty),
                        cookingTimeMinutes = dto.cookingTimeMinutes,
                        tags = dto.tags,
                        imageUrl = apiConfig.resolveImageUrl(dto.imageUrl),
                    )
                }
            )
        }

    override suspend fun submitGenerationRequest(query: String): AppResult<SubmitRequestResult> =
        mapSubmitResult(api.submitRequest(query))

    override suspend fun submitGenerationRequestExtra(query: String): AppResult<SubmitRequestResult> =
        mapSubmitResult(api.submitRequestExtra(query))

    private fun mapSubmitResult(
        result: AppResult<com.plasodig.excel.core.data.remote.SubmitRequestResponseDto>,
    ): AppResult<SubmitRequestResult> = when (result) {
        is AppResult.Failure -> result
        is AppResult.Success -> {
            val dto = result.data
            AppResult.Success(
                SubmitRequestResult(
                    status = dto.status.toGenerationRequestStatus(),
                    requestId = dto.requestId,
                    excelId = dto.excelId?.let { ExcelId(it) },
                    message = dto.message,
                )
            )
        }
    }

    override suspend fun fetchRequestStatus(id: String): AppResult<RequestStatus> =
        when (val result = api.fetchRequestStatus(id)) {
            is AppResult.Failure -> result
            is AppResult.Success -> AppResult.Success(
                RequestStatus(
                    id = result.data.id,
                    status = result.data.status.toGenerationRequestStatus(),
                    query = result.data.query,
                    errorMessage = result.data.errorMessage,
                    excelId = result.data.excelId?.let { ExcelId(it) },
                )
            )
        }

    override suspend fun reportExcel(
        id: ExcelId,
        reason: ReportReason,
        detail: String,
    ): AppResult<ReportResult> = when (val result = api.reportExcel(id.value, reason.apiValue, detail)) {
        is AppResult.Failure -> result
        is AppResult.Success -> AppResult.Success(
            ReportResult(
                reportCount = result.data.reportCount,
                unpublished = result.data.unpublished,
                message = result.data.message,
            )
        )
    }

    private fun parseCategory(name: String): ExcelCategory =
        runCatching { ExcelCategory.valueOf(name) }.getOrDefault(ExcelCategory.BasicFormula)

    private fun parseDifficulty(name: String): Difficulty =
        runCatching { Difficulty.valueOf(name) }.getOrDefault(Difficulty.Medium)

    // ---------- internal ----------

    private fun upsertSummary(dto: ExcelSummaryDto, now: Long) {
        // Preserve relatedFunctions/steps yang sudah ada di DB (kalau detail sudah pernah di-fetch).
        val existing = dao.findById(dto.id)
        val existingRelatedFunctions = if (existing != null) dao.relatedFunctionsOf(existing.id) else emptyList()
        val existingSteps = if (existing != null) dao.stepsOf(existing.id) else emptyList()

        dao.upsertFull(
            id = dto.id,
            title = dto.title,
            description = dto.description,
            imageUrl = apiConfig.resolveImageUrl(dto.imageUrl),
            category = dto.category,
            difficulty = dto.difficulty,
            cookingTimeMinutes = dto.cookingTimeMinutes,
            servings = dto.servings,
            tags = dto.tags.joinToString(","),
            updatedAtEpoch = now,
            relatedFunctions = existingRelatedFunctions.mapIndexed { idx, e ->
                RelatedFunctionInput(
                    name = e.name,
                    quantity = e.quantity,
                    unit = e.unit,
                    group = e.groupName,
                    orderIndex = idx,
                )
            },
            steps = existingSteps.map {
                StepInput(
                    order = it.stepOrder.toInt(),
                    instruction = it.instruction,
                    durationMinutes = it.durationMinutes?.toInt(),
                )
            },
        )
    }

    private fun upsertDetail(dto: ExcelDetailDto, now: Long) {
        dao.upsertFull(
            id = dto.id,
            title = dto.title,
            description = dto.description,
            imageUrl = apiConfig.resolveImageUrl(dto.imageUrl),
            category = dto.category,
            difficulty = dto.difficulty,
            cookingTimeMinutes = dto.cookingTimeMinutes,
            servings = dto.servings,
            tags = dto.tags.joinToString(","),
            updatedAtEpoch = now,
            relatedFunctions = dto.relatedFunctions.mapIndexed { idx, i ->
                RelatedFunctionInput(
                    name = i.name,
                    quantity = i.quantity,
                    unit = i.unit,
                    group = null,
                    orderIndex = idx,
                )
            },
            steps = dto.steps.map {
                StepInput(order = it.order, instruction = it.instruction, durationMinutes = null)
            },
        )
    }
}

internal expect fun currentTimeMillis(): Long
