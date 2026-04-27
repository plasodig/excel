package com.plasodig.excel.core.database.dao

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.plasodig.excel.core.database.ExcelDatabase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

class ExcelDao(
    private val db: ExcelDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private val excelQ = db.excelQueries
    private val ingQ = db.relatedFunctionQueries
    private val stepQ = db.tutorialStepQueries
    private val favQ = db.favoriteQueries

    fun observeAll(category: String? = null): Flow<List<com.plasodig.excel.core.database.ExcelEntity>> =
        if (category == null) {
            excelQ.selectAll().asFlow().mapToList(ioDispatcher)
        } else {
            excelQ.selectByCategory(category).asFlow().mapToList(ioDispatcher)
        }

    fun observeTop(
        category: String? = null,
        limit: Int,
    ): Flow<List<com.plasodig.excel.core.database.ExcelEntity>> {
        val max = limit.toLong().coerceAtLeast(0L)
        return if (category == null) {
            excelQ.selectTop(max).asFlow().mapToList(ioDispatcher)
        } else {
            excelQ.selectTopByCategory(category, max).asFlow().mapToList(ioDispatcher)
        }
    }

    fun observeById(id: String): Flow<com.plasodig.excel.core.database.ExcelEntity?> =
        excelQ.selectById(id).asFlow().mapToOneOrNull(ioDispatcher)

    fun findById(id: String): com.plasodig.excel.core.database.ExcelEntity? =
        excelQ.selectById(id).executeAsOneOrNull()

    fun findAll(): List<com.plasodig.excel.core.database.ExcelEntity> =
        excelQ.selectAll().executeAsList()

    fun updateImageUrl(id: String, imageUrl: String, updatedAt: Long) {
        excelQ.updateImageUrl(imageUrl = imageUrl, updatedAt = updatedAt, id = id)
    }

    fun updateTitleCategory(id: String, title: String, category: String, updatedAt: Long) {
        excelQ.updateTitleCategory(title = title, category = category, updatedAt = updatedAt, id = id)
    }

    fun search(query: String): Flow<List<com.plasodig.excel.core.database.ExcelEntity>> =
        excelQ.searchByTitle(query).asFlow().mapToList(ioDispatcher)

    fun relatedFunctionsOf(excelId: String): List<com.plasodig.excel.core.database.RelatedFunctionEntity> =
        ingQ.selectByExcel(excelId).executeAsList()

    fun stepsOf(excelId: String): List<com.plasodig.excel.core.database.TutorialStepEntity> =
        stepQ.selectByExcel(excelId).executeAsList()

    fun observeFavoriteIds(): Flow<List<String>> =
        favQ.selectAll().asFlow().mapToList(ioDispatcher)

    fun isFavorite(excelId: String): Boolean =
        favQ.isFavorite(excelId).executeAsOne()

    fun addFavorite(excelId: String, at: Long) {
        favQ.addFavorite(excelId, at)
    }

    fun removeFavorite(excelId: String) {
        favQ.removeFavorite(excelId)
    }

    fun upsertFull(
        id: String,
        title: String,
        description: String,
        imageUrl: String,
        category: String,
        difficulty: String,
        cookingTimeMinutes: Int,
        servings: Int,
        tags: String,
        updatedAtEpoch: Long,
        relatedFunctions: List<RelatedFunctionInput>,
        steps: List<StepInput>,
    ) {
        db.transaction {
            excelQ.upsert(
                id, title, description, imageUrl, category, difficulty,
                cookingTimeMinutes.toLong(), servings.toLong(), tags, updatedAtEpoch,
            )
            ingQ.deleteByExcel(id)
            relatedFunctions.forEach {
                ingQ.insertRelatedFunction(id, it.name, it.quantity, it.unit, it.group, it.orderIndex.toLong())
            }
            stepQ.deleteByExcel(id)
            steps.forEach {
                stepQ.insertStep(id, it.order.toLong(), it.instruction, it.durationMinutes?.toLong())
            }
        }
    }

    fun deleteAll() {
        db.transaction { excelQ.deleteAll() }
    }

    fun count(): Long = excelQ.countAll().executeAsOne()
}

data class RelatedFunctionInput(
    val name: String,
    val quantity: String,
    val unit: String,
    val group: String?,
    val orderIndex: Int,
)

data class StepInput(
    val order: Int,
    val instruction: String,
    val durationMinutes: Int?,
)
