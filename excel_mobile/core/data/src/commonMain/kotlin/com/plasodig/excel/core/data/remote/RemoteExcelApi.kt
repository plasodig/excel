package com.plasodig.excel.core.data.remote

import com.plasodig.excel.core.common.AppError
import com.plasodig.excel.core.common.AppResult
import com.plasodig.excel.core.data.config.ApiConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Klien HTTP untuk endpoint publik excel_dashboard:
 * - GET /api/excels        → manifest semua tutorial ter-publish
 * - GET /api/excels/:id    → detail satu tutorial (relatedFunctions + steps)
 *
 * Tidak ada autentikasi — endpoint memang publik. Mobile app read-only.
 */
class RemoteExcelApi(private val config: ApiConfig) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val client = HttpClient {
        install(ContentNegotiation) { json(json) }
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = 30_000
        }
        expectSuccess = false
    }

    fun close() = client.close()

    /**
     * Fetch page manifest. `category` null = semua kategori. `limit` & `offset` menghasilkan
     * pagination server-side (dashboard clamp limit max 100). Response mencakup field `total`
     * supaya caller bisa tentukan `reachedEnd` secara eksplisit, sekalipun mix dengan cache DB.
     */
    suspend fun fetchManifest(
        category: String? = null,
        offset: Int = 0,
        limit: Int = 10,
    ): AppResult<ExcelManifestDto> = try {
        val res = client.get(config.excelsUrl(category, offset, limit))
        decode(res) { it.body<ExcelManifestDto>() }
    } catch (t: Throwable) {
        AppResult.Failure(toError(t, "manifest"))
    }

    suspend fun fetchDetail(id: String): AppResult<ExcelDetailDto> = try {
        val res = client.get(config.excelDetailUrl(id))
        if (res.status == HttpStatusCode.NotFound) {
            AppResult.Failure(AppError.NotFound("Tutorial $id tidak tersedia"))
        } else {
            decode(res) { it.body<ExcelDetailDto>() }
        }
    } catch (t: Throwable) {
        AppResult.Failure(toError(t, "detail $id"))
    }

    /**
     * Fetch saran tutorial yang mirip dengan query user. Tidak ada AI call di server —
     * server-side cuma SQL LIKE + ranking heuristik, gratis dan instant (<100ms).
     */
    suspend fun fetchSuggestions(query: String): AppResult<SuggestionsResponseDto> = try {
        val res = client.post(config.suggestionsUrl) {
            contentType(ContentType.Application.Json)
            setBody(QueryBodyDto(query))
        }
        decode(res) { it.body<SuggestionsResponseDto>() }
    } catch (t: Throwable) {
        AppResult.Failure(toError(t, "suggestions"))
    }

    /**
     * Submit permintaan tutorial baru (demand-driven). Server langsung dispatch background job
     * AI generation. Endpoint kena rate limit 5/jam/IP → kalau exceeded, return 429.
     */
    suspend fun submitRequest(query: String): AppResult<SubmitRequestResponseDto> = try {
        val res = client.post(config.requestsUrl) {
            contentType(ContentType.Application.Json)
            setBody(QueryBodyDto(query))
        }
        decode(res) { it.body<SubmitRequestResponseDto>() }
    } catch (t: Throwable) {
        AppResult.Failure(toError(t, "submit request"))
    }

    /**
     * Submit extra request setelah user tonton rewarded ad. Endpoint ini pakai bucket rate
     * limit terpisah (10/jam/IP) — bypass 5/jam normal. Mobile hanya panggil kalau user
     * sudah terbukti tonton rewarded sampai selesai (RewardedOutcome.Granted).
     */
    suspend fun submitRequestExtra(query: String): AppResult<SubmitRequestResponseDto> = try {
        val res = client.post(config.requestsExtraUrl) {
            contentType(ContentType.Application.Json)
            setBody(QueryBodyDto(query))
        }
        decode(res) { it.body<SubmitRequestResponseDto>() }
    } catch (t: Throwable) {
        AppResult.Failure(toError(t, "submit request extra"))
    }

    suspend fun fetchRequestStatus(id: String): AppResult<RequestStatusDto> = try {
        val res = client.get(config.requestStatusUrl(id))
        if (res.status == HttpStatusCode.NotFound) {
            AppResult.Failure(AppError.NotFound("Permintaan $id tidak ada"))
        } else {
            decode(res) { it.body<RequestStatusDto>() }
        }
    } catch (t: Throwable) {
        AppResult.Failure(toError(t, "request status $id"))
    }

    /**
     * Report tutorial yang inaccurate/offensive/dangerous ke server.
     * Required by Google Play Generative AI + UGC policy (in-app reporting mechanism).
     */
    suspend fun reportExcel(
        excelId: String,
        reason: String,
        detail: String = "",
    ): AppResult<ReportResponseDto> = try {
        val res = client.post(config.reportExcelUrl(excelId)) {
            contentType(ContentType.Application.Json)
            setBody(ReportBodyDto(reason = reason, detail = detail))
        }
        if (res.status == HttpStatusCode.NotFound) {
            AppResult.Failure(AppError.NotFound("Tutorial $excelId tidak ada"))
        } else {
            decode(res) { it.body<ReportResponseDto>() }
        }
    } catch (t: Throwable) {
        AppResult.Failure(toError(t, "report $excelId"))
    }

    private suspend inline fun <T> decode(
        res: HttpResponse,
        crossinline parse: suspend (HttpResponse) -> T,
    ): AppResult<T> {
        if (res.status.value == 429) {
            val retry = res.headers["Retry-After"]?.toIntOrNull() ?: 3600
            return AppResult.Failure(AppError.RateLimited(retryAfterSeconds = retry))
        }
        if (!res.status.isSuccess()) {
            val body = runCatching { res.bodyAsText() }.getOrDefault("")
            return AppResult.Failure(AppError.Network("HTTP ${res.status.value}: ${body.take(200)}"))
        }
        return AppResult.Success(parse(res))
    }

    private fun toError(t: Throwable, where: String): AppError {
        val msg = t.message ?: t::class.simpleName ?: "unknown"
        return AppError.Network("Gagal fetch $where: $msg")
    }
}

@Serializable
data class ExcelManifestDto(
    val version: Long = 0L,
    /** Total tutorial published di server untuk query ini (semua category kalau null). */
    val total: Int = 0,
    val limit: Int = 0,
    val offset: Int = 0,
    /** Jumlah item di field `excels` — bisa < limit kalau di ekor page. */
    val count: Int = 0,
    val excels: List<ExcelSummaryDto> = emptyList(),
)

@Serializable
data class ExcelSummaryDto(
    val id: String,
    val title: String,
    val category: String,
    val description: String = "",
    val difficulty: String = "Medium",
    val cookingTimeMinutes: Int = 0,
    val servings: Int = 0,
    val tags: List<String> = emptyList(),
    val imageUrl: String? = null,
    val updatedAt: Long = 0L,
    val publishedAt: Long? = null,
)

@Serializable
data class ExcelDetailDto(
    val id: String,
    val title: String,
    val category: String,
    val description: String = "",
    val difficulty: String = "Medium",
    val cookingTimeMinutes: Int = 0,
    val servings: Int = 0,
    val tags: List<String> = emptyList(),
    val imageUrl: String? = null,
    @SerialName("ingredients")
    val relatedFunctions: List<RelatedFunctionDto> = emptyList(),
    val steps: List<StepDto> = emptyList(),
    val publishedAt: Long? = null,
)

@Serializable
data class RelatedFunctionDto(
    val name: String,
    val quantity: String = "",
    val unit: String = "",
)

@Serializable
data class StepDto(
    val order: Int,
    val instruction: String,
)

@Serializable
data class QueryBodyDto(val query: String)

@Serializable
data class SuggestionsResponseDto(
    val success: Boolean = false,
    val query: String = "",
    val count: Int = 0,
    val suggestions: List<SuggestionDto> = emptyList(),
)

@Serializable
data class SuggestionDto(
    val id: String,
    val title: String,
    val category: String,
    val description: String = "",
    val difficulty: String = "Medium",
    val cookingTimeMinutes: Int = 0,
    val tags: List<String> = emptyList(),
    val imageUrl: String? = null,
)

/**
 * Response /api/requests (submit). Server bisa balas 3 varian:
 * - status = "queued" → requestId diisi, requestStatus = "pending"
 * - status = "already_requested" → sudah ada pending/approved, requestId + status lama
 * - status = "already_exists" → slug sudah ada di katalog, excelId diisi
 */
@Serializable
data class SubmitRequestResponseDto(
    val success: Boolean = false,
    val status: String = "",
    val requestId: String? = null,
    val requestStatus: String? = null,
    val excelId: String? = null,
    val message: String = "",
)

@Serializable
data class ReportBodyDto(val reason: String, val detail: String = "")

@Serializable
data class ReportResponseDto(
    val success: Boolean = false,
    val reportCount: Int = 0,
    val unpublished: Boolean = false,
    val message: String = "",
)

/** Response GET /api/requests/:id — status polling. */
@Serializable
data class RequestStatusDto(
    val success: Boolean = false,
    val id: String = "",
    val status: String = "",
    val query: String = "",
    val slugTarget: String = "",
    val requestedAt: Long = 0L,
    val completedAt: Long? = null,
    val errorMessage: String? = null,
    val excelId: String? = null,
)
