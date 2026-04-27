package com.plasodig.excel.image

import android.content.Context
import coil3.PlatformContext
import okio.Path
import okio.Path.Companion.toOkioPath

internal actual fun imageCacheDir(context: PlatformContext): Path {
    val ctx = context as Context
    return ctx.cacheDir.resolve("excel_images").toOkioPath()
}
