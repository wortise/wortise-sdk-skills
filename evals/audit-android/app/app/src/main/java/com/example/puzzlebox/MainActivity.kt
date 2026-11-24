package com.example.puzzlebox

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.wortise.ads.AdError
import com.wortise.ads.AdSettings
import com.wortise.ads.WortiseSdk
import com.wortise.ads.banner.BannerAd
import com.wortise.ads.interstitial.InterstitialAd
import com.wortise.ads.rewarded.RewardedAd

class MainActivity : AppCompatActivity() {

    private var coins = 0

    private lateinit var bannerAd: BannerAd

    private lateinit var coinsView: TextView

    private var rewardedAd: RewardedAd? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        AdSettings.testEnabled = true
        WortiseSdk.initialize(this, "5a1f3c2e-9d47-4b8a-a6e1-0c3d7f2b9e54")

        coinsView = findViewById(R.id.coins)
        updateCoins()

        bannerAd = findViewById(R.id.banner)
        bannerAd.listener = object : BannerAd.Listener {
            override fun onBannerFailedToLoad(ad: BannerAd, error: AdError) {
                ad.loadAd()
            }
        }

        if (interstitialAd == null) {
            interstitialAd = InterstitialAd(this, "9b1e4d2c-3a5f-4e7b-8c6d-1f2a3b4c5d6e").also {
                it.listener = interstitialListener
                it.loadAd()
            }
        }

        rewardedAd = RewardedAd(this, "test-rewarded").also {
            it.listener = rewardedListener
            it.loadAd()
        }

        findViewById<Button>(R.id.finish).setOnClickListener {
            interstitialAd?.showAd(this)
        }

        findViewById<Button>(R.id.reward).setOnClickListener {
            rewardedAd?.showAd(this)
        }
    }

    override fun onDestroy() {
        bannerAd.destroy()
        super.onDestroy()
    }

    private fun updateCoins() {
        coinsView.text = getString(R.string.coins_label, coins)
    }

    private val interstitialListener = object : InterstitialAd.Listener {

        override fun onInterstitialLoaded(ad: InterstitialAd) {
            ad.showAd(this@MainActivity)
        }

        override fun onInterstitialDismissed(ad: InterstitialAd) {
            ad.loadAd()
        }

        override fun onInterstitialFailedToLoad(ad: InterstitialAd, error: AdError) {
            ad.loadAd()
        }
    }

    private val rewardedListener = object : RewardedAd.Listener {

        override fun onRewardedDismissed(ad: RewardedAd) {
            coins += 50
            updateCoins()
            ad.loadAd()
        }
    }

    companion object {
        private var interstitialAd: InterstitialAd? = null
    }
}
