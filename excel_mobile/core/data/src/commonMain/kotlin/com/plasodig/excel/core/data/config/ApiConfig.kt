package com.plasodig.excel.core.data.config

/**
 * Konfigurasi base URL endpoint Tutorial Dashboard. Di-bind sebagai single ke Koin di app entry
 * point (Android `ExcelApplication`, iOS `MainViewController`) supaya gampang ganti
 * antara dev/staging/prod tanpa rebuild data module.
 */
data class ApiConfig(
    /** Contoh: "https://excel.plasodig.my.id" — TANPA trailing slash. */
    val baseUrl: String,
) {
    init { require(baseUrl.isNotBlank()) { "ApiConfig.baseUrl kosong" } }

    private val normalizedBase: String = baseUrl.trimEnd('/')

    /**
     * URL manifest. Kalau `limit`/`offset` di-set, dashboard akan return page sesuai request.
     * Server clamp limit max 100. `category` opsional filter di server.
     */
    fun excelsUrl(category: String? = null, offset: Int = 0, limit: Int = 0): String {
        if (category == null && limit <= 0 && offset <= 0) return "$normalizedBase/api/excels"
        val params = buildList {
            if (limit > 0) add("limit=$limit")
            if (offset > 0) add("offset=$offset")
            if (!category.isNullOrBlank()) add("category=$category")
        }
        return "$normalizedBase/api/excels?${params.joinToString("&")}"
    }

    fun excelDetailUrl(id: String): String = "$normalizedBase/api/excels/$id"

    val suggestionsUrl: String get() = "$normalizedBase/api/suggestions"
    val requestsUrl: String get() = "$normalizedBase/api/requests"
    val requestsExtraUrl: String get() = "$normalizedBase/api/requests/extra"
    fun requestStatusUrl(id: String): String = "$normalizedBase/api/requests/$id"
    fun reportExcelUrl(id: String): String = "$normalizedBase/api/excels/$id/report"

    /**
     * Dashboard mengirim field `imageUrl` sebagai path relatif (mis. `/api/images/excels/vlookup-dasar.png`)
     * karena gambar di-proxy dari KV, bukan R2 public URL. Coil butuh absolute URL → resolve di sini.
     */
    fun resolveImageUrl(url: String?): String = when {
        url.isNullOrBlank() -> ""
        url.startsWith("http://") || url.startsWith("https://") -> url
        url.startsWith("/") -> "$normalizedBase$url"
        else -> "$normalizedBase/$url"
    }
}
