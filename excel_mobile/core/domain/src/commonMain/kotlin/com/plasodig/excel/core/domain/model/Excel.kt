package com.plasodig.excel.core.domain.model

import kotlin.jvm.JvmInline

@JvmInline
value class ExcelId(val value: String)

data class Excel(
    val id: ExcelId,
    val title: String,
    val description: String,
    val imageUrl: String,
    val category: ExcelCategory,
    val difficulty: Difficulty,
    val cookingTimeMinutes: Int,
    val servings: Int,
    val relatedFunctions: List<RelatedFunction>,
    val steps: List<TutorialStep>,
    val tags: List<String> = emptyList(),
)

data class RelatedFunction(
    val name: String,
    val quantity: String,
    val unit: String,
    val group: String? = null,
)

data class TutorialStep(
    val order: Int,
    val instruction: String,
    val durationMinutes: Int? = null,
)

enum class Difficulty { Easy, Medium, Hard }

enum class ExcelCategory(val label: String) {
    BasicFormula("Rumus Dasar"),
    Function("Fungsi"),
    Lookup("Lookup & Reference"),
    PivotTable("PivotTable"),
    Chart("Grafik & Chart"),
    Macro("Makro & VBA"),
    Format("Format & Style"),
    Database("Database & Filter"),
}
