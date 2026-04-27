package com.plasodig.excel.core.ads

/**
 * iOS stub — no-op. Saat CocoaPods setup, ganti dengan UMP iOS SDK
 * (`Google-Mobile-Ads-SDK` include UMP module).
 */
actual class ConsentManager {

    actual suspend fun ensureConsent() {
        // TODO: integrasi UMP iOS saat Podfile setup. Saat ini no-op — ads iOS belum aktif.
    }

    actual fun canServePersonalizedAds(): Boolean = true
}
