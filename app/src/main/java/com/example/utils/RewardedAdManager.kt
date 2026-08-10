package com.example.utils

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

object RewardedAdManager {
    private const val TAG = "REWARDED_AD_MANAGER"
    
    // Official Google Mobile Ads Test Rewarded Ad Unit ID
    private const val TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

    @Volatile
    private var isAdActive = false

    @Volatile
    private var isMobileAdsInitialized = false

    @Volatile
    private var preloadedAd: RewardedAd? = null

    @Volatile
    private var isPreloading = false

    fun ensureMobileAdsInitialized(context: Context) {
        if (!isMobileAdsInitialized) {
            synchronized(this) {
                if (!isMobileAdsInitialized) {
                    try {
                        MobileAds.initialize(context.applicationContext) {}
                        isMobileAdsInitialized = true
                        Log.d(TAG, "MobileAds initialized successfully")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to initialize MobileAds: ${e.message}", e)
                    }
                }
            }
        }
    }

    /**
     * Preloads a Rewarded Ad in the background so it can be presented immediately when requested.
     */
    fun preloadRewardedAd(context: Context) {
        ensureMobileAdsInitialized(context)
        if (preloadedAd != null || isPreloading) return
        isPreloading = true

        val adRequest = AdRequest.Builder().build()
        Log.d(TAG, "Preloading Rewarded Ad...")

        RewardedAd.load(
            context,
            TEST_REWARDED_AD_UNIT_ID,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(rewardedAd: RewardedAd) {
                    preloadedAd = rewardedAd
                    isPreloading = false
                    Log.d(TAG, "Rewarded Ad preloaded successfully")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    preloadedAd = null
                    isPreloading = false
                    Log.w(TAG, "Failed to preload Rewarded Ad: ${loadAdError.message}")
                }
            }
        )
    }

    /**
     * Loads and shows a Rewarded Ad.
     * The [onRewardEarned] callback is ONLY invoked if the user successfully watches the ad and earns the reward.
     * If the user closes the ad before reward, [onAdClosedWithoutReward] is invoked.
     * If the ad fails to load or show, [onError] is called.
     */
    fun showRewardedAd(
        activity: Activity,
        onRewardEarned: () -> Unit,
        onAdClosedWithoutReward: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (isAdActive) {
            Log.d(TAG, "Ad request ignored: An ad flow is already active.")
            return
        }

        isAdActive = true
        ensureMobileAdsInitialized(activity)

        var rewardEarned = false

        fun presentAd(rewardedAd: RewardedAd) {
            rewardedAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Rewarded Ad dismissed. rewardEarned=$rewardEarned")
                    isAdActive = false
                    // Preload the next ad in background
                    preloadRewardedAd(activity)
                    // Resume background music if enabled
                    BackgroundMusicPlayer.resume(activity)

                    if (!rewardEarned) {
                        onAdClosedWithoutReward()
                    }
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.e(TAG, "Rewarded Ad failed to show: ${adError.message} (code ${adError.code})")
                    isAdActive = false
                    preloadedAd = null
                    preloadRewardedAd(activity)
                    BackgroundMusicPlayer.resume(activity)
                    onError("Hint unavailable right now. Please try again.")
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "Rewarded Ad is now showing full screen")
                    BackgroundMusicPlayer.pause()
                }
            }

            rewardedAd.show(activity) { rewardItem ->
                Log.d(TAG, "User completed Rewarded Ad! Reward earned: amount=${rewardItem.amount}, type=${rewardItem.type}")
                rewardEarned = true
                onRewardEarned()
            }
        }

        val existingAd = preloadedAd
        if (existingAd != null) {
            preloadedAd = null
            Log.d(TAG, "Using preloaded Rewarded Ad")
            presentAd(existingAd)
        } else {
            val adRequest = AdRequest.Builder().build()
            Log.d(TAG, "No preloaded ad available. Loading Rewarded Ad directly...")

            RewardedAd.load(
                activity,
                TEST_REWARDED_AD_UNIT_ID,
                adRequest,
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(rewardedAd: RewardedAd) {
                        Log.d(TAG, "Rewarded Ad loaded directly. Presenting to user...")
                        presentAd(rewardedAd)
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        Log.e(TAG, "Rewarded Ad failed to load: ${loadAdError.message} (code ${loadAdError.code})")
                        isAdActive = false
                        preloadRewardedAd(activity)
                        onError("Hint unavailable right now. Please try again.")
                    }
                }
            )
        }
    }
}
