package com.example.wordtiles

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.wortise.ads.AdError
import com.wortise.ads.AdSize
import com.wortise.ads.WortiseSdk
import com.wortise.ads.appopen.AppOpenAd
import com.wortise.ads.banner.BannerAd
import com.wortise.ads.consent.ConsentManager
import com.wortise.ads.interstitial.InterstitialAd
import com.wortise.ads.rewarded.RewardedAd
import com.wortise.ads.rewarded.models.Reward

class MainActivity : ComponentActivity() {

    private var adsReady by mutableStateOf(false)

    private var appOpenAd: AppOpenAd? = null

    private var coins by mutableIntStateOf(0)

    private val handler = Handler(Looper.getMainLooper())

    private var interstitialAd: InterstitialAd? = null

    private var rewardedAd: RewardedAd? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Column(Modifier.fillMaxSize()) {
                Text("Coins: $coins")
                Button(onClick = ::onLevelFinished) { Text("Next level") }
                Button(onClick = ::onGetCoins) { Text("Watch an ad for 30 coins") }
                if (adsReady) {
                    Banner()
                }
            }
        }

        WortiseSdk.wait {
            ConsentManager.requestIfRequired(this) {
                if (!isFinishing && !isDestroyed) {
                    loadAds()
                }
            }
        }
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        appOpenAd?.destroy()
        interstitialAd?.destroy()
        rewardedAd?.destroy()
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        interstitialAd?.loadAd()
    }

    override fun onStart() {
        super.onStart()
        appOpenAd?.tryToShowAd(this)
    }

    private fun loadAds() {
        interstitialAd = InterstitialAd(this, AdUnits.INTERSTITIAL).also {
            it.listener = interstitialListener
            it.loadAd()
        }
        rewardedAd = RewardedAd(this, AdUnits.REWARDED).also {
            it.listener = rewardedListener
            it.loadAd()
        }
        appOpenAd = AppOpenAd(this, AdUnits.APP_OPEN).also {
            it.autoReload = true
            it.loadAd()
        }
        adsReady = true
    }

    private fun onGetCoins() {
        val ad = rewardedAd
        if (ad?.isAvailable == true) {
            ad.showAd(this)
        }
    }

    private fun onLevelFinished() {
        val ad = interstitialAd
        if (ad?.isAvailable == true) {
            ad.showAd(this)
        } else {
            startNextLevel()
        }
    }

    private fun startNextLevel() {
        // Game logic
    }

    @Composable
    private fun Banner() {
        AndroidView(
            factory = { context ->
                BannerAd(context).apply {
                    adSize   = AdSize.HEIGHT_50
                    adUnitId = AdUnits.BANNER
                }
            },
            update = { banner -> banner.loadAd() },
            modifier = Modifier.fillMaxWidth()
        )
    }

    private val interstitialListener = object : InterstitialAd.Listener {

        override fun onInterstitialDismissed(ad: InterstitialAd) {
            ad.loadAd()
            startNextLevel()
        }

        override fun onInterstitialFailedToLoad(ad: InterstitialAd, error: AdError) {
            val delay = maxOf(ad.cooldownRemainingMs, 30_000L)
            handler.postDelayed({ ad.loadAd() }, delay)
        }

        override fun onInterstitialFailedToShow(ad: InterstitialAd, error: AdError) {
            ad.loadAd()
            startNextLevel()
        }
    }

    private val rewardedListener = object : RewardedAd.Listener {

        override fun onRewardedCompleted(ad: RewardedAd, reward: Reward) {
            coins += 30
        }

        override fun onRewardedDismissed(ad: RewardedAd) {
            ad.loadAd()
        }

        override fun onRewardedFailedToLoad(ad: RewardedAd, error: AdError) {
            val delay = maxOf(ad.cooldownRemainingMs, 30_000L)
            handler.postDelayed({ ad.loadAd() }, delay)
        }
    }
}
