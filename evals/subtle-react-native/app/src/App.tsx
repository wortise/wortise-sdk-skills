import React, { useEffect } from 'react';
import { NavigationContainer } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { startAds } from './ads';
import { GameScreen } from './GameScreen';
import { MenuScreen } from './MenuScreen';

const Stack = createNativeStackNavigator();

export default function App() {
  useEffect(() => {
    startAds();
  }, []);

  return (
    <NavigationContainer>
      <Stack.Navigator>
        <Stack.Screen name="Menu" component={MenuScreen} />
        <Stack.Screen name="Game" component={GameScreen} />
      </Stack.Navigator>
    </NavigationContainer>
  );
}
