package com.plasodig.excel

import android.app.Application
import android.content.Context
import com.plasodig.excel.core.ads.ConsentManager
import com.plasodig.excel.core.ads.RewardedAdController
import com.plasodig.excel.core.data.config.ApiConfig
import com.plasodig.excel.core.database.DatabaseDriverFactory
import com.plasodig.excel.di.initKoin
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

class ExcelApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@ExcelApplication)
            modules(
                module {
                    single { DatabaseDriverFactory(get()) }
                    single { ApiConfig(baseUrl = BuildConfig.EXCEL_API_BASE) }
                    // ConsentManager butuh Activity context idealnya — tapi di Application
                    // scope kita set dulu dengan app context, lalu di App.kt rebuild dengan
                    // activity context kalau tersedia. Factory by default read androidContext.
                    single { ConsentManager(get<Context>(), debugBypass = BuildConfig.DEBUG) }
                    single { RewardedAdController(get<Context>()) }
                },
            )
        }
    }
}
