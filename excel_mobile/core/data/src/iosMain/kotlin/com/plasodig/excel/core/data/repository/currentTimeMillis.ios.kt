package com.plasodig.excel.core.data.repository

import kotlinx.datetime.Clock

internal actual fun currentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()
