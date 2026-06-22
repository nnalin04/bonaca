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
      // Wait for AuthProvider to finish restoring (or failing to restore) a stored session
      // before deciding where to go — routing to /login first would flash the login screen
      // even for an already-authenticated user.
      if (isLoading) return;
      if (!isAuthenticated) {
        router.replace('/(auth)/login');
      } else if (!profileCompleted) {
        // A session can be restored (valid tokens) without onboarding ever finishing, e.g. the
        // app was killed between OTP verify and Complete Profile — land back there, not Home,
        // which would otherwise fail every member fetch with a raw "Complete your profile first".
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
