package com.plasodig.excel.core.ads

/**
 * Outcome dari menampilkan rewarded ad:
 *  - [Granted]: user tonton sampai selesai, reward diterima — caller boleh lanjut kasih benefit.
 *  - [Dismissed]: user close sebelum reward threshold — TIDAK beri benefit.
 *  - [Failed]: ada error (load gagal, tidak ada ad, dll). Tampilkan fallback ke user.
 */
sealed interface RewardedOutcome {
    data object Granted : RewardedOutcome
    data object Dismissed : RewardedOutcome
    data class Failed(val reason: String) : RewardedOutcome
}

/**
 * Load + show rewarded ad dengan suspend API. Dipakai di escape-hatch search miss:
 * kalau user kena rate limit generate, dialog offer "tonton iklan untuk 1 extra".
 *
 * Implementasi:
 *  - Android: Google Mobile Ads SDK `RewardedAd.load()` → `show(activity)`.
 *  - iOS: stub `Failed` sampai CocoaPods setup (Phase selanjutnya).
 */
expect class RewardedAdController {
    suspend fun loadAndShow(): RewardedOutcome
}
