package com.plasodig.excel.feature.exceldetail

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import com.plasodig.excel.core.common.AppError
import com.plasodig.excel.core.common.AppResult
import com.plasodig.excel.core.domain.model.Excel
import com.plasodig.excel.core.domain.model.ExcelId
import com.plasodig.excel.core.domain.model.ReportReason
import com.plasodig.excel.core.domain.usecase.EnsureExcelDetailUseCase
import com.plasodig.excel.core.domain.usecase.GetExcelDetailUseCase
import com.plasodig.excel.core.domain.usecase.ReportExcelUseCase
import com.plasodig.excel.core.domain.usecase.ToggleFavoriteUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class ExcelDetailState(
    val isLoading: Boolean = true,
    val excel: Excel? = null,
    val checkedSteps: Set<Int> = emptySet(),
    val isFavorite: Boolean = false,
    val error: String? = null,
    /** True saat fetch detail dari API (lazy load pertama kali tutorial dibuka). */
    val isFetchingDetail: Boolean = false,
    /** Pesan error fetch detail. Null kalau tidak ada. */
    val fetchError: String? = null,
    /** Dialog laporkan terbuka. */
    val showReportDialog: Boolean = false,
    /** Sedang submit report ke server. */
    val isReporting: Boolean = false,
    /** One-shot snackbar text setelah report sukses/gagal. */
    val reportToast: String? = null,
)

sealed interface ExcelDetailIntent {
    data object Back : ExcelDetailIntent
    data object ToggleFavorite : ExcelDetailIntent
    data class ToggleStep(val order: Int) : ExcelDetailIntent
    data object Retry : ExcelDetailIntent
    data object OpenReportDialog : ExcelDetailIntent
    data object DismissReportDialog : ExcelDetailIntent
    data class SubmitReport(val reason: ReportReason, val detail: String = "") : ExcelDetailIntent
    data object ConsumeReportToast : ExcelDetailIntent
}

interface ExcelDetailComponent {
    val state: Value<ExcelDetailState>
    fun onIntent(intent: ExcelDetailIntent)
}

class DefaultExcelDetailComponent(
    componentContext: ComponentContext,
    private val excelId: ExcelId,
    private val getDetail: GetExcelDetailUseCase,
    private val toggleFavorite: ToggleFavoriteUseCase,
    private val ensureDetail: EnsureExcelDetailUseCase,
    private val reportExcel: ReportExcelUseCase,
    private val onBack: () -> Unit,
) : ExcelDetailComponent, ComponentContext by componentContext {

    private val _state = MutableValue(ExcelDetailState())
    override val state: Value<ExcelDetailState> = _state
    private val scope = coroutineScope(Dispatchers.Main)

    init {
        // Observe DB — emit summary dulu, lalu detail lengkap setelah ensureDetail() upsert.
        getDetail(excelId)
            .onEach { excel ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    excel = excel,
                    error = if (excel == null) "Tutorial tidak ditemukan" else null,
                )
            }
            .catch { t -> _state.value = _state.value.copy(isLoading = false, error = t.message) }
            .launchIn(scope)

        // Lazy fetch detail (relatedFunctions + steps) kalau belum ada.
        triggerFetch()
    }

    private fun triggerFetch() {
        scope.launch {
            _state.value = _state.value.copy(isFetchingDetail = true, fetchError = null)
            when (val result = ensureDetail(excelId)) {
                is AppResult.Success -> _state.value = _state.value.copy(isFetchingDetail = false, fetchError = null)
                is AppResult.Failure -> _state.value = _state.value.copy(
                    isFetchingDetail = false,
                    fetchError = result.error.userMessage(),
                )
            }
        }
    }

    override fun onIntent(intent: ExcelDetailIntent) {
        when (intent) {
            ExcelDetailIntent.Back -> onBack()
            ExcelDetailIntent.ToggleFavorite -> scope.launch {
                toggleFavorite(excelId)
                _state.value = _state.value.copy(isFavorite = !_state.value.isFavorite)
            }
            is ExcelDetailIntent.ToggleStep -> {
                val current = _state.value.checkedSteps
                _state.value = _state.value.copy(
                    checkedSteps = if (intent.order in current) current - intent.order
                    else current + intent.order,
                )
            }
            ExcelDetailIntent.Retry -> triggerFetch()
            ExcelDetailIntent.OpenReportDialog -> _state.value = _state.value.copy(showReportDialog = true)
            ExcelDetailIntent.DismissReportDialog -> _state.value = _state.value.copy(showReportDialog = false)
            ExcelDetailIntent.ConsumeReportToast -> _state.value = _state.value.copy(reportToast = null)
            is ExcelDetailIntent.SubmitReport -> onSubmitReport(intent)
        }
    }

    private fun onSubmitReport(intent: ExcelDetailIntent.SubmitReport) {
        scope.launch {
            _state.value = _state.value.copy(isReporting = true)
            val result = reportExcel(excelId, intent.reason, intent.detail)
            _state.value = when (result) {
                is AppResult.Success -> _state.value.copy(
                    isReporting = false,
                    showReportDialog = false,
                    reportToast = if (result.data.unpublished)
                        "Laporan diterima. Tutorial dihilangkan untuk ditinjau."
                    else
                        "Terima kasih, laporanmu kami terima.",
                )
                is AppResult.Failure -> _state.value.copy(
                    isReporting = false,
                    showReportDialog = false,
                    reportToast = "Gagal mengirim laporan: ${result.error.message}",
                )
            }
        }
    }
}

private fun AppError.userMessage(): String = when (this) {
    is AppError.Network -> message
    is AppError.NotFound -> "Tidak ditemukan: $message"
    is AppError.Database -> "Penyimpanan: $message"
    is AppError.Validation -> "Tidak valid: $message"
    is AppError.Unknown -> "Tidak diketahui: $message"
    is AppError.RateLimited -> "Terlalu banyak permintaan. Coba lagi nanti."
}
