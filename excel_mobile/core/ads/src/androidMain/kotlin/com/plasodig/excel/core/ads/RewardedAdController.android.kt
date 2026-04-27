package com.plasodig.excel.core.ads

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import co.touchlab.kermit.Logger
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

private val log = Logger.withTag("RewardedAd")

/**
 * Wrap `RewardedAd` SDK jadi suspend API. Singleton via Koin binding (Application context).
 *
 * Karena Koin bind dengan `androidContext()` = Application, `show()` butuh Activity yang
 * harus di-bind manual dari MainActivity.onCreate via [bindActivity]. Sama pattern seperti
 * ConsentManager.
 */
actual class RewardedAdController(private val context: Context) {

    private var boundActivity: Activity? = null

    /** Dipanggil dari MainActivity.onCreate sebelum setContent. */
    fun bindActivity(activity: Activity) {
        boundActivity = activity
    }

    /** Dipanggil dari MainActivity.onDestroy untuk cegah memory leak. */
    fun unbindActivity() {
        boundActivity = null
    }

    actual suspend fun loadAndShow(): RewardedOutcome {
        val activity = boundActivity ?: context.findActivity()
        if (activity == null) {
            log.w { "No Activity — bindActivity() belum dipanggil dari MainActivity?" }
            return RewardedOutcome.Failed("Activity tidak tersedia")
        }
        val ad = loadRewarded(activity) ?: return RewardedOutcome.Failed("Gagal memuat iklan")
        return showAndAwaitOutcome(activity, ad)
    }

    private suspend fun loadRewarded(activity: Activity): RewardedAd? =
        suspendCancellableCoroutine { cont ->
            val request = AdRequest.Builder().build()
            RewardedAd.load(
                activity,
                PlatformAdIds.rewarded,
                request,
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) {
                        if (cont.isActive) cont.resume(ad)
                    }

                    override fun onAdFailedToLoad(err: LoadAdError) {
                        log.w { "Rewarded load fail: ${err.code} ${err.message}" }
                        if (cont.isActive) cont.resume(null)
                    }
                },
            )
        }

    private suspend fun showAndAwaitOutcome(
        activity: Activity,
        ad: RewardedAd,
    ): RewardedOutcome = suspendCancellableCoroutine { cont ->
        var earned = false

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                if (cont.isActive) {
                    cont.resume(
                        if (earned) RewardedOutcome.Granted
                        else RewardedOutcome.Dismissed,
                    )
                }
            }

            override fun onAdFailedToShowFullScreenContent(err: AdError) {
                log.w { "Rewarded show fail: ${err.code} ${err.message}" }
                if (cont.isActive) {
                    cont.resume(RewardedOutcome.Failed(err.message))
                }
            }
        }

        ad.show(activity) { _ ->
            earned = true
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
