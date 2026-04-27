package com.plasodig.excel.core.common

import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity

object AppLogger {
    fun d(tag: String, msg: String) = Logger.d(tag) { msg }
    fun i(tag: String, msg: String) = Logger.i(tag) { msg }
    fun w(tag: String, msg: String, t: Throwable? = null) = Logger.w(tag, t) { msg }
    fun e(tag: String, msg: String, t: Throwable? = null) = Logger.e(tag, t) { msg }

    fun setMinSeverity(severity: Severity) {
        Logger.setMinSeverity(severity)
    }
}
