import { Platform } from 'react-native';
import {
  RNWortiseAdSettings,
  RNWortiseConsentManager,
  RNWortiseSdk,
} from '@wortise/react-native-sdk';

const APP_KEY = Platform.select({
  android: '5a1f3c2e-9d47-4b8a-a6e1-0c3d7f2b9e54',
  default: 'e08b6c3d-71a4-4f29-9b5e-2d6f8a1c4e37',
});

export const AD_UNITS = Platform.select({
  android: {
    banner: '3f0c2a1e-7b4d-4c1a-9e2f-5d8b6a0c4e11',
    interstitial: '9b1e4d2c-3a5f-4e7b-8c6d-1f2a3b4c5d6e',
    rewarded: 'c47a8e15-2f9b-4d63-a1e0-5b7c3d9f2e86',
  },
  default: {
    banner: '81d4e6a2-5c3b-4f70-9e1d-a62b8c4f0d93',
    interstitial: '2e9f7b1c-4d6a-4853-b0c2-7f1e3a5d9b48',
    rewarded: 'f5c3a9d7-8e2b-41f6-a3d0-6b4e1c7f2a95',
  },
})!;

let markReady!: () => void;

export const adsReady = new Promise<void>(resolve => {
  markReady = resolve;
});

let started = false;

export async function startAds(): Promise<void> {
  if (started) {
    return adsReady;
  }
  started = true;

  RNWortiseAdSettings.setTestEnabled(__DEV__);

  await RNWortiseSdk.initialize(APP_KEY);
  await RNWortiseConsentManager.requestIfRequired();

  markReady();
}
