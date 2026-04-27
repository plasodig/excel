package com.plasodig.excel.core.ads

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * iOS stub — integrasi AdMob iOS butuh:
 *   1. Add CocoaPod `Google-Mobile-Ads-SDK` ke `iosApp/Podfile`.
 *   2. Update `iosApp/iosApp/Info.plist` dengan `GADApplicationIdentifier` = AdUnitIds.ADMOB_APP_ID_IOS.
 *   3. Di Swift bridge: initialize `GADMobileAds.sharedInstance().start()`.
 *   4. Expose UIView wrapper ke Kotlin via ObjC interop, lalu pakai UIKitView di Compose.
 *
 * Sementara return Box kosong supaya composable tree aman di iOS build.
 */
@Composable
actual fun AdSlot(
    placement: AdPlacement,
    modifier: Modifier,
) {
    Box(modifier = modifier)
}
