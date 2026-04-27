package com.plasodig.excel.feature.excellist

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import com.plasodig.excel.core.common.AppResult
import com.plasodig.excel.core.domain.model.PageResult
import com.plasodig.excel.core.domain.model.ExcelCategory
import com.plasodig.excel.core.domain.model.ExcelId
import com.plasodig.excel.core.domain.model.ExcelSummary
import com.plasodig.excel.core.domain.usecase.GetExcelListUseCase
import com.plasodig.excel.core.domain.usecase.LoadExcelPageUseCase
import com.plasodig.excel.core.domain.usecase.RefreshExcelsUseCase
import com.plasodig.excel.core.domain.usecase.ToggleFavoriteUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Ukuran page — server fetch 10 tutorial per request. Disarankan kelipatan dari ukuran
 * grid row (2 kolom) supaya footer indicator tampil rapi.
 */
const val PAGE_SIZE: Int = 10

data class ExcelListState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    /** Sedang fetch page berikutnya. Berbeda dari `isRefreshing` supaya footer loader tampil. */
    val isPaging: Boolean = false,
    val selectedCategory: ExcelCategory? = null,
    val items: List<ExcelSummary> = emptyList(),
    val error: String? = null,
    /** Berapa item sudah ter-fetch dari server. Juga = LIMIT untuk observe DB. */
    val loadedCount: Int = PAGE_SIZE,
    /** Server sudah kasih semua — LoadMore di-skip. */
    val reachedEnd: Boolean = false,
    /** Total tutorial published untuk kategori aktif (0 kalau belum diketahui). */
    val totalCount: Int = 0,
    /** One-shot event untuk snackbar refresh. */
    val refreshEvent: RefreshEvent? = null,
) {
    val isEmpty: Boolean get() = !isLoading && items.isEmpty() && error == null
    val hasMore: Boolean get() = !reachedEnd
}

data class RefreshEvent(val id: Long, val message: String, val isError: Boolean)

sealed interface ExcelListIntent {
    data object Load : ExcelListIntent
    data object Refresh : ExcelListIntent
    data object LoadMore : ExcelListIntent
    data object ConsumeRefreshEvent : ExcelListIntent
    data class FilterChanged(val category: ExcelCategory?) : ExcelListIntent
    data class ToggleFavorite(val id: ExcelId) : ExcelListIntent
    data class ItemClicked(val id: ExcelId) : ExcelListIntent
}

interface ExcelListComponent {
    val state: Value<ExcelListState>
    fun onIntent(intent: ExcelListIntent)
}

class DefaultExcelListComponent(
    componentContext: ComponentContext,
    private val getExcels: GetExcelListUseCase,
    private val toggleFavorite: ToggleFavoriteUseCase,
    private val refresh: RefreshExcelsUseCase,
    private val loadPage: LoadExcelPageUseCase,
    private val onExcelSelected: (ExcelId) -> Unit,
) : ExcelListComponent, ComponentContext by componentContext {

    private val _state = MutableValue(ExcelListState())
    override val state: Value<ExcelListState> = _state

    private val scope = coroutineScope(Dispatchers.Main)
    private var observeJob: Job? = null
    private var refreshEventCounter: Long = 0L

    init {
        observe(category = null, limit = PAGE_SIZE)
        // Fetch page pertama dari server saat startup (background — DB emit empty dulu).
        scope.launch { refreshFirstPage(category = null, silent = true) }
    }

    override fun onIntent(intent: ExcelListIntent) {
        when (intent) {
            ExcelListIntent.Load -> {
                val st = _state.value
                observe(st.selectedCategory, st.loadedCount)
            }
            ExcelListIntent.Refresh -> onRefresh()
            ExcelListIntent.LoadMore -> onLoadMore()
            ExcelListIntent.ConsumeRefreshEvent -> _state.update { it.copy(refreshEvent = null) }
            is ExcelListIntent.FilterChanged -> onFilterChanged(intent.category)
            is ExcelListIntent.ToggleFavorite -> scope.launch { toggleFavorite(intent.id) }
            is ExcelListIntent.ItemClicked -> onExcelSelected(intent.id)
        }
    }

    private fun observe(category: ExcelCategory?, limit: Int) {
        observeJob?.cancel()
        observeJob = getExcels(category, limit)
            .onEach { list ->
                _state.update { it.copy(isLoading = false, items = list, error = null) }
            }
            .catch { t ->
                _state.update { it.copy(isLoading = false, error = t.message) }
            }
            .launchIn(scope)
    }

    private fun onFilterChanged(category: ExcelCategory?) {
        _state.update {
            it.copy(
                selectedCategory = category,
                loadedCount = PAGE_SIZE,
                reachedEnd = false,
                totalCount = 0,
            )
        }
        observe(category, PAGE_SIZE)
        scope.launch { refreshFirstPage(category, silent = true) }
    }

    private fun onRefresh() {
        scope.launch {
            _state.update { it.copy(isRefreshing = true, refreshEvent = null) }
            val result = refreshFirstPage(_state.value.selectedCategory, silent = false)
            _state.update { it.copy(isRefreshing = false, refreshEvent = result) }
        }
    }

    /**
     * Fetch page pertama dari server. Kalau sukses → reset loadedCount ke PAGE_SIZE dan
     * re-subscribe DB observer supaya UI render fresh data.
     * `silent = true` → tidak emit refreshEvent (dipakai di startup/filter change).
     */
    private suspend fun refreshFirstPage(
        category: ExcelCategory?,
        silent: Boolean,
    ): RefreshEvent? = when (val result = refresh(category, PAGE_SIZE)) {
        is AppResult.Success -> {
            applyPageResult(result.data, resetToFirstPage = true)
            observe(category, PAGE_SIZE)
            if (silent) null else RefreshEvent(
                id = ++refreshEventCounter,
                message = "Daftar diperbarui",
                isError = false,
            )
        }
        is AppResult.Failure -> {
            // Kalau DB kosong & API gagal, surface error ke UI supaya user tahu.
            if (_state.value.items.isEmpty()) {
                _state.update { it.copy(error = result.error.message) }
            }
            if (silent) null else RefreshEvent(
                id = ++refreshEventCounter,
                message = "Refresh gagal: ${result.error.message}",
                isError = true,
            )
        }
    }

    private fun onLoadMore() {
        val st = _state.value
        if (st.reachedEnd || st.isPaging || st.isRefreshing) return

        scope.launch {
            _state.update { it.copy(isPaging = true) }
            val category = _state.value.selectedCategory
            val offset = _state.value.loadedCount
            when (val result = loadPage(category, offset, PAGE_SIZE)) {
                is AppResult.Success -> {
                    applyPageResult(result.data, resetToFirstPage = false)
                    observe(_state.value.selectedCategory, _state.value.loadedCount)
                }
                is AppResult.Failure -> {
                    _state.update {
                        it.copy(
                            isPaging = false,
                            refreshEvent = RefreshEvent(
                                id = ++refreshEventCounter,
                                message = "Muat tambahan gagal: ${result.error.message}",
                                isError = true,
                            ),
                        )
                    }
                }
            }
        }
    }

    private fun applyPageResult(page: PageResult, resetToFirstPage: Boolean) {
        _state.update { st ->
            // Cap increment ke PAGE_SIZE: kalau server kirim lebih (mis. belum support paging),
            // loadedCount jangan melompat — sebagai gantinya kita render semua yang server
            // kasih lewat branch khusus di bawah.
            val increment = minOf(page.receivedCount, PAGE_SIZE)
            val newLoaded = when {
                // Server kirim full manifest saat refresh pertama → tampilkan semua sekaligus
                // (kalau tidak, user akan lihat 10 dari N tanpa bisa LoadMore karena reachedEnd).
                resetToFirstPage && page.receivedCount > PAGE_SIZE -> page.receivedCount
                resetToFirstPage -> PAGE_SIZE
                else -> st.loadedCount + increment
            }
            st.copy(
                isPaging = false,
                loadedCount = newLoaded,
                reachedEnd = page.reachedEnd,
                totalCount = when {
                    page.total > 0 -> page.total
                    page.reachedEnd -> newLoaded
                    else -> st.totalCount
                },
                error = null,
            )
        }
    }
}

private fun MutableValue<ExcelListState>.update(transform: (ExcelListState) -> ExcelListState) {
    value = transform(value)
}
