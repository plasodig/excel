package com.plasodig.excel.core.domain.model

data class ExcelSummary(
    val id: ExcelId,
    val title: String,
    val description: String,
    val imageUrl: String,
    val category: ExcelCategory,
    val difficulty: Difficulty,
    val cookingTimeMinutes: Int,
    val isFavorite: Boolean = false,
)

fun Excel.toSummary(isFavorite: Boolean = false) = ExcelSummary(
    id = id,
    title = title,
    description = description,
    imageUrl = imageUrl,
    category = category,
    difficulty = difficulty,
    cookingTimeMinutes = cookingTimeMinutes,
    isFavorite = isFavorite,
)
