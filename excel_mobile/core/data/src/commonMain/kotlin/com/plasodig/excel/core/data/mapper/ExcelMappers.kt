package com.plasodig.excel.core.data.mapper

import com.plasodig.excel.core.database.TutorialStepEntity
import com.plasodig.excel.core.database.RelatedFunctionEntity
import com.plasodig.excel.core.database.ExcelEntity
import com.plasodig.excel.core.database.dao.RelatedFunctionInput
import com.plasodig.excel.core.database.dao.StepInput
import com.plasodig.excel.core.domain.model.TutorialStep
import com.plasodig.excel.core.domain.model.Difficulty
import com.plasodig.excel.core.domain.model.RelatedFunction
import com.plasodig.excel.core.domain.model.Excel
import com.plasodig.excel.core.domain.model.ExcelCategory
import com.plasodig.excel.core.domain.model.ExcelId
import com.plasodig.excel.core.domain.model.ExcelSummary

fun ExcelEntity.toSummary(isFavorite: Boolean): ExcelSummary = ExcelSummary(
    id = ExcelId(id),
    title = title,
    description = description,
    imageUrl = imageUrl,
    category = category.toCategory(),
    difficulty = difficulty.toDifficulty(),
    cookingTimeMinutes = cookingTimeMinutes.toInt(),
    isFavorite = isFavorite,
)

fun ExcelEntity.toDomain(
    relatedFunctions: List<RelatedFunctionEntity>,
    steps: List<TutorialStepEntity>,
): Excel = Excel(
    id = ExcelId(id),
    title = title,
    description = description,
    imageUrl = imageUrl,
    category = category.toCategory(),
    difficulty = difficulty.toDifficulty(),
    cookingTimeMinutes = cookingTimeMinutes.toInt(),
    servings = servings.toInt(),
    relatedFunctions = relatedFunctions.map { RelatedFunction(it.name, it.quantity, it.unit, it.groupName) },
    steps = steps.map { TutorialStep(it.stepOrder.toInt(), it.instruction, it.durationMinutes?.toInt()) },
    tags = tags.split(",").filter { it.isNotBlank() },
)

fun Excel.toRelatedFunctionInputs(): List<RelatedFunctionInput> =
    relatedFunctions.mapIndexed { idx, i -> RelatedFunctionInput(i.name, i.quantity, i.unit, i.group, idx) }

fun Excel.toStepInputs(): List<StepInput> =
    steps.map { StepInput(it.order, it.instruction, it.durationMinutes) }

fun Excel.tagsJoined(): String = tags.joinToString(",")

private fun String.toCategory(): ExcelCategory = runCatching {
    ExcelCategory.valueOf(this)
}.getOrElse { ExcelCategory.BasicFormula }

private fun String.toDifficulty(): Difficulty = runCatching {
    Difficulty.valueOf(this)
}.getOrElse { Difficulty.Medium }
