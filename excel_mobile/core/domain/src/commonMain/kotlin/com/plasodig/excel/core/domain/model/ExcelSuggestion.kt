package com.plasodig.excel.core.domain.model

/**
 * Saran tutorial yang sudah published untuk query user yang miss di pencarian.
 * Dihitung di server via SQL LIKE + ranking heuristik (zero-LLM, gratis).
 */
data class ExcelSuggestion(
    val id: ExcelId,
    val title: String,
    val category: ExcelCategory,
    val description: String,
    val difficulty: Difficulty,
    val cookingTimeMinutes: Int,
    val tags: List<String>,
    val imageUrl: String,
)
