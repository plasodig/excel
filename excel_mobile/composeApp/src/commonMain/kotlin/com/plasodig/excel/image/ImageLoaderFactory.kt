package com.plasodig.excel.image

import androidx.compose.runtime.Composable
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.compose.setSingletonImageLoaderFactory
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.CachePolicy
import coil3.request.crossfade
import okio.Path

private const val MEMORY_CACHE_BYTES: Long = 32L * 1024 * 1024
private const val DISK_CACHE_BYTES: Long = 50L * 1024 * 1024

internal expect fun imageCacheDir(context: PlatformContext): Path

@Composable
fun SetupExcelImageLoader() {
    setSingletonImageLoaderFactory { ctx -> buildExcelImageLoader(ctx) }
}

private fun buildExcelImageLoader(context: PlatformContext): ImageLoader =
    ImageLoader.Builder(context)
        .components { add(KtorNetworkFetcherFactory()) }
        .memoryCache {
            MemoryCache.Builder()
                .maxSizeBytes(MEMORY_CACHE_BYTES)
                .build()
        }
        .diskCache {
            DiskCache.Builder()
                .directory(imageCacheDir(context))
                .maxSizeBytes(DISK_CACHE_BYTES)
                .build()
        }
        .memoryCachePolicy(CachePolicy.ENABLED)
        .diskCachePolicy(CachePolicy.ENABLED)
        .networkCachePolicy(CachePolicy.ENABLED)
        .crossfade(true)
        .build()
