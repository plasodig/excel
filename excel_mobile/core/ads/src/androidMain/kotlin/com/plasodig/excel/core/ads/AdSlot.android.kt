package com.plasodig.excel.core.ads

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import co.touchlab.kermit.Logger
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView

private val log = Logger.withTag("AdSlot")

@Composable
actual fun AdSlot(
    placement: AdPlacement,
    modifier: Modifier,
) {
    when (placement) {
        AdPlacement.DetailFooterBanner -> BannerAdSlot(modifier = modifier)
        AdPlacement.ListInFeedNative -> NativeAdSlot(modifier = modifier)
    }
}

// ===== Banner =============================================================

@Composable
private fun BannerAdSlot(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val config: Configuration = LocalConfiguration.current
    val adWidth = remember(config.screenWidthDp) { config.screenWidthDp }
    val adId = remember { PlatformAdIds.banner }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { ctx ->
                AdView(ctx).apply {
                    adUnitId = adId
                    setAdSize(
                        AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(ctx, adWidth),
                    )
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    )
                    runCatching { loadAd(AdRequest.Builder().build()) }
                        .onFailure { log.w(it) { "AdView load exception" } }
                }
            },
        )
    }
}

// ===== Native Advanced ====================================================

/**
 * Native Advanced ad — layout mirip ExcelCard (media + judul + body + CTA + label "Sponsored").
 * Kita simpan reference ke child views via `View.tag` (NativeAdRefs) supaya saat bind
 * di `update` callback tidak perlu traverse hierarchy.
 */
@Composable
private fun NativeAdSlot(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val adId = remember { PlatformAdIds.native }
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }

    DisposableEffect(adId) {
        val loader = AdLoader.Builder(context, adId)
            .forNativeAd { loaded ->
                nativeAd?.destroy()
                nativeAd = loaded
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(err: LoadAdError) {
                    log.w { "Native ad load fail: ${err.code} ${err.message}" }
                }
            })
            .withNativeAdOptions(
                NativeAdOptions.Builder()
                    .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
                    .build(),
            )
            .build()
        loader.loadAd(AdRequest.Builder().build())
        onDispose {
            nativeAd?.destroy()
            nativeAd = null
        }
    }

    nativeAd?.let { ad ->
        AndroidView(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            factory = { ctx ->
                val (view, refs) = buildNativeAdView(ctx)
                view.tag = refs
                bindNativeAd(view, refs, ad)
                view
            },
            update = { view ->
                val refs = view.tag as? NativeAdRefs ?: return@AndroidView
                bindNativeAd(view as NativeAdView, refs, ad)
            },
        )
    }
}

private data class NativeAdRefs(
    val headline: TextView,
    val body: TextView,
    val cta: Button,
    val media: MediaView,
)

private fun buildNativeAdView(context: Context): Pair<NativeAdView, NativeAdRefs> {
    val density = context.resources.displayMetrics.density
    fun dp(v: Int) = (v * density).toInt()

    val nativeView = NativeAdView(context)

    val root = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )
        background = GradientDrawable().apply {
            cornerRadius = dp(16).toFloat()
            setColor(resolveColorAttr(context, android.R.attr.colorBackground))
        }
    }

    // Media frame (image/video + "Sponsored" label overlay).
    val mediaFrame = FrameLayout(context).apply {
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(160),
        )
    }
    val media = MediaView(context).apply {
        layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
    }
    val sponsoredLabel = TextView(context).apply {
        text = "Sponsored"
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
        setTextColor(Color.WHITE)
        setPadding(dp(6), dp(2), dp(6), dp(2))
        background = GradientDrawable().apply {
            cornerRadius = dp(6).toFloat()
            setColor(Color.argb(160, 0, 0, 0))
        }
        layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            setMargins(dp(8), dp(8), 0, 0)
        }
    }
    mediaFrame.addView(media)
    mediaFrame.addView(sponsoredLabel)

    val headline = TextView(context).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        setTypeface(typeface, Typeface.BOLD)
        setTextColor(resolveColorAttr(context, android.R.attr.textColorPrimary))
        setPadding(dp(12), dp(12), dp(12), dp(4))
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
    }

    val body = TextView(context).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        setTextColor(resolveColorAttr(context, android.R.attr.textColorSecondary))
        setPadding(dp(12), 0, dp(12), dp(10))
        maxLines = 2
        ellipsize = TextUtils.TruncateAt.END
    }

    val cta = Button(context).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        isAllCaps = false
        minimumHeight = dp(36)
        setPadding(dp(16), 0, dp(16), 0)
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        ).apply {
            setMargins(dp(12), 0, dp(12), dp(12))
        }
    }

    root.addView(mediaFrame)
    root.addView(headline)
    root.addView(body)
    root.addView(cta)
    nativeView.addView(root)

    nativeView.mediaView = media
    nativeView.headlineView = headline
    nativeView.bodyView = body
    nativeView.callToActionView = cta

    return nativeView to NativeAdRefs(headline, body, cta, media)
}

private fun bindNativeAd(view: NativeAdView, refs: NativeAdRefs, ad: NativeAd) {
    refs.headline.text = ad.headline.orEmpty()
    refs.body.text = ad.body.orEmpty()
    refs.cta.text = ad.callToAction?.ifBlank { "Lihat" } ?: "Lihat"
    view.setNativeAd(ad)
}

private fun resolveColorAttr(context: Context, attr: Int): Int {
    val typedValue = TypedValue()
    context.theme.resolveAttribute(attr, typedValue, true)
    return typedValue.data
}

/**
 * Placeholder ringan (opsional) untuk meng-indikasi space iklan saat loading.
 */
@Composable
fun AdLoadingPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth().height(50.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Iklan",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
