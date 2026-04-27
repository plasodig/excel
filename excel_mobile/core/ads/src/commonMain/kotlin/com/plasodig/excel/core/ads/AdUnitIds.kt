package com.plasodig.excel.core.ads

/**
 * Ad unit IDs — TERPUSAT di sini untuk mudah diganti ke production.
 *
 * CURRENT MODE: Google AdMob TEST IDs (aman untuk development, tidak hasilkan impression billing,
 * dan tidak akan invalidate akun AdMob). Test IDs ini adalah IDs resmi yang Google sediakan.
 * Referensi: https://developers.google.com/admob/android/test-ads
 *
 * UNTUK RELEASE:
 * 1. Buat app di https://apps.admob.com/ → dapat App ID + Ad Unit IDs untuk banner & native
 * 2. Ganti string di bawah dengan production IDs
 * 3. Pastikan AndroidManifest.xml `com.google.android.gms.ads.APPLICATION_ID` juga di-update
 * 4. Ganti iOS bundle plist GADApplicationIdentifier juga
 *
 * PERINGATAN: JANGAN pernah klik iklan sendiri di production build — Google auto-banned
 * untuk "invalid traffic". Di debug build, selalu pakai test IDs (sudah default di sini).
 */
object AdUnitIds {

    // ─── App IDs ───────────────────────────────────────────────────────────
    /** Android AdMob App ID (test). Pasang di AndroidManifest meta-data APPLICATION_ID. */
    const val ADMOB_APP_ID_ANDROID = "ca-app-pub-3940256099942544~3347511713"

    /** iOS AdMob App ID (test). Pasang di Info.plist GADApplicationIdentifier. */
    const val ADMOB_APP_ID_IOS = "ca-app-pub-3940256099942544~1458002511"

    // ─── Banner (adaptive anchored) ────────────────────────────────────────
    const val BANNER_ANDROID = "ca-app-pub-3940256099942544/6300978111"
    const val BANNER_IOS = "ca-app-pub-3940256099942544/2934735716"

    // ─── Native Advanced ───────────────────────────────────────────────────
    const val NATIVE_ANDROID = "ca-app-pub-3940256099942544/2247696110"
    const val NATIVE_IOS = "ca-app-pub-3940256099942544/3986624511"

    // ─── Interstitial (belum dipakai, cadangan) ────────────────────────────
    const val INTERSTITIAL_ANDROID = "ca-app-pub-3940256099942544/1033173712"
    const val INTERSTITIAL_IOS = "ca-app-pub-3940256099942544/4411468910"

    // ─── Rewarded (escape hatch saat rate limit di search miss) ────────────
    const val REWARDED_ANDROID = "ca-app-pub-3940256099942544/5224354917"
    const val REWARDED_IOS = "ca-app-pub-3940256099942544/1712485313"

    // ─── Rewarded Interstitial (belum dipakai, cadangan) ───────────────────
    const val REWARDED_INTERSTITIAL_ANDROID = "ca-app-pub-3940256099942544/5354046379"
    const val REWARDED_INTERSTITIAL_IOS = "ca-app-pub-3940256099942544/6978759866"
}

/**
 * Platform-resolver: pick Android atau iOS ID sesuai runtime.
 * Dipakai implementasi AdSlot per platform.
 */
expect object PlatformAdIds {
    val appId: String
    val banner: String
    val native: String
    val rewarded: String
}
