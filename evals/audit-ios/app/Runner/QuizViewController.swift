import UIKit
import WortiseSDK

class BannerLogger: NSObject, WABannerDelegate {
    func didLoad(bannerAd: WABannerAd) { print("banner loaded") }
}

class QuizViewController: UIViewController {

    @IBOutlet weak var bannerAd: WABannerAd!

    var interstitialAd: WAInterstitialAd!
    var rewardedAd: WARewardedAd!
    var hints = 0

    override func viewDidLoad() {
        super.viewDidLoad()

        bannerAd.adUnitId = "3f0c2a1e-7b4d-4c1a-9e2f-5d8b6a0c4e11"
        bannerAd.delegate = BannerLogger()
        bannerAd.loadAd()

        interstitialAd = WAInterstitialAd(adUnitId: "9b1e4d2c-3a5f-4e7b-8c6d-1f2a3b4c5d6e")
        interstitialAd.delegate = self
        interstitialAd.loadAd()

        rewardedAd = WARewardedAd(adUnitId: "test-rewarded")
        rewardedAd.delegate = self
        rewardedAd.loadAd()
    }

    @IBAction func quizFinished() {
        interstitialAd.showAd(from: self)
    }

    @IBAction func getHint() {
        rewardedAd.showAd(from: self)
    }
}

extension QuizViewController: WAInterstitialDelegate {
    func didFailToLoad(interstitialAd: WAInterstitialAd, error: WAAdError) {
        interstitialAd.loadAd()
    }
    func didDismiss(interstitialAd: WAInterstitialAd) {
        interstitialAd.loadAd()
    }
}

extension QuizViewController: WARewardedDelegate {
    func didDismiss(rewardedAd: WARewardedAd) {
        hints += 1
        rewardedAd.loadAd()
    }
}
