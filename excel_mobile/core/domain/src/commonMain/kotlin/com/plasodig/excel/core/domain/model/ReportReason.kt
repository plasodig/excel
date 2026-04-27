package com.plasodig.excel.core.domain.model

/**
 * Alasan user melaporkan tutorial. String value dikirim ke server apa adanya.
 */
enum class ReportReason(val apiValue: String, val label: String) {
    Inaccurate("inaccurate", "Tidak akurat (rumus/langkah salah)"),
    Offensive("offensive", "Menyinggung atau tidak pantas"),
    Dangerous("dangerous", "Menyesatkan atau berisiko"),
    Other("other", "Lainnya"),
}

/** Hasil submit report (server balas count terbaru + apakah excel auto-unpublished). */
data class ReportResult(
    val reportCount: Int,
    val unpublished: Boolean,
    val message: String,
)
