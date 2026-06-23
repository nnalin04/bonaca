import { Image } from 'expo-image';
import { LinearGradient } from 'expo-linear-gradient';
import { useRouter } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import { useEffect } from 'react';
import { StyleSheet, View } from 'react-native';

import { useAuth } from '@/features/auth/AuthContext';
import { Colors } from '@/theme/tokens';

const AUTO_ADVANCE_DELAY_MS = 1800;

export function SplashScreen() {
  const router = useRouter();
  const { isLoading, isAuthenticated, profileCompleted } = useAuth();

  useEffect(() => {
    const timer = setTimeout(() => {
      if (isLoading) return;
      if (!isAuthenticated) {
        router.replace('/(auth)/login');
      } else if (!profileCompleted) {
        router.replace('/(auth)/complete-profile');
      } else {
        router.replace('/(tabs)/home');
      }
    }, AUTO_ADVANCE_DELAY_MS);

    return () => clearTimeout(timer);
  }, [router, isLoading, isAuthenticated, profileCompleted]);

  return (
    <LinearGradient
      colors={[Colors.headerGradientStart, Colors.headerGradientEnd]}
      locations={[0, 0.9504]}
      start={{ x: 0.9705, y: -0.432 }}
      end={{ x: 0.2064, y: 1.2136 }}
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
