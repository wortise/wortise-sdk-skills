import 'package:flutter/material.dart';
import 'package:wortise/wortise.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  WortiseSdk.initialize('5a1f3c2e-9d47-4b8a-a6e1-0c3d7f2b9e54');
  AdSettings.setTestEnabled(true);
  runApp(const WordRushApp());
}

class WordRushApp extends StatelessWidget {
  const WordRushApp({super.key});

  @override
  Widget build(BuildContext context) => const MaterialApp(home: GameScreen());
}

class GameScreen extends StatefulWidget {
  const GameScreen({super.key});

  @override
  State<GameScreen> createState() => _GameScreenState();
}

class _GameScreenState extends State<GameScreen> {
  int coins = 0;
  bool waitingForAd = false;

  @override
  Widget build(BuildContext context) {
    final interstitial = InterstitialAd('9b1e4d2c-3a5f-4e7b-8c6d-1f2a3b4c5d6e', listener: (event, args) {
      if (event == InterstitialAdEvent.FAILED_TO_LOAD) {
        InterstitialAd('9b1e4d2c-3a5f-4e7b-8c6d-1f2a3b4c5d6e').loadAd();
      }
      if (event == InterstitialAdEvent.DISMISSED) {
        setState(() => waitingForAd = false);
      }
    })..loadAd();

    final rewarded = RewardedAd('test-rewarded', listener: (event, args) {
      if (event == RewardedAdEvent.DISMISSED) {
        setState(() => coins += 20);
      }
    })..loadAd();

    return Scaffold(
      body: ListView(
        children: [
          Text('Coins: $coins'),
          ElevatedButton(
            onPressed: waitingForAd ? null : () {
              setState(() => waitingForAd = true);
              interstitial.showAd();
            },
            child: const Text('Next round'),
          ),
          ElevatedButton(
            onPressed: () => rewarded.showAd(),
            child: const Text('Free coins'),
          ),
          const BannerAd(
            adUnitId: '3f0c2a1e-7b4d-4c1a-9e2f-5d8b6a0c4e11',
            adSize: AdSize.MATCH_VIEW,
            autoRefreshTime: 60,
          ),
        ],
      ),
    );
  }
}
