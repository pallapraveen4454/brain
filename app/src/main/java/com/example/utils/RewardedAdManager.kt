package com.example.utils

import android.app.Activity
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

    private fun ensureMobileAdsInitialized(activity: Activity) {
        if (!isMobileAdsInitialized) {
            synchronized(this) {
                if (!isMobileAdsInitialized) {
                    try {
                        MobileAds.initialize(activity.applicationContext) {}
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
     * Loads and shows a Rewarded Ad.
     * The [onRewardEarned] callback is ONLY invoked if the user successfully watches the ad and earns the reward.
     * If the user cancels, skips, or if the ad fails to load/show, [onError] is called and no reward is granted.
     */
    fun showRewardedAd(
        activity: Activity,
        onRewardEarned: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (isAdActive) {
            Log.d(TAG, "Ad request ignored: An ad flow is already active.")
            return
        }

        isAdActive = true
        ensureMobileAdsInitialized(activity)

        val adRequest = AdRequest.Builder().build()

        Log.d(TAG, "Loading Rewarded Ad with Unit ID: $TEST_REWARDED_AD_UNIT_ID")

        RewardedAd.load(
            activity,
            TEST_REWARDED_AD_UNIT_ID,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(rewardedAd: RewardedAd) {
                    Log.d(TAG, "Rewarded Ad loaded successfully. Presenting to user...")

                    rewardedAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            Log.d(TAG, "Rewarded Ad dismissed by user")
                            isAdActive = false
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            Log.e(TAG, "Rewarded Ad failed to show: ${adError.message} (code ${adError.code})")
                            isAdActive = false
                            onError("Hint unavailable right now. Please try again.")
                        }

                        override fun onAdShowedFullScreenContent() {
                            Log.d(TAG, "Rewarded Ad is now showing full screen")
                        }
                    }

                    rewardedAd.show(activity) { rewardItem ->
                        Log.d(TAG, "User completed Rewarded Ad! Reward earned: amount=${rewardItem.amount}, type=${rewardItem.type}")
                        onRewardEarned()
                    }
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.e(TAG, "Rewarded Ad failed to load: ${loadAdError.message} (code ${loadAdError.code})")
                    isAdActive = false
                    onError("Hint unavailable right now. Please try again.")
                }
            }
        )
    }
}
