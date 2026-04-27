package com.plasodig.excel.core.ads

import android.app.Activity
import android.content.Context
import co.touchlab.kermit.Logger
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

private val log = Logger.withTag("ConsentManager")

/**
 * Android implementation UMP. Constructor param `debugBypass = true` (via BuildConfig.DEBUG)
 * langsung init MobileAds TANPA UMP — supaya test ads work saat dev tanpa perlu consent form
 * yang kadang stuck di emulator / non-EU region.
 *
 * Critical fix: `requestConsentInfoUpdate()` WAJIB dipanggil dengan Activity, bukan Application
 * context. MainActivity.onCreate → `consentManager.bindActivity(this)` supaya Activity tersedia
 * sebelum `ensureConsent()` dipanggil dari App.kt.
 */
actual class ConsentManager(
    private val context: Context,
    private val debugBypass: Boolean = false,
) {

    private val consentInfo: ConsentInformation =
        UserMessagingPlatform.getConsentInformation(context)

    private var boundActivity: Activity? = null
    private var adsSdkInitialized = false

    /** Dipanggil dari MainActivity.onCreate sebelum setContent. */
    fun bindActivity(activity: Activity) {
        boundActivity = activity
    }

    /** Dipanggil dari MainActivity.onDestroy untuk cegah memory leak. */
    fun unbindActivity() {
        boundActivity = null
    }

    actual suspend fun ensureConsent() {
        // Debug bypass — langsung init MobileAds, skip UMP flow entirely.
        if (debugBypass) {
            log.i { "Debug bypass ON — skip UMP, init MobileAds." }
            initAds()
            return
        }

        val activity = boundActivity
        if (activity == null) {
            log.w { "No activity bound — skip UMP. Call bindActivity() dari MainActivity.onCreate." }
            return
        }

        log.i { "UMP flow: status sebelum request = ${consentInfo.consentStatus}, canRequestAds=${consentInfo.canRequestAds()}" }

        requestInfoAndShowForm(activity)

        log.i { "UMP flow: status setelah request = ${consentInfo.consentStatus}, canRequestAds=${consentInfo.canRequestAds()}" }

        if (consentInfo.canRequestAds()) {
            initAds()
        } else {
            log.w { "Consent belum OK — ads tidak di-init. Status: ${consentInfo.consentStatus}" }
        }
    }

    actual fun canServePersonalizedAds(): Boolean = consentInfo.canRequestAds()

    private suspend fun requestInfoAndShowForm(activity: Activity) = suspendCancellableCoroutine<Unit> { cont ->
        val params = ConsentRequestParameters.Builder().build()
        consentInfo.requestConsentInfoUpdate(
            activity,
            params,
            {
                log.i { "requestConsentInfoUpdate OK. Status: ${consentInfo.consentStatus}" }
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    if (formError != null) {
                        log.w { "Consent form error ${formError.errorCode}: ${formError.message}" }
                    } else {
                        log.i { "Consent form completed (or not required). Status: ${consentInfo.consentStatus}" }
                    }
                    if (cont.isActive) cont.resume(Unit)
                }
            },
            { err ->
                log.w { "requestConsentInfoUpdate FAIL ${err.errorCode}: ${err.message}" }
                if (cont.isActive) cont.resume(Unit)
            },
        )
    }

    private fun initAds() {
        if (adsSdkInitialized) return
        MobileAds.initialize(context) { status ->
            log.i { "MobileAds initialized. Adapter status: ${status.adapterStatusMap}" }
        }
        adsSdkInitialized = true
    }
}
