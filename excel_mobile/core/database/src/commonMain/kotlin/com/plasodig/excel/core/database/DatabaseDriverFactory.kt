package com.plasodig.excel.core.database

import app.cash.sqldelight.db.SqlDriver

expect class DatabaseDriverFactory {
    fun create(): SqlDriver
}

fun createDatabase(factory: DatabaseDriverFactory): ExcelDatabase =
    ExcelDatabase(factory.create())
