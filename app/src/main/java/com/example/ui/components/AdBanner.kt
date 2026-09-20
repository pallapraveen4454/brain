package com.example.ui.components

import android.content.Context
import android.util.Log
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkCardSurface
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.TextMuted
import com.example.utils.RewardedAdManager
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Standard AdMob Banner Component for BrainQuizAI.
 * Dynamically displays the banner once loaded, reserving zero space
 * when loading, failed, or when no ad is available.
 */
@Composable
fun AdMobBanner(
    modifier: Modifier = Modifier,
    adUnitId: String = "ca-app-pub-6636916633195809/8442776346" // Production AdMob Banner Ad Unit ID
) {
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current
    val coroutineScope = rememberCoroutineScope()
    var isAdLoaded by remember { mutableStateOf(false) }

    if (isPreview) {
        return
    }

    val retryJobRef = remember { mutableStateOf<Job?>(null) }

    val adView = remember(adUnitId) {
        RewardedAdManager.ensureMobileAdsInitialized(context)
        val maxRetries = 3
        val retryDelaysMs = longArrayOf(10_000L, 30_000L, 60_000L)
        var retryCount = 0

        AdView(context).apply {
            setAdSize(AdSize.BANNER)
            setAdUnitId(adUnitId)
            adListener = object : AdListener() {
                override fun onAdLoaded() {
                    isAdLoaded = true
                    retryJobRef.value?.cancel()
                    retryJobRef.value = null
                    Log.d(
                        "AdMobBanner",
                        "Banner ad loaded successfully. ResponseInfo: ${responseInfo?.toString() ?: "None"}"
                    )
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(
                        "AdMobBanner",
                        "Banner failed to load: code=${error.code}, domain=${error.domain}, message=${error.message}, responseInfo=${error.responseInfo?.toString() ?: "None"}"
                    )
                    if (isAdLoaded) {
                        return
                    }
                    if (retryCount < maxRetries) {
                        val delayMs = retryDelaysMs.getOrElse(retryCount) { 60_000L }
                        val nextAttempt = retryCount + 1
                        retryCount = nextAttempt
                        Log.d(
                            "AdMobBanner",
                            "Scheduling banner retry #$nextAttempt in ${delayMs / 1000}s (due to code ${error.code}: ${error.message})"
                        )
                        retryJobRef.value?.cancel()
                        retryJobRef.value = coroutineScope.launch {
                            delay(delayMs)
                            if (!isAdLoaded) {
                                Log.d(
                                    "AdMobBanner",
                                    "Executing banner retry #$nextAttempt for unitId: $adUnitId"
                                )
                                try {
                                    this@apply.loadAd(AdRequest.Builder().build())
                                } catch (e: Exception) {
                                    Log.e("AdMobBanner", "Error executing banner retry #$nextAttempt", e)
                                }
                            }
                        }
                    } else {
                        Log.w("AdMobBanner", "Maximum retry limit ($maxRetries) reached for banner ad. Stopping retries.")
                    }
                }
            }
            try {
                Log.d("AdMobBanner", "Banner ad load started for unitId: $adUnitId")
                loadAd(AdRequest.Builder().build())
            } catch (e: Exception) {
                Log.e("AdMobBanner", "Error loading banner ad", e)
            }
        }
    }

    DisposableEffect(adView) {
        onDispose {
            retryJobRef.value?.cancel()
            retryJobRef.value = null
            try {
                (adView.parent as? ViewGroup)?.removeView(adView)
                adView.destroy()
            } catch (e: Exception) {
                Log.w("AdMobBanner", "Error destroying adView", e)
            }
        }
    }

    if (isAdLoaded) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .testTag("admob_banner_container"),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                modifier = Modifier.wrapContentSize(),
                factory = {
                    (adView.parent as? ViewGroup)?.removeView(adView)
                    adView
                }
            )
        }
    }
}

@Composable
fun AdBannerPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(DarkCardSurface.copy(alpha = 0.5f))
            .border(1.dp, DarkCardBorder, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "AdMob Banner Ad Area",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp
            ),
            color = TextMuted
        )
    }
}
