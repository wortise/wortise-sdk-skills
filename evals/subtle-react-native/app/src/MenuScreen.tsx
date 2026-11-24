import React from 'react';
import { Button, View } from 'react-native';

// Opens GameScreen; players go back to the menu after every run
export function MenuScreen({ navigation }: { navigation: any }) {
  return (
    <View style={{ flex: 1, justifyContent: 'center' }}>
      <Button title="Play" onPress={() => navigation.navigate('Game')} />
    </View>
  );
}
