import UIKit
import WortiseSDK

@main
class AppDelegate: UIResponder, UIApplicationDelegate {

    func application(_ application: UIApplication,
                     didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        WAAdSettings.testEnabled = true

        DispatchQueue.global(qos: .background).async {
            WortiseAds.shared.initialize(assetKey: "5a1f3c2e-9d47-4b8a-a6e1-0c3d7f2b9e54")
        }
        return true
    }
}
