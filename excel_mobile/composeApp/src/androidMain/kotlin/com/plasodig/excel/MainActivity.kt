package com.plasodig.excel

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.arkivanov.decompose.defaultComponentContext
import com.plasodig.excel.core.ads.ConsentManager
import com.plasodig.excel.core.ads.RewardedAdController
import com.plasodig.excel.navigation.DefaultRootComponent
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val consentManager: ConsentManager by inject()
    private val rewardedController: RewardedAdController by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // UMP + RewardedAd.show() butuh Activity, bukan Application context. Bind dulu
        // sebelum setContent supaya Composable ads lifecycle bisa jalan.
        consentManager.bindActivity(this)
        rewardedController.bindActivity(this)
        val root = DefaultRootComponent(
            componentContext = defaultComponentContext(),
        )
        setContent {
            App(root = root)
        }
    }

    override fun onDestroy() {
        consentManager.unbindActivity()
        rewardedController.unbindActivity()
        super.onDestroy()
    }
}
