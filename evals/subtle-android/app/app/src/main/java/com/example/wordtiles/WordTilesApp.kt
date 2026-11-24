package com.example.wordtiles

import android.app.Application
import com.wortise.ads.AdSettings
import com.wortise.ads.WortiseSdk
import com.wortise.ads.appopen.AppOpenManager

class WordTilesApp : Application() {

    private val appOpenManager by lazy {
        AppOpenManager.getInstance(this, AdUnits.APP_OPEN)
    }

    override fun onCreate() {
        super.onCreate()

        AdSettings.testEnabled = BuildConfig.DEBUG

        WortiseSdk.initialize(this, "5a1f3c2e-9d47-4b8a-a6e1-0c3d7f2b9e54")

        appOpenManager.loadAd()
    }
}
