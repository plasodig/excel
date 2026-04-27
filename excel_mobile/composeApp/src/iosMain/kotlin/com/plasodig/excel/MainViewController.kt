package com.plasodig.excel

import androidx.compose.ui.window.ComposeUIViewController
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.plasodig.excel.core.ads.ConsentManager
import com.plasodig.excel.core.ads.RewardedAdController
import com.plasodig.excel.core.data.config.ApiConfig
import com.plasodig.excel.core.database.DatabaseDriverFactory
import com.plasodig.excel.di.initKoin
import com.plasodig.excel.navigation.DefaultRootComponent
import org.koin.dsl.module
import platform.UIKit.UIViewController

private val lifecycle = LifecycleRegistry()
private var koinStarted = false

/**
 * iOS app harus pass base URL endpoint excel_dashboard saat construct.
 * Default ke custom domain produksi — override dari Swift kalau perlu staging/dev.
 */
fun MainViewController(apiBaseUrl: String = "https://excel.plasodig.my.id"): UIViewController {
    if (!koinStarted) {
        initKoin {
            modules(
                module {
                    single { DatabaseDriverFactory() }
                    single { ApiConfig(baseUrl = apiBaseUrl) }
                    single { ConsentManager() }
                    single { RewardedAdController() }
                },
            )
        }
        koinStarted = true
    }
    val root = DefaultRootComponent(
        componentContext = DefaultComponentContext(lifecycle = lifecycle),
    )
    return ComposeUIViewController { App(root = root) }
}
