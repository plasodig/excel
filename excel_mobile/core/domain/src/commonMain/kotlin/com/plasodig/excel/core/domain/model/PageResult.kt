package com.plasodig.excel.core.domain.model

/**
 * Hasil satu page fetch dari server.
 *
 * @property receivedCount jumlah item yang diterima di page ini (bisa 0 sampai pageSize)
 * @property total total item published di server untuk query ini (semua category kalau null)
 * @property reachedEnd true kalau offset + receivedCount >= total (tidak ada page berikutnya)
 */
data class PageResult(
    val receivedCount: Int,
    val total: Int,
    val reachedEnd: Boolean,
)
