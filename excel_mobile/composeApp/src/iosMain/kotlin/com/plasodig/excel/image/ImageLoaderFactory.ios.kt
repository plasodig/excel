package com.plasodig.excel.image

import coil3.PlatformContext
import okio.Path
import okio.Path.Companion.toPath
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

internal actual fun imageCacheDir(context: PlatformContext): Path {
    val paths = NSSearchPathForDirectoriesInDomains(
        directory = NSCachesDirectory,
        domainMask = NSUserDomainMask,
        expandTilde = true,
    )
    val base = paths.firstOrNull() as? String ?: "/tmp"
    return "$base/excel_images".toPath()
}
