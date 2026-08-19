package com.example.ui.components

import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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

/**
 * Standard AdMob Banner Component for BrainQuizAI.
 * Formatted with safe insets, glassmorphism border, and fallback layout.
 */
@Composable
fun AdMobBanner(
    modifier: Modifier = Modifier,
    adUnitId: String = "ca-app-pub-3940256099942544/6300978111" // Official Google Test Banner Ad Unit ID
) {
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("admob_banner_container"),
        contentAlignment = Alignment.Center
    ) {
        if (isPreview) {
            AdBannerPlaceholder()
        } else {
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkCardSurface.copy(alpha = 0.6f))
                    .border(0.8.dp, GlassBorder.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                factory = { ctx ->
                    RewardedAdManager.ensureMobileAdsInitialized(ctx)
                    AdView(ctx).apply {
                        setAdSize(AdSize.BANNER)
                        setAdUnitId(adUnitId)
                        adListener = object : AdListener() {
                            override fun onAdLoaded() {
                                Log.d("AdMobBanner", "Banner ad loaded successfully")
                            }

                            override fun onAdFailedToLoad(error: LoadAdError) {
                                Log.w("AdMobBanner", "Banner failed to load: ${error.message}")
                            }
                        }
                        try {
                            loadAd(AdRequest.Builder().build())
                        } catch (e: Exception) {
                            Log.e("AdMobBanner", "Error loading banner ad", e)
                        }
                    }
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
