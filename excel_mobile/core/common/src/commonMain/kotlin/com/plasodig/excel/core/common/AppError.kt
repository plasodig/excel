package com.plasodig.excel.core.common

sealed interface AppError {
    val message: String

    data class Network(
        override val message: String,
        val statusCode: Int? = null,
    ) : AppError

    data class Database(override val message: String) : AppError

    data class NotFound(val id: String) : AppError {
        override val message: String get() = "not found: $id"
    }

    data class Validation(override val message: String) : AppError

    data class Unknown(
        override val message: String,
        val cause: Throwable? = null,
    ) : AppError

    /**
     * HTTP 429 — user (IP) sudah melewati batas rate limit untuk endpoint tertentu.
     * [retryAfterSeconds] dari header Retry-After kalau server kirim; default 3600.
     */
    data class RateLimited(
        val retryAfterSeconds: Int = 3600,
        override val message: String = "rate_limit_exceeded",
    ) : AppError
}
