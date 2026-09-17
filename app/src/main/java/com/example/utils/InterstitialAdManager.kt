package com.example.utils

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

object InterstitialAdManager {
    private const val TAG = "INTERSTITIAL_AD_MGR"

    // Production Interstitial Ad Unit ID for BrainQuizAI
    private const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-6636916633195809/4216184354"

    @Volatile
    private var isAdShowing = false

    @Volatile
    private var preloadedAd: InterstitialAd? = null

    @Volatile
    private var isPreloading = false

    /**
     * Preloads an Interstitial Ad in background so it is ready when the quiz completes.
     */
    fun preloadInterstitialAd(context: Context) {
        val appContext = context.applicationContext ?: context
        if (preloadedAd != null || isPreloading) return
        isPreloading = true

        val adRequest = AdRequest.Builder().build()
        Log.d(TAG, "Preloading Interstitial Ad...")

        InterstitialAd.load(
            appContext,
            INTERSTITIAL_AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    preloadedAd = interstitialAd
                    isPreloading = false
                    Log.d(TAG, "Interstitial Ad preloaded successfully and ready to display")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    preloadedAd = null
                    isPreloading = false
                    Log.w(TAG, "Interstitial Ad failed to preload: ${loadAdError.message} (code: ${loadAdError.code})")
                }
            }
        )
    }

    /**
     * Displays the Interstitial ad if available.
     * Always calls onAdDismissed() so the user flow continues without being blocked.
     */
    fun showInterstitialAd(
        activity: Activity,
        onAdDismissed: () -> Unit
    ) {
        if (isAdShowing) {
            Log.w(TAG, "Interstitial ad is already showing. Ignoring duplicate request.")
            onAdDismissed()
            return
        }

        val ad = preloadedAd
        if (ad == null) {
            Log.d(TAG, "No preloaded Interstitial Ad ready. Continuing flow directly.")
            // Try preloading next one for future runs
            preloadInterstitialAd(activity)
            onAdDismissed()
            return
        }

        isAdShowing = true
        // Consume the preloaded instance
        preloadedAd = null

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Interstitial ad dismissed by user")
                isAdShowing = false
                // Preload for the next quiz
                preloadInterstitialAd(activity)
                onAdDismissed()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.e(TAG, "Interstitial ad failed to show: ${adError.message} (code: ${adError.code})")
                isAdShowing = false
                preloadInterstitialAd(activity)
                onAdDismissed()
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Interstitial ad shown successfully")
            }
        }

        try {
            ad.show(activity)
        } catch (e: Exception) {
            Log.e(TAG, "Exception showing Interstitial Ad: ${e.message}", e)
            isAdShowing = false
            preloadInterstitialAd(activity)
            onAdDismissed()
        }
    }
}
