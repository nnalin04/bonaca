import { Image } from 'expo-image';
import { LinearGradient } from 'expo-linear-gradient';
import { useRouter } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import { useEffect } from 'react';
import { StyleSheet, View } from 'react-native';

import { Colors } from '@/theme/tokens';

const AUTO_ADVANCE_DELAY_MS = 1800;

export function SplashScreen() {
  const router = useRouter();

  useEffect(() => {
    const timer = setTimeout(() => {
      router.replace('/(auth)/login');
    }, AUTO_ADVANCE_DELAY_MS);

    return () => clearTimeout(timer);
  }, [router]);

  return (
    <LinearGradient
      colors={[Colors.headerGradientStart, Colors.headerGradientEnd]}
      locations={[0, 0.95]}
      start={{ x: 0.97, y: -0.43 }}
      end={{ x: 0.21, y: 1.21 }}
      style={styles.screen}>
      <StatusBar style="light" />
      <View style={styles.content}>
        <Image
          source={require('../../../assets/images/brand/bonaca-mark.png')}
          style={styles.mark}
          contentFit="contain"
        />
      </View>
    </LinearGradient>
  );
}

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  content: {
    alignItems: 'center',
    justifyContent: 'center',
  },
  mark: {
    width: 80,
    height: 80,
  },
});
