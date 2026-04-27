package com.plasodig.excel.feature.search

import co.touchlab.kermit.Logger
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import com.plasodig.excel.core.ads.RewardedAdController
import com.plasodig.excel.core.ads.RewardedOutcome
import com.plasodig.excel.core.common.AppError
import com.plasodig.excel.core.common.AppResult
import com.plasodig.excel.core.domain.model.GenerationRequestStatus
import com.plasodig.excel.core.domain.model.ExcelId
import com.plasodig.excel.core.domain.model.ExcelSuggestion
import com.plasodig.excel.core.domain.model.ExcelSummary
import com.plasodig.excel.core.domain.model.SubmitRequestResult
import com.plasodig.excel.core.domain.usecase.FetchRequestStatusUseCase
import com.plasodig.excel.core.domain.usecase.FetchSuggestionsUseCase
import com.plasodig.excel.core.domain.usecase.SearchExcelsUseCase
import com.plasodig.excel.core.domain.usecase.SubmitExtraGenerationRequestUseCase
import com.plasodig.excel.core.domain.usecase.SubmitGenerationRequestUseCase
import com.plasodig.excel.core.domain.usecase.ToggleFavoriteUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

/** Polling interval untuk status request yang sedang processing. */
private const val POLL_INTERVAL_MS = 3_000L

/** Safety cap: kalau 10 menit belum selesai, anggap stuck → tampil error. */
private const val POLL_TIMEOUT_MS = 10 * 60 * 1000L

data class SearchState(
    val query: String = "",
    val isSearching: Boolean = false,
    val results: List<ExcelSummary> = emptyList(),
    val error: String? = null,
    /** Saran tutorial similar saat hasil lokal kosong. */
    val suggestions: List<ExcelSuggestion> = emptyList(),
    val isFetchingSuggestions: Boolean = false,
    /** Status generate-job saat user tekan tombol "Buat tutorial ini". */
    val generate: GenerateJob? = null,
    /** Dialog rate limit muncul saat user kena limit normal — escape hatch via rewarded ad. */
    val rateLimitDialog: RateLimitDialogState? = null,
    /** One-shot snackbar untuk error rewarded/ad load/etc. */
    val toast: String? = null,
) {
    val isEmpty: Boolean
        get() = query.isNotBlank() && !isSearching && results.isEmpty() && error == null
}

/** State dialog "Kamu sudah kena rate limit — tunggu atau tonton iklan?". */
data class RateLimitDialogState(
    val retryAfterSeconds: Int,
    val isWatchingAd: Boolean = false,
)

/** Snapshot state job auto-generate yang dipoll. */
data class GenerateJob(
    val requestId: String,
    val status: GenerationRequestStatus,
    val message: String,
    val excelId: ExcelId? = null,
    val errorMessage: String? = null,
)

sealed interface SearchIntent {
    data class QueryChanged(val value: String) : SearchIntent
    data object Clear : SearchIntent
    data class ItemClicked(val id: ExcelId) : SearchIntent
    data class ToggleFavorite(val id: ExcelId) : SearchIntent
    data object GenerateExcel : SearchIntent
    data object DismissGenerate : SearchIntent
    data object OpenGeneratedExcel : SearchIntent
    data object DismissRateLimit : SearchIntent
    data object WatchRewardedAd : SearchIntent
    data object ConsumeToast : SearchIntent
}

interface SearchComponent {
    val state: Value<SearchState>
    fun onIntent(intent: SearchIntent)
}

/**
 * Pencarian lokal di SQLDelight, debounce 250 ms. Kalau lokal kosong, auto-fetch suggestions
 * dari dashboard (ranking SQL murni, gratis). User bisa tap "Buat tutorial ini" →
 * server-side auto-generate di background, mobile polling sampai completed.
 */
@OptIn(FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class DefaultSearchComponent(
    componentContext: ComponentContext,
    private val search: SearchExcelsUseCase,
    private val fetchSuggestions: FetchSuggestionsUseCase,
    private val submitRequest: SubmitGenerationRequestUseCase,
    private val submitRequestExtra: SubmitExtraGenerationRequestUseCase,
    private val fetchRequestStatus: FetchRequestStatusUseCase,
    private val toggleFavorite: ToggleFavoriteUseCase,
    private val rewardedController: RewardedAdController,
    private val onExcelSelected: (ExcelId) -> Unit,
) : SearchComponent, ComponentContext by componentContext {

    private val log = Logger.withTag("SearchComponent")
    private val _state = MutableValue(SearchState())
    override val state: Value<SearchState> = _state

    private val scope = coroutineScope(Dispatchers.Main)
    private val queryFlow = MutableStateFlow("")
    private var suggestionsJob: Job? = null
    private var pollingJob: Job? = null

    init {
        queryFlow
            .debounce(250)
            .flatMapLatest { q ->
                _state.update { it.copy(isSearching = q.isNotBlank()) }
                search(q)
            }
            .onEach { results ->
                _state.update { it.copy(isSearching = false, results = results, error = null) }
                val st = _state.value
                if (st.query.length >= 2 && results.isEmpty() && st.generate == null) {
                    triggerFetchSuggestions(st.query)
                } else if (results.isNotEmpty()) {
                    _state.update { it.copy(suggestions = emptyList()) }
                }
            }
            .catch { t ->
                _state.update { it.copy(isSearching = false, error = t.message) }
            }
            .launchIn(scope)
    }

    override fun onIntent(intent: SearchIntent) {
        when (intent) {
            is SearchIntent.QueryChanged -> {
                // Query baru → batalkan generate job yang lama, reset banner.
                pollingJob?.cancel()
                _state.update {
                    it.copy(
                        query = intent.value,
                        error = null,
                        generate = null,
                        suggestions = emptyList(),
                    )
                }
                queryFlow.value = intent.value.trim()
            }
            SearchIntent.Clear -> {
                suggestionsJob?.cancel()
                pollingJob?.cancel()
                _state.value = SearchState()
                queryFlow.value = ""
            }
            is SearchIntent.ItemClicked -> onExcelSelected(intent.id)
            is SearchIntent.ToggleFavorite -> scope.launch { toggleFavorite(intent.id) }
            SearchIntent.GenerateExcel -> onGenerateExcel()
            SearchIntent.DismissGenerate -> {
                pollingJob?.cancel()
                _state.update { it.copy(generate = null) }
            }
            SearchIntent.OpenGeneratedExcel -> {
                _state.value.generate?.excelId?.let(onExcelSelected)
            }
            SearchIntent.DismissRateLimit -> _state.update {
                it.copy(rateLimitDialog = null, generate = null)
            }
            SearchIntent.WatchRewardedAd -> onWatchRewardedAd()
            SearchIntent.ConsumeToast -> _state.update { it.copy(toast = null) }
        }
    }

    private fun triggerFetchSuggestions(query: String) {
        suggestionsJob?.cancel()
        suggestionsJob = scope.launch {
            _state.update { it.copy(isFetchingSuggestions = true) }
            when (val result = fetchSuggestions(query)) {
                is AppResult.Success -> _state.update {
                    if (it.query.trim() == query) {
                        it.copy(isFetchingSuggestions = false, suggestions = result.data)
                    } else it.copy(isFetchingSuggestions = false)
                }
                is AppResult.Failure -> _state.update {
                    it.copy(isFetchingSuggestions = false)
                }
            }
        }
    }

    private fun onGenerateExcel() {
        val query = _state.value.query.trim()
        log.i { "onGenerateExcel: query='$query' length=${query.length}" }
        if (query.length < 3) {
            log.w { "Query terlalu pendek (min 3) — ignore" }
            return
        }

        _state.update {
            it.copy(
                generate = GenerateJob(
                    requestId = "",
                    status = GenerationRequestStatus.Processing,
                    message = "Mengirim permintaan…",
                ),
            )
        }

        scope.launch {
            log.i { "Calling submitRequest('$query')..." }
            val result = submitRequest(query)
            log.i { "submitRequest result: $result" }
            handleSubmitResult(result)
        }
    }

    /**
     * Escape hatch saat rate limit kena. Tampilkan rewarded ad; kalau user tonton sampai
     * selesai → submit via endpoint `/api/requests/extra` yang bypass rate limit biasa.
     */
    private fun onWatchRewardedAd() {
        val dialog = _state.value.rateLimitDialog ?: return
        if (dialog.isWatchingAd) return

        _state.update {
            it.copy(rateLimitDialog = dialog.copy(isWatchingAd = true))
        }

        scope.launch {
            val outcome = rewardedController.loadAndShow()
            when (outcome) {
                is RewardedOutcome.Granted -> {
                    _state.update {
                        it.copy(
                            rateLimitDialog = null,
                            generate = GenerateJob(
                                requestId = "",
                                status = GenerationRequestStatus.Processing,
                                message = "Mengirim permintaan…",
                            ),
                        )
                    }
                    val query = _state.value.query.trim()
                    handleSubmitResult(submitRequestExtra(query))
                }
                is RewardedOutcome.Dismissed -> _state.update {
                    it.copy(
                        rateLimitDialog = it.rateLimitDialog?.copy(isWatchingAd = false),
                        toast = "Iklan di-tutup sebelum selesai. Coba lagi atau tunggu.",
                    )
                }
                is RewardedOutcome.Failed -> _state.update {
                    it.copy(
                        rateLimitDialog = it.rateLimitDialog?.copy(isWatchingAd = false),
                        toast = "Gagal memuat iklan: ${outcome.reason}",
                    )
                }
            }
        }
    }

    /**
     * Handler hasil submit generate request — dipakai oleh regular submit dan extra submit.
     * Sukses → cek AlreadyExists short-circuit atau mulai polling. Failure →
     *   - RateLimited: tampilkan dialog escape hatch
     *   - Lainnya: set generate status Failed
     */
    private fun handleSubmitResult(result: AppResult<SubmitRequestResult>) {
        log.i { "handleSubmitResult: $result" }
        when (result) {
            is AppResult.Success -> {
                val submission = result.data
                log.i { "Success: status=${submission.status} requestId=${submission.requestId} excelId=${submission.excelId}" }
                if (submission.status == GenerationRequestStatus.AlreadyExists &&
                    submission.excelId != null
                ) {
                    _state.update {
                        it.copy(
                            generate = GenerateJob(
                                requestId = submission.requestId ?: "",
                                status = GenerationRequestStatus.AlreadyExists,
                                message = submission.message,
                                excelId = submission.excelId,
                            ),
                        )
                    }
                    return
                }
                val reqId = submission.requestId
                if (reqId.isNullOrBlank()) {
                    _state.update {
                        it.copy(
                            generate = GenerateJob(
                                requestId = "",
                                status = GenerationRequestStatus.Failed,
                                message = "Server tidak mengembalikan request ID.",
                                errorMessage = "Invalid response",
                            ),
                        )
                    }
                    return
                }
                _state.update {
                    it.copy(
                        generate = GenerateJob(
                            requestId = reqId,
                            status = GenerationRequestStatus.Processing,
                            message = submission.message,
                        ),
                    )
                }
                startPolling(reqId)
            }
            is AppResult.Failure -> {
                val err = result.error
                log.w { "Failure: type=${err::class.simpleName} message=${err.message}" }
                if (err is AppError.RateLimited) {
                    log.i { "Rate limited — tampil dialog escape hatch (retryAfter=${err.retryAfterSeconds}s)" }
                    _state.update {
                        it.copy(
                            generate = null,
                            rateLimitDialog = RateLimitDialogState(retryAfterSeconds = err.retryAfterSeconds),
                        )
                    }
                    return
                }
                _state.update {
                    it.copy(
                        generate = GenerateJob(
                            requestId = "",
                            status = GenerationRequestStatus.Failed,
                            message = "Gagal mengirim permintaan",
                            errorMessage = err.message,
                        ),
                    )
                }
            }
        }
    }

    private fun startPolling(requestId: String) {
        log.i { "startPolling: requestId=$requestId" }
        pollingJob?.cancel()
        val startedAt = Clock.System.now().toEpochMilliseconds()
        pollingJob = scope.launch {
            while (isActive) {
                delay(POLL_INTERVAL_MS)
                if (Clock.System.now().toEpochMilliseconds() - startedAt > POLL_TIMEOUT_MS) {
                    _state.update { st ->
                        val current = st.generate ?: return@update st
                        st.copy(
                            generate = current.copy(
                                status = GenerationRequestStatus.Failed,
                                message = "Timeout — server terlalu lama merespons.",
                                errorMessage = "timeout",
                            ),
                        )
                    }
                    return@launch
                }
                when (val poll = fetchRequestStatus(requestId)) {
                    is AppResult.Success -> {
                        val data = poll.data
                        _state.update { st ->
                            val current = st.generate ?: return@update st
                            if (current.requestId != requestId) return@update st
                            st.copy(
                                generate = current.copy(
                                    status = data.status,
                                    excelId = data.excelId,
                                    errorMessage = data.errorMessage,
                                    message = statusMessage(data.status, data.errorMessage),
                                ),
                            )
                        }
                        if (data.status == GenerationRequestStatus.Completed ||
                            data.status == GenerationRequestStatus.Failed
                        ) {
                            return@launch
                        }
                    }
                    is AppResult.Failure -> {
                        // Polling gagal sekali — lanjut, mungkin network flaky. Kalau fatal,
                        // timeout akan eventually kick in.
                    }
                }
            }
        }
    }

    private fun statusMessage(status: GenerationRequestStatus, error: String?): String = when (status) {
        GenerationRequestStatus.Processing -> "Sedang membuat tutorial… biasanya 15-30 detik."
        GenerationRequestStatus.Completed -> "Tutorial berhasil dibuat."
        GenerationRequestStatus.Failed -> error?.let { "Gagal: $it" } ?: "Gagal membuat tutorial."
        GenerationRequestStatus.AlreadyExists -> "Tutorial sudah tersedia."
        GenerationRequestStatus.Unknown -> "Status tidak diketahui."
    }
}

private fun MutableValue<SearchState>.update(transform: (SearchState) -> SearchState) {
    value = transform(value)
}
