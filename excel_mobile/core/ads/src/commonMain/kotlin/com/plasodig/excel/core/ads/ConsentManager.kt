package com.plasodig.excel.core.ads

/**
 * Consent manager untuk UMP (User Messaging Platform) — compliance GDPR/CCPA/UU PDP.
 *
 * Flow:
 * 1. App startup → `ensureConsent()` diikhlaskan (fire-and-forget)
 * 2. Kalau user EU / CA / ID region dengan consent required → form otomatis muncul
 * 3. User kasih preferensi → ads SDK initialize dengan scope yang sesuai
 * 4. Kalau tidak required → langsung ok, ads muncul normal
 *
 * Di Android pakai Google UMP SDK; di iOS no-op sementara (Phase 1).
 */
expect class ConsentManager {

    /**
     * Request consent info dari UMP + kalau perlu tampilkan form. Dipanggil sekali saat startup.
     * Setelah ini selesai (atau tidak required), ads SDK boleh di-initialize.
     */
    suspend fun ensureConsent()

    /** True kalau user bisa di-serve personalized ads. Mempengaruhi request flags. */
    fun canServePersonalizedAds(): Boolean
}
