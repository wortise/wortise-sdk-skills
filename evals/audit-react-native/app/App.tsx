import React, { useRef, useState } from 'react';
import { Button, Text, View } from 'react-native';
import {
  RNWortiseAdSettings,
  RNWortiseInterstitial,
  RNWortiseRewarded,
  RNWortiseSdk,
  WortiseBanner,
} from '@wortise/react-native-sdk';

RNWortiseInterstitial.setAdUnitId('9b1e4d2c-3a5f-4e7b-8c6d-1f2a3b4c5d6e');

export default function App() {
  const [lives, setLives] = useState(3);
  const bannerRef = useRef<WortiseBanner>(null);

  RNWortiseSdk.initialize('5a1f3c2e-9d47-4b8a-a6e1-0c3d7f2b9e54');
  RNWortiseAdSettings.setTestEnabled(true);

  RNWortiseRewarded.addEventListener('onRewardedDismissed', () => {
    setLives((l) => l + 1);
  });

  const onGameOver = () => {
    RNWortiseInterstitial.loadAd();
    if (RNWortiseInterstitial.isAvailable) {
      RNWortiseInterstitial.showAd();
    }
  };

  const onExtraLife = () => {
    RNWortiseRewarded.setAdUnitId('test-rewarded');
    RNWortiseRewarded.loadAd();
    RNWortiseRewarded.showAd();
  };

  return (
    <View style={{ flex: 1 }}>
      <Text>Lives: {lives}</Text>
      <Button title="Game over" onPress={onGameOver} />
      <Button title="Extra life" onPress={onExtraLife} />
      <WortiseBanner
        ref={bannerRef}
        adUnitId="3f0c2a1e-7b4d-4c1a-9e2f-5d8b6a0c4e11"
        autoRefreshTime={60}
      />
    </View>
  );
}
