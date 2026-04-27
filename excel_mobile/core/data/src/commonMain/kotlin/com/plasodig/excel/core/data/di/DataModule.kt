package com.plasodig.excel.core.data.di

import com.plasodig.excel.core.data.remote.RemoteExcelApi
import com.plasodig.excel.core.data.repository.ExcelRepositoryImpl
import com.plasodig.excel.core.database.ExcelDatabase
import com.plasodig.excel.core.database.createDatabase
import com.plasodig.excel.core.database.dao.ExcelDao
import com.plasodig.excel.core.domain.repository.ExcelRepository
import com.plasodig.excel.core.domain.usecase.EnsureExcelDetailUseCase
import com.plasodig.excel.core.domain.usecase.FetchRequestStatusUseCase
import com.plasodig.excel.core.domain.usecase.FetchSuggestionsUseCase
import com.plasodig.excel.core.domain.usecase.GetExcelDetailUseCase
import com.plasodig.excel.core.domain.usecase.GetExcelListUseCase
import com.plasodig.excel.core.domain.usecase.LoadExcelPageUseCase
import com.plasodig.excel.core.domain.usecase.ObserveFavoritesUseCase
import com.plasodig.excel.core.domain.usecase.RefreshExcelsUseCase
import com.plasodig.excel.core.domain.usecase.ReportExcelUseCase
import com.plasodig.excel.core.domain.usecase.SearchExcelsUseCase
import com.plasodig.excel.core.domain.usecase.SubmitExtraGenerationRequestUseCase
import com.plasodig.excel.core.domain.usecase.SubmitGenerationRequestUseCase
import com.plasodig.excel.core.domain.usecase.ToggleFavoriteUseCase
import org.koin.core.module.Module
import org.koin.dsl.module

val useCaseModule: Module = module {
    factory { GetExcelListUseCase(get()) }
    factory { GetExcelDetailUseCase(get()) }
    factory { SearchExcelsUseCase(get()) }
    factory { ToggleFavoriteUseCase(get()) }
    factory { ObserveFavoritesUseCase(get()) }
    factory { RefreshExcelsUseCase(get()) }
    factory { LoadExcelPageUseCase(get()) }
    factory { EnsureExcelDetailUseCase(get()) }
    factory { FetchSuggestionsUseCase(get()) }
    factory { SubmitGenerationRequestUseCase(get()) }
    factory { SubmitExtraGenerationRequestUseCase(get()) }
    factory { FetchRequestStatusUseCase(get()) }
    factory { ReportExcelUseCase(get()) }
}

/**
 * Mobile app sebagai konsumen API excel_dashboard:
 * - SQLDelight cache lokal untuk offline-first.
 * - RemoteExcelApi (Ktor) untuk fetch manifest + detail dari Cloudflare Worker.
 * - ApiConfig di-bind di entry point (Android `ExcelApplication`, iOS `MainViewController`)
 *   supaya base URL bisa beda per build (dev vs prod).
 */
val dataModule: Module = module {
    single<ExcelDatabase> { createDatabase(get()) }
    single { ExcelDao(get()) }
    single { RemoteExcelApi(config = get()) }
    single<ExcelRepository> {
        ExcelRepositoryImpl(
            dao = get(),
            api = get(),
            apiConfig = get(),
        )
    }
}
