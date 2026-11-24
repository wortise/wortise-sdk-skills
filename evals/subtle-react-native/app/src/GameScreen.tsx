import React, { useEffect, useRef, useState } from 'react';
import { Button, Dimensions, Text, View } from 'react-native';
import {
  RNWortiseAdSize,
  RNWortiseInterstitial,
  RNWortiseRewarded,
  WortiseBanner,
} from '@wortise/react-native-sdk';
import { AD_UNITS, adsReady } from './ads';

export function GameScreen({ navigation }: { navigation: any }) {
  const [gems, setGems] = useState(0);
  const bannerRef = useRef<WortiseBanner>(null);
  const [adSize] = useState(() =>
    RNWortiseAdSize.getAnchoredAdaptiveBannerAdSize(Dimensions.get('window').width),
  );

  useEffect(() => {
    adsReady.then(() => {
      bannerRef.current?.loadAd();

      RNWortiseInterstitial.setAdUnitId(AD_UNITS.interstitial);
      RNWortiseInterstitial.loadAd();

      RNWortiseRewarded.setAdUnitId(AD_UNITS.rewarded);
      RNWortiseRewarded.loadAd();
    });

    const retryLater = async (ad: typeof RNWortiseInterstitial | typeof RNWortiseRewarded) => {
      const remaining: number = await ad.cooldownRemainingMs;
      setTimeout(() => ad.loadAd(), Math.max(remaining, 30000));
    };

    RNWortiseInterstitial.addEventListener('onInterstitialDismissed', () => {
      RNWortiseInterstitial.loadAd();
      navigation.goBack();
    });
    RNWortiseInterstitial.addEventListener('onInterstitialFailedToLoad', () =>
      retryLater(RNWortiseInterstitial),
    );
    RNWortiseRewarded.addEventListener('onRewardedCompleted', ({ amount }) => {
      setGems(g => g + 5);
    });
    RNWortiseRewarded.addEventListener('onRewardedDismissed', () => RNWortiseRewarded.loadAd());
    RNWortiseRewarded.addEventListener('onRewardedFailedToLoad', () =>
      retryLater(RNWortiseRewarded),
    );
  }, []);

  const onRunOver = async () => {
    if (RNWortiseInterstitial.isAvailable) {
      RNWortiseInterstitial.showAd();
    } else {
      navigation.goBack();
    }
  };

  const onFreeGems = async () => {
    if (await RNWortiseRewarded.isAvailable) {
      RNWortiseRewarded.showAd();
    }
  };

  return (
    <View style={{ flex: 1 }}>
      <Text>Gems: {gems}</Text>
      <Button title="Run over" onPress={onRunOver} />
      <Button title="Watch an ad for 5 gems" onPress={onFreeGems} />
      <WortiseBanner
        ref={bannerRef}
        adSize={adSize}
        adUnitId={AD_UNITS.banner}
        autoRefreshTime={60000}
      />
    </View>
  );
}
