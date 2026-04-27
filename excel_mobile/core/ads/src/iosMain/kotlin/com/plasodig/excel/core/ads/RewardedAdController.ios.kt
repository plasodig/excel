package com.plasodig.excel.core.ads

/**
 * iOS stub — butuh CocoaPods Google-Mobile-Ads-SDK di `iosApp/Podfile` + Swift bridge
 * untuk expose `GADRewardedAd`. Phase selanjutnya.
 */
actual class RewardedAdController {
    actual suspend fun loadAndShow(): RewardedOutcome =
        RewardedOutcome.Failed("Rewarded ads belum didukung di iOS")
}
